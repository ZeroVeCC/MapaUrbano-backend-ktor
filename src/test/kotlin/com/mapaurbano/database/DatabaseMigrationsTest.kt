package com.mapaurbano.database

import com.mapaurbano.infrastructure.database.repositories.ReportRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AdminReportQueryRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.CategoryRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ImageRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.StatisticsRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ReportSubmissionRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AuditRepositoryImpl
import com.mapaurbano.audit.application.ListAuditEventsUseCase
import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.reports.domain.AdminReportFilter
import com.mapaurbano.users.application.GetOwnReportUseCase
import com.mapaurbano.users.application.ListUserReportsUseCase
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.shared.domain.PersistenceException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.application.applicationModule
import com.mapaurbano.application.configurePlugins
import com.mapaurbano.application.configureRateLimiting
import com.mapaurbano.application.configureRouting
import com.mapaurbano.application.configureSecurity
import com.mapaurbano.application.repositoryModule
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.ktor.plugin.Koin

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
            assertEquals(4, flyway().migrate().migrationsExecuted)
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
        for (invalid in listOf("", "123456", "abcdefgh", "00000000")) {
            rejects("23514") { execute("UPDATE users SET dni = ? WHERE id = ?", invalid, id) }
        }
        for (tooLong in listOf("123456789", "30.123.456")) {
            rejects("22001") { execute("UPDATE users SET dni = ? WHERE id = ?", tooLong, id) }
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
        assertEquals(2, flyway(url).migrate().migrationsExecuted)
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
            "categories", "admin_users", "admin_sessions", "users", "user_sessions", "teams", "team_members",
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
            assertEquals("4", scalar("SELECT count(*) FROM flyway_schema_history WHERE success"))
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
    fun `report and optional image are persisted atomically`(): Unit = runBlocking {
        val userId = user()
        connection.commit()
        val report = reportDomain(userId.toString())
        val invalidImage = ReportImage(
            id = UUID.randomUUID().toString(),
            reportId = report.id,
            data = byteArrayOf(0),
            contentType = "image/png",
            sizeBytes = 1,
            widthPx = 1,
            heightPx = 1,
            originalFilename = "evidencia.png",
            checksumSha256 = "invalid",
            createdAt = report.createdAt,
        )
        Database.connect(postgres.jdbcUrl, driver = "org.postgresql.Driver", user = postgres.username, password = postgres.password)

        assertFailsWith<PersistenceException> {
            ReportSubmissionRepositoryImpl().create(report, invalidImage)
        }
        assertEquals("0", scalar("SELECT count(*) FROM reports WHERE id = ?", UUID.fromString(report.id)))

        execute("DELETE FROM users WHERE id = ?", userId)
        connection.commit()
    }

    @Test
    fun `stable report and audit cursors do not skip rows with tied timestamps`(): Unit = runBlocking {
        val userId = user()
        val reportIds = List(3) { report(userId) }
        val auditIds = List(3) { UUID.randomUUID() }
        val tiedAt = Instant.parse("2100-01-01T00:00:00Z")
        val tiedTimestamp = java.sql.Timestamp.from(tiedAt)
        reportIds.forEach { execute("UPDATE reports SET created_at = ?, updated_at = ? WHERE id = ?", tiedTimestamp, tiedTimestamp, it) }
        auditIds.forEach {
            execute(
                "INSERT INTO audit_events (id, actor_user_id, action, entity_type, entity_id, occurred_at) VALUES (?, ?, 'cursor.test', 'report', ?, ?)",
                it, userId, reportIds.first(), tiedTimestamp,
            )
        }
        connection.commit()
        Database.connect(postgres.jdbcUrl, driver = "org.postgresql.Driver", user = postgres.username, password = postgres.password)

        try {
            val userReports = ListUserReportsUseCase(ReportRepositoryImpl(), CategoryRepositoryImpl(), ImageRepositoryImpl())
            val firstUserPage = userReports.execute(userId.toString(), cursor = null, limit = 2)
            val secondUserPage = userReports.execute(userId.toString(), cursor = assertNotNull(firstUserPage.nextCursor), limit = 2)
            assertEquals(reportIds.map(UUID::toString).toSet(), (firstUserPage.items + secondUserPage.items).map { it.id }.toSet())

            val adminReports = AdminReportQueryRepositoryImpl()
            val firstAdminPage = adminReports.list(AdminReportFilter(limit = 2))
            val secondAdminPage = adminReports.list(AdminReportFilter(cursor = assertNotNull(firstAdminPage.nextCursor), limit = 2))
            assertTrue(reportIds.map(UUID::toString).toSet().all { id ->
                (firstAdminPage.items + secondAdminPage.items).any { it.id == id }
            })

            val audit = ListAuditEventsUseCase(AuditRepositoryImpl())
            val firstAuditPage = audit.execute(cursor = null, limit = 2)
            val secondAuditPage = audit.execute(cursor = assertNotNull(firstAuditPage.nextCursor), limit = 2)
            assertTrue(auditIds.map(UUID::toString).toSet().all { id ->
                (firstAuditPage.items + secondAuditPage.items).any { it.id == id }
            })
        } finally {
            execute("DELETE FROM audit_events WHERE id = ANY (?)", connection.createArrayOf("uuid", auditIds.toTypedArray()))
            execute("DELETE FROM reports WHERE id = ANY (?)", connection.createArrayOf("uuid", reportIds.toTypedArray()))
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
    fun `admin sessions persist CSRF expiration and revocation constraints`() {
        val admin = admin()
        val session = UUID.randomUUID()
        execute(
            "INSERT INTO admin_sessions (id, admin_user_id, csrf_token, expires_at) VALUES (?, ?, ?, now() + interval '8 hours')",
            session, admin, UUID.randomUUID().toString().replace("-", ""),
        )
        assertEquals(admin.toString(), scalar("SELECT admin_user_id::text FROM admin_sessions WHERE id = ?", session))
        rejects("23505") {
            execute(
                "INSERT INTO admin_sessions (id, admin_user_id, csrf_token, expires_at) SELECT ?, ?, csrf_token, now() + interval '8 hours' FROM admin_sessions WHERE id = ?",
                UUID.randomUUID(), admin, session,
            )
        }
        rejects("23514") { execute("UPDATE admin_sessions SET expires_at = created_at WHERE id = ?", session) }
        rejects("23514") { execute("UPDATE admin_sessions SET revoked_at = created_at - interval '1 second' WHERE id = ?", session) }
    }

    @Test
    fun `admin login cookie CSRF report query and logout work end to end`() = testApplication {
        val adminId = UUID.randomUUID()
        val reportId = report()
        val teamId = UUID.randomUUID()
        val password = "admin-password-2026"
        execute(
            "INSERT INTO admin_users (id, username, password_hash) VALUES (?, 'integration-admin', ?)",
            adminId, BCrypt.withDefaults().hashToString(12, password.toCharArray()),
        )
        execute("INSERT INTO teams (id, name) VALUES (?, 'Integración vial')", teamId)
        connection.commit()

        application {
            install(Koin) { modules(repositoryModule, applicationModule) }
            configurePlugins()
            configureRateLimiting()
            configureSecurity()
            configureRouting()
        }
        val browser = createClient {
            install(HttpCookies)
            install(ClientWebSockets)
        }
        try {
            val login = browser.post("/api/v1/admin/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"integration-admin","password":"$password"}""")
            }
            assertEquals(HttpStatusCode.OK, login.status)
            assertTrue(
                login.headers.getAll(HttpHeaders.SetCookie).orEmpty()
                    .any { it.contains("Path=/api/v1", ignoreCase = true) },
                "La cookie administrativa debe alcanzar /api/v1/ws/admin",
            )
            val csrf = Json.parseToJsonElement(login.bodyAsText()).jsonObject.getValue("csrfToken").jsonPrimitive.content
            assertTrue(csrf.isNotBlank())
            assertEquals(HttpStatusCode.OK, browser.get("/api/v1/admin/auth/me").status)
            browser.webSocket("/api/v1/ws/admin") {
                val connected = withTimeout(2_000) { incoming.receive() as Frame.Text }
                val connectedType = Json.parseToJsonElement(connected.readText())
                    .jsonObject.getValue("type").jsonPrimitive.content
                assertEquals("connected", connectedType)
            }

            val reports = browser.get("/api/v1/admin/reports")
            assertEquals(HttpStatusCode.OK, reports.status)
            assertTrue(reports.bodyAsText().contains(reportId.toString()))
            assertEquals(HttpStatusCode.OK, browser.get("/api/v1/admin/reports/$reportId").status)

            val mutationBody = """{"status":"IN_PROGRESS","version":0,"note":"Integración"}"""
            assertEquals(HttpStatusCode.Forbidden, browser.patch("/api/v1/admin/reports/$reportId/status") {
                contentType(ContentType.Application.Json)
                setBody(mutationBody)
            }.status)
            val changed = browser.patch("/api/v1/admin/reports/$reportId/status") {
                contentType(ContentType.Application.Json)
                header("X-CSRF-Token", csrf)
                setBody(mutationBody)
            }
            assertEquals(HttpStatusCode.OK, changed.status)
            assertTrue(changed.bodyAsText().contains("IN_PROGRESS"))

            val assignmentBody = """{"teamId":"$teamId","version":1}"""
            assertEquals(HttpStatusCode.NoContent, browser.put("/api/v1/admin/reports/$reportId/assignment") {
                contentType(ContentType.Application.Json)
                header("X-CSRF-Token", csrf)
                setBody(assignmentBody)
            }.status)
            assertTrue(browser.get("/api/v1/admin/reports/$reportId").bodyAsText().contains("Integración vial"))
            assertEquals(HttpStatusCode.Conflict, browser.put("/api/v1/admin/reports/$reportId/assignment") {
                contentType(ContentType.Application.Json)
                header("X-CSRF-Token", csrf)
                setBody(assignmentBody)
            }.status)

            assertEquals(HttpStatusCode.NoContent, browser.post("/api/v1/admin/auth/logout") {
                header("X-CSRF-Token", csrf)
            }.status)
            assertEquals(HttpStatusCode.Unauthorized, browser.get("/api/v1/admin/auth/me").status)

            assertEquals(HttpStatusCode.OK, browser.post("/api/v1/admin/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"integration-admin","password":"$password"}""")
            }.status)
            execute("UPDATE admin_users SET is_active = FALSE WHERE id = ?", adminId)
            connection.commit()
            assertEquals(
                HttpStatusCode.Unauthorized,
                browser.get("/api/v1/admin/auth/me").status,
                "Una cuenta administrativa desactivada no debe conservar acceso con una cookie previa",
            )
        } finally {
            connection.rollback()
            execute("DELETE FROM audit_events WHERE actor_admin_user_id = ?", adminId)
            execute("DELETE FROM report_assignments WHERE report_id = ?", reportId)
            execute("DELETE FROM report_status_history WHERE report_id = ?", reportId)
            execute("DELETE FROM admin_sessions WHERE admin_user_id = ?", adminId)
            execute("DELETE FROM reports WHERE id = ?", reportId)
            execute("DELETE FROM teams WHERE id = ?", teamId)
            execute("DELETE FROM admin_users WHERE id = ?", adminId)
            connection.commit()
        }
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
        assertEquals(4, flyway(url).migrate().migrationsExecuted)
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

            val adminQueries = AdminReportQueryRepositoryImpl()
            val adminDetail = assertNotNull(adminQueries.findDetail(report.id))
            assertEquals(report.latitude, adminDetail.latitude, 0.000001)
            assertEquals(report.longitude, adminDetail.longitude, 0.000001)
            val adminList = adminQueries.list(AdminReportFilter(limit = 50))
            assertTrue(adminList.items.any { it.id == report.id && it.latitude == report.latitude && it.longitude == report.longitude })
            assertTrue(StatisticsRepositoryImpl().get().totalReports >= 1)

            if (report.userId != null) {
                val byUser = restartedRepository.findByUserId(report.userId, cursor = null, limit = 20)
                assertTrue(byUser.any { it.id == report.id && it.latitude == report.latitude && it.longitude == report.longitude })
                val ownList = ListUserReportsUseCase(
                    restartedRepository, CategoryRepositoryImpl(), ImageRepositoryImpl(),
                ).execute(report.userId, cursor = null)
                assertTrue(ownList.items.any { it.id == report.id && it.latitude == report.latitude && it.longitude == report.longitude })
                val ownDetail = GetOwnReportUseCase(restartedRepository, adminQueries).execute(report.userId, report.id)
                val json = Json.encodeToString(ownDetail)
                assertTrue(json.contains("\"latitude\":${report.latitude}"))
                assertTrue(json.contains("\"longitude\":${report.longitude}"))
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
