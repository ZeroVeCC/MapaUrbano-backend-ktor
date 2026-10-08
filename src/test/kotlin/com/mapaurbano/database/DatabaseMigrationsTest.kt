package com.mapaurbano.database

import com.mapaurbano.infrastructure.database.repositories.ReportRepositoryImpl
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportStatus
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("database")
@Testcontainers
class DatabaseMigrationsTest {
    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:16-3.5").asCompatibleSubstituteFor("postgres"),
        )

        private fun flyway(url: String = postgres.jdbcUrl): Flyway =
            DatabaseMigrations.configure(url, postgres.username, postgres.password)

        @BeforeAll
        @JvmStatic
        fun migrateEmptyDatabase() {
            // The PostGIS image preinstalls extensions. An extension-only schema must be accepted.
            assertEquals(3, flyway().migrate().migrationsExecuted)
            flyway().validate()
        }
    }

    private lateinit var connection: Connection

    @BeforeEach
    fun connect() {
        connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        connection.autoCommit = false
    }

    @AfterEach
    fun rollback() {
        connection.rollback()
        connection.close()
    }

    @Test
    fun `DNI permits registration without email and enforces canonical uniqueness`() {
        val id = UUID.randomUUID()
        execute("INSERT INTO users (id, dni, display_name, password_hash) VALUES (?, '30123456', 'Vecino', 'test-only-hash')", id)
        assertEquals("30123456", scalar("SELECT dni FROM users WHERE id = ?", id))
        assertEquals(null, scalar("SELECT email FROM users WHERE id = ?", id))
        rejects("23505") { execute("INSERT INTO users (dni, display_name, password_hash) VALUES ('30123456', 'Otro', 'hash')") }
        for (invalid in listOf("", "123456", "123456789", "30.123.456", "abcdefgh", "00000000")) {
            rejects("23514") { execute("UPDATE users SET dni = ? WHERE id = ?", invalid, id) }
        }
        rejects("23514") { execute("UPDATE users SET dni = NULL WHERE id = ?", id) }
    }

    @Test
    fun `upgrade from V2 preserves legacy users and supports verified DNI assignment`() {
        val url = newDatabase("dni_upgrade_test")
        val old = Flyway.configure().configuration(flyway(url).configuration).target("2").load()
        assertEquals(2, old.migrate().migrationsExecuted)
        val id = UUID.randomUUID()
        DriverManager.getConnection(url, postgres.username, postgres.password).use { db ->
            db.prepareStatement("INSERT INTO users (id, email, display_name, password_hash) VALUES (?, 'legacy@example.com', 'Legacy', 'preserved-hash')").use {
                it.setObject(1, id)
                it.executeUpdate()
            }
        }
        assertEquals(1, flyway(url).migrate().migrationsExecuted)
        DriverManager.getConnection(url, postgres.username, postgres.password).use { db ->
            db.createStatement().use { statement ->
                statement.executeQuery("SELECT email, dni, password_hash FROM users").use {
                    assertTrue(it.next())
                    assertEquals("legacy@example.com", it.getString("email"))
                    assertEquals(null, it.getString("dni"))
                    assertEquals("preserved-hash", it.getString("password_hash"))
                }
                assertEquals(1, statement.executeUpdate("UPDATE users SET dni = '30123456' WHERE email = 'legacy@example.com'"))
            }
        }
        flyway(url).validate()
        assertEquals(0, flyway(url).migrate().migrationsExecuted)
    }

    @Test
    fun `schema contains all required tables and spatial index`() {
        val tables = setOf(
            "categories", "admin_users", "users", "user_sessions", "teams", "team_members",
            "reports", "report_images", "report_status_history", "report_priority_history",
            "report_assignments", "audit_events",
        )
        for (table in tables) assertEquals(table, scalar("SELECT to_regclass(?)::text", table))
        assertTrue(scalar("SELECT postgis_version()")!!.startsWith("3.5"))
        assertTrue(scalar("SELECT indexdef FROM pg_indexes WHERE indexname = 'reports_location_gist_idx'")!!.contains("USING gist (location)"))
        assertTrue(scalar("SELECT indexdef FROM pg_indexes WHERE indexname = 'report_assignments_target_idx'")!!.contains("(team_id, responsible_admin_user_id)"))
    }

    @Test
    fun `repeat migrate preserves existing data and checksums`() {
        val id = report()
        connection.commit()
        try {
            assertEquals(0, flyway().migrate().migrationsExecuted)
            flyway().validate()
            assertEquals(id.toString(), scalar("SELECT id::text FROM reports WHERE id = ?", id))
            assertEquals("3", scalar("SELECT count(*) FROM flyway_schema_history WHERE success"))
            assertFailsWith<FlywayException> { flyway().clean() }
        } finally {
            execute("DELETE FROM reports WHERE id = ?", id)
            connection.commit()
        }
    }

    @Test
    fun `category seed is idempotent and preserves municipal edits`() {
        assertEquals("bache,basura,inundacion,luminaria,otro,vandalismo", scalar("SELECT string_agg(slug, ',' ORDER BY slug) FROM categories"))
        val id = scalar("SELECT id::text FROM categories WHERE slug = 'bache'")
        execute("UPDATE categories SET name = 'Baches municipales', is_active = false WHERE slug = 'bache'")
        val seed = javaClass.getResource("/db/migration/V2__seed_initial_categories.sql")!!.readText()
        execute(seed)
        assertEquals("6", scalar("SELECT count(*) FROM categories"))
        assertEquals(id, scalar("SELECT id::text FROM categories WHERE slug = 'bache'"))
        assertEquals("Baches municipales", scalar("SELECT name FROM categories WHERE slug = 'bache' AND NOT is_active"))
    }

    @Test
    fun `registered and anonymous reports enforce exclusive authorship`() {
        val user = user()
        val anonymous = report()
        val registered = report(user)
        assertEquals("pending:medium:0", scalar("SELECT status::text || ':' || priority::text || ':' || version FROM reports WHERE id = ?", anonymous))
        rejects("23514") { execute("UPDATE reports SET user_id = ? WHERE id = ?", user, anonymous) }
        rejects("23514") { execute("UPDATE reports SET user_id = NULL WHERE id = ?", registered) }
        rejects("23514") { execute("UPDATE reports SET tracking_code_hint = NULL WHERE id = ?", anonymous) }
        rejects("23505") { execute("UPDATE reports SET tracking_code_hash = (SELECT tracking_code_hash FROM reports WHERE id = ?) WHERE id = ?", anonymous, report()) }
        rejects("23514") { execute("UPDATE reports SET version = -1 WHERE id = ?", anonymous) }
        rejects("22P02") { execute("UPDATE reports SET status = 'unknown' WHERE id = ?", anonymous) }
        rejects("22P02") { execute("UPDATE reports SET priority = 'unknown' WHERE id = ?", anonymous) }
        rejects("23514") { execute("UPDATE reports SET title = '   ' WHERE id = ?", anonymous) }
        rejects("23514") { execute("UPDATE reports SET description = '' WHERE id = ?", anonymous) }
        rejects("23503") { execute("UPDATE reports SET category_id = ? WHERE id = ?", UUID.randomUUID(), anonymous) }
    }

    @Test
    fun `repository persists status priority and location for anonymous report`(): Unit = runBlocking {
        val report = reportDomain(userId = null)

        verifyRepositoryPersistence(report)
    }

    @Test
    fun `repository persists status priority and location for account report`(): Unit = runBlocking {
        val userId = user()
        connection.commit()
        val report = reportDomain(userId = userId.toString())

        try {
            verifyRepositoryPersistence(report)
        } finally {
            execute("DELETE FROM users WHERE id = ?", userId)
            connection.commit()
        }
    }

    @Test
    fun `only one image per report with valid metadata and bounded size`() {
        val id = report()
        image(id)
        rejects("23505") { image(id) }
        rejects("23514") { execute("UPDATE report_images SET size_bytes = 2 WHERE report_id = ?", id) }
        rejects("23514") { execute("UPDATE report_images SET content_type = 'text/html' WHERE report_id = ?", id) }
        rejects("23514") { execute("UPDATE report_images SET width_px = 0 WHERE report_id = ?", id) }
        rejects("23514") { execute("UPDATE report_images SET height_px = 12001 WHERE report_id = ?", id) }
        rejects("23514") { execute("UPDATE report_images SET checksum_sha256 = 'invalid' WHERE report_id = ?", id) }
        rejects("23514") { execute("UPDATE report_images SET data = ''::bytea, size_bytes = 0 WHERE report_id = ?", id) }
        execute("UPDATE report_images SET data = decode(repeat('00', 5242880), 'hex'), size_bytes = 5242880 WHERE report_id = ?", id)
        rejects("23514") { execute("UPDATE report_images SET data = decode(repeat('00', 5242881), 'hex'), size_bytes = 5242881 WHERE report_id = ?", id) }
        assertEquals("5242880", scalar("SELECT octet_length(data) FROM report_images WHERE report_id = ?", id))
    }

    @Test
    fun `user and admin identifiers are unique regardless of case and sessions have valid dates`() {
        val user = user("neighbor@example.com")
        rejects("23505") { user("NEIGHBOR@example.com") }
        rejects("23514") { user("  ") }
        // Normalization and the complete email policy belong to the backend contract.
        user("o'connor@example.com")
        val admin = admin("Municipio")
        rejects("23505") { admin("MUNICIPIO") }
        assertNotNull(admin)
        val session = UUID.randomUUID()
        execute("INSERT INTO user_sessions (id, user_id, token_hash, expires_at) VALUES (?, ?, decode('aabb', 'hex'), now() + interval '1 day')", session, user)
        rejects("23505") { execute("INSERT INTO user_sessions (user_id, token_hash, expires_at) VALUES (?, decode('aabb', 'hex'), now() + interval '1 day')", user) }
        rejects("23514") { execute("UPDATE user_sessions SET expires_at = created_at WHERE id = ?", session) }
        rejects("23514") { execute("UPDATE user_sessions SET revoked_at = created_at - interval '1 second' WHERE id = ?", session) }
        rejects("23514") { execute("UPDATE users SET deleted_at = now() WHERE id = ?", user) }
    }

    @Test
    fun `reassignment preserves history and permits only one active assignment`() {
        val id = report()
        val admin = admin()
        val team = UUID.randomUUID()
        execute("INSERT INTO teams (id, name) VALUES (?, ?)", team, team.toString())
        val assignment = UUID.randomUUID()
        execute("INSERT INTO report_assignments (id, report_id, team_id, assigned_by) VALUES (?, ?, ?, ?)", assignment, id, team, admin)
        rejects("23505") { execute("INSERT INTO report_assignments (report_id, responsible_admin_user_id, assigned_by) VALUES (?, ?, ?)", id, admin, admin) }
        rejects("23514") { execute("UPDATE report_assignments SET team_id = NULL WHERE id = ?", assignment) }
        rejects("23514") { execute("UPDATE report_assignments SET unassigned_at = assigned_at - interval '1 second' WHERE id = ?", assignment) }
        execute("UPDATE report_assignments SET unassigned_at = now() WHERE id = ?", assignment)
        execute("INSERT INTO report_assignments (report_id, responsible_admin_user_id, assigned_by) VALUES (?, ?, ?)", id, admin, admin)
        assertEquals("2", scalar("SELECT count(*) FROM report_assignments WHERE report_id = ?", id))
        assertEquals("1", scalar("SELECT count(*) FROM report_assignments WHERE report_id = ? AND unassigned_at IS NULL", id))
    }

    @Test
    fun `soft deletion preserves reports images and audit history`() {
        val user = user()
        val admin = admin()
        val id = report(user)
        image(id)
        execute("INSERT INTO report_status_history (report_id, to_status) VALUES (?, 'pending')", id)
        execute("INSERT INTO report_priority_history (report_id, changed_by, to_priority) VALUES (?, ?, 'medium')", id, admin)
        execute("INSERT INTO audit_events (actor_user_id, action, entity_type, entity_id) VALUES (?, 'report.created', 'report', ?)", user, id)
        rejects("23514") { execute("UPDATE audit_events SET actor_admin_user_id = ? WHERE entity_id = ?", admin, id) }
        execute("UPDATE users SET is_active = false, deleted_at = now() WHERE id = ?", user)
        execute("UPDATE reports SET deleted_at = now() WHERE id = ?", id)
        for (table in listOf("report_images", "report_status_history", "report_priority_history")) {
            assertEquals("1", scalar("SELECT count(*) FROM $table WHERE report_id = ?", id))
        }
        assertEquals("1", scalar("SELECT count(*) FROM reports WHERE id = ? AND user_id = ?", id, user))
        rejects("23503") { execute("DELETE FROM reports WHERE id = ?", id) }
    }

    @Test
    fun `timestamps are refreshed and geography supports meter queries`() {
        val id = report()
        // Commit creates a new transaction timestamp for the subsequent update.
        connection.commit()
        try {
            val before = scalar("SELECT updated_at::text FROM reports WHERE id = ?", id)!!
            execute("UPDATE reports SET title = 'Updated' WHERE id = ?", id)
            assertEquals("yes", scalar("SELECT CASE WHEN updated_at > ?::timestamptz THEN 'yes' ELSE 'no' END FROM reports WHERE id = ?", before, id))
            assertEquals("0", scalar("SELECT version FROM reports WHERE id = ?", id))
            assertEquals("4326", scalar("SELECT ST_SRID(location::geometry) FROM reports WHERE id = ?", id))
            assertEquals("yes", scalar("SELECT CASE WHEN ST_DWithin(location, ST_SetSRID(ST_MakePoint(-68.8458, -32.8895),4326)::geography, 10) THEN 'yes' ELSE 'no' END FROM reports WHERE id = ?", id))
        } finally {
            execute("DELETE FROM reports WHERE id = ?", id)
            connection.commit()
        }
    }

    @Test
    fun `failed migration rolls back without leaving partial tables`() {
        val url = newDatabase("rollback_test")
        assertEquals(3, flyway(url).migrate().migrationsExecuted)
        val failing = Flyway.configure().configuration(flyway(url).configuration)
            .locations("classpath:db/migration", "classpath:db/failing").load()
        assertFailsWith<FlywayException> { failing.migrate() }
        DriverManager.getConnection(url, postgres.username, postgres.password).use { db ->
            db.createStatement().use { statement ->
                statement.executeQuery("SELECT to_regclass('partial_migration'), (SELECT count(*) FROM categories)").use {
                    assertTrue(it.next())
                    assertEquals(null, it.getString(1))
                    assertEquals(6, it.getInt(2))
                }
            }
        }
        flyway(url).validate()
        assertEquals(0, flyway(url).migrate().migrationsExecuted)
    }

    @Test
    fun `backup restores schema binary evidence and migration history`() {
        val id = report()
        image(id)
        connection.commit()
        try {
            val url = newDatabase("restore_test")
            val dump = postgres.execInContainer("pg_dump", "-U", postgres.username, "-d", postgres.databaseName, "-Fc", "-f", "/tmp/migration-test.dump")
            assertEquals(0, dump.exitCode, dump.stderr)
            val restore = postgres.execInContainer("pg_restore", "-U", postgres.username, "-d", "restore_test", "--exit-on-error", "/tmp/migration-test.dump")
            assertEquals(0, restore.exitCode, restore.stderr)
            DriverManager.getConnection(url, postgres.username, postgres.password).use { db ->
                db.prepareStatement("SELECT encode(data, 'hex'), size_bytes FROM report_images WHERE report_id = ?").use { statement ->
                    statement.setObject(1, id)
                    statement.executeQuery().use {
                        assertTrue(it.next())
                        assertEquals("00", it.getString(1))
                        assertEquals(1, it.getInt(2))
                    }
                }
            }
            flyway(url).validate()
            assertEquals(0, flyway(url).migrate().migrationsExecuted)
        } finally {
            execute("DELETE FROM reports WHERE id = ?", id)
            connection.commit()
        }
    }

    private fun newDatabase(name: String): String {
        require(name.matches(Regex("[a-z_]+")))
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use {
            it.createStatement().use { statement -> statement.execute("CREATE DATABASE $name TEMPLATE template0") }
        }
        return postgres.jdbcUrl.replace("/${postgres.databaseName}", "/$name")
    }

    private fun user(email: String = "${UUID.randomUUID()}@example.com"): UUID {
        val id = UUID.randomUUID()
        execute("INSERT INTO users (id, email, display_name, password_hash) VALUES (?, ?, 'Test', 'test-only-hash')", id, email)
        return id
    }

    private fun admin(username: String = UUID.randomUUID().toString()): UUID {
        val id = UUID.randomUUID()
        execute("INSERT INTO admin_users (id, username, password_hash) VALUES (?, ?, 'test-only-hash')", id, username)
        return id
    }

    private fun report(user: UUID? = null): UUID {
        val id = UUID.randomUUID()
        execute(
            """INSERT INTO reports (id, category_id, user_id, title, description, location, tracking_code_hash, tracking_code_hint)
               VALUES (?, (SELECT id FROM categories WHERE slug = 'bache'), ?, 'Bache', 'Descripción de prueba',
               ST_SetSRID(ST_MakePoint(-68.8458, -32.8895),4326)::geography, ?, ?)""",
            id, user, if (user == null) id.toString().toByteArray() else null, if (user == null) "test" else null,
        )
        return id
    }

    private fun reportDomain(userId: String?): Report {
        val id = UUID.randomUUID()
        val now = Instant.now()
        return Report(
            id = id.toString(),
            categoryId = scalar("SELECT id::text FROM categories WHERE slug = 'bache'")!!,
            userId = userId,
            status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM,
            title = "Bache desde Android",
            description = "Prueba de persistencia con Exposed",
            latitude = -32.8895,
            longitude = -68.8458,
            trackingCodeHash = if (userId == null) id.toString().toByteArray() else null,
            trackingCodeHint = if (userId == null) "test****" else null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun verifyRepositoryPersistence(report: Report) {
        Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password,
        )

        try {
            val repository = ReportRepositoryImpl()
            repository.create(report)
            assertEquals("pending", scalar("SELECT status::text FROM reports WHERE id = ?", UUID.fromString(report.id)))
            assertEquals("medium", scalar("SELECT priority::text FROM reports WHERE id = ?", UUID.fromString(report.id)))
            assertEquals(report.latitude, scalar("SELECT ST_Y(location::geometry) FROM reports WHERE id = ?", UUID.fromString(report.id))!!.toDouble(), 0.000001)
            assertEquals(report.longitude, scalar("SELECT ST_X(location::geometry) FROM reports WHERE id = ?", UUID.fromString(report.id))!!.toDouble(), 0.000001)

            // Reconstruct the repository to exercise the same read path used after a backend restart.
            val restartedRepository = ReportRepositoryImpl()
            val byId = assertNotNull(restartedRepository.findById(report.id))
            assertEquals(report.latitude, byId.latitude, 0.000001)
            assertEquals(report.longitude, byId.longitude, 0.000001)

            val worldList = restartedRepository.findByBbox(
                minLat = -90.0,
                minLng = -180.0,
                maxLat = 90.0,
                maxLng = 180.0,
                status = null,
                categoryId = null,
                limit = 50,
            )
            assertTrue(worldList.any { it.id == report.id && it.latitude == report.latitude && it.longitude == report.longitude })

            if (report.userId != null) {
                val byUser = restartedRepository.findByUserId(report.userId, cursor = null, limit = 20)
                assertTrue(byUser.any { it.id == report.id && it.latitude == report.latitude && it.longitude == report.longitude })
            } else {
                val byTrackingCode = assertNotNull(restartedRepository.findByTrackingCodeHash(report.trackingCodeHash!!))
                assertEquals(report.latitude, byTrackingCode.latitude, 0.000001)
                assertEquals(report.longitude, byTrackingCode.longitude, 0.000001)
            }
        } finally {
            execute("DELETE FROM reports WHERE id = ?", UUID.fromString(report.id))
            connection.commit()
        }
    }

    private fun image(report: UUID) {
        // Tests DB constraints only; image decoding belongs to the media use case.
        execute("""INSERT INTO report_images (report_id, data, content_type, size_bytes, width_px, height_px, checksum_sha256)
            VALUES (?, decode('00', 'hex'), 'image/png', 1, 1, 1, encode(digest(decode('00', 'hex'), 'sha256'), 'hex'))""", report)
    }

    private fun execute(sql: String, vararg values: Any?) {
        connection.prepareStatement(sql).use { statement ->
            values.forEachIndexed { i, value -> statement.setObject(i + 1, value) }
            statement.execute()
        }
    }

    private fun scalar(sql: String, vararg values: Any?): String? =
        connection.prepareStatement(sql).use { statement ->
            values.forEachIndexed { i, value -> statement.setObject(i + 1, value) }
            statement.executeQuery().use { result ->
                assertTrue(result.next())
                result.getString(1)
            }
        }

    private fun rejects(sqlState: String, action: () -> Unit) {
        val savepoint = connection.setSavepoint()
        try {
            assertEquals(sqlState, assertFailsWith<SQLException> { action() }.sqlState)
        } finally {
            connection.rollback(savepoint)
            connection.releaseSavepoint(savepoint)
        }
    }
}
