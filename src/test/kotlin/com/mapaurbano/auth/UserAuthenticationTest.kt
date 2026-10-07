package com.mapaurbano.auth

import com.mapaurbano.application.*
import com.mapaurbano.auth.application.*
import com.mapaurbano.auth.dto.LoginRequest
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.application.CreateReportUseCaseTest
import com.mapaurbano.categories.domain.CategoryRepository
import io.ktor.client.request.forms.*
import com.mapaurbano.shared.domain.*
import com.mapaurbano.users.api.userRoutes
import com.mapaurbano.users.application.*
import com.mapaurbano.users.domain.*
import com.mapaurbano.users.dto.RegisterRequest
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.time.Instant
import kotlin.test.*

class UserAuthenticationTest {
    private class Users : UserRepository {
        val entries = mutableMapOf<String, User>()
        override suspend fun findById(id: String) = entries[id]
        override suspend fun findByDni(dni: String) = entries.values.find { it.dni == dni }
        override suspend fun create(user: User): User { entries[user.id] = user; return user }
        override suspend fun deactivate(id: String) { entries[id]?.let { entries[id] = it.copy(isActive = false) } }
    }

    private class Sessions : SessionRepository {
        val entries = mutableMapOf<String, UserSession>()
        override suspend fun createSession(session: UserSession): UserSession { entries[session.id] = session; return session }
        override suspend fun findByTokenHash(tokenHash: ByteArray) = entries.values.find { it.tokenHash.contentEquals(tokenHash) }
        override suspend fun revokeSession(sessionId: String, revokedAt: Instant) {
            entries[sessionId]?.let { entries[sessionId] = it.copy(revokedAt = revokedAt) }
        }
        override suspend fun revokeAllForUser(userId: String, revokedAt: Instant) {
            entries.values.filter { it.userId == userId }.forEach { revokeSession(it.id, revokedAt) }
        }
        override suspend fun touchLastUsed(sessionId: String, lastUsedAt: Instant) {
            entries[sessionId]?.let { entries[sessionId] = it.copy(lastUsedAt = lastUsedAt) }
        }
    }

    @Test fun `DNI se normaliza sin aceptar letras ni documentos invalidos`() {
        assertEquals("30123456", Dni.normalize(" 30.123.456 "))
        assertEquals("1234567", Dni.normalize("1.234.567"))
        assertEquals("01234567", Dni.normalize("01234567"))
        listOf("", "123456", "123456789", "00000000", "30abc123456", "30-123-456", "3.0123456", "３０１２３４５６").forEach {
            assertNull(Dni.normalize(it), it)
        }
    }

    @Test fun `registro rechaza DNI duplicado normalizado y claves fuera de limite`() = runBlocking {
        val users = Users()
        val sessions = Sessions()
        val register = RegisterUserUseCase(users, sessions)
        val request = RegisterRequest("30.123.456", " Vecino ", "password123")
        register.execute(request)
        assertEquals("30123456", users.entries.values.single().dni)
        assertEquals("Vecino", users.entries.values.single().displayName)
        assertFailsWith<ConflictException> { register.execute(request.copy(dni = "30123456")) }
        for (bad in listOf(request.copy(dni = "abc"), request.copy(password = "short"), request.copy(password = "á".repeat(37)))) {
            assertFailsWith<ValidationException> { register.execute(bad) }
        }
        assertEquals(1, users.entries.size)
        assertEquals(1, sessions.entries.size)
    }

    @Test fun `login tiene error generico y bearer rechaza token falso vencido revocado o cuenta inactiva`() = runBlocking {
        val users = Users()
        val sessions = Sessions()
        val created = RegisterUserUseCase(users, sessions).execute(RegisterRequest("30123456", "Vecino", "password123"))
        val login = LoginUserUseCase(users, sessions)
        val auth = AuthenticateUserUseCase(users, sessions)
        val valid = auth.execute(created.token)!!
        assertEquals(created.id, valid.userId)
        assertNotEquals(created.token, valid.sessionId)
        assertNull(auth.execute(created.id))
        assertNull(auth.execute("x".repeat(43)))
        val errors = listOf(LoginRequest("30123456", "wrong"), LoginRequest("99999999", "password123"), LoginRequest("abc", "password123"))
            .map { assertFailsWith<AuthenticationException> { login.execute(it) }.message }
        assertEquals(1, errors.toSet().size)
        val loginResponse = login.execute(LoginRequest("30.123.456", "password123"))
        assertEquals(created.id, auth.execute(loginResponse.token)?.userId)
        val session = sessions.entries.getValue(valid.sessionId)
        sessions.entries[session.id] = session.copy(expiresAt = Instant.now().minusSeconds(1))
        assertNull(auth.execute(created.token))
        sessions.entries[session.id] = session
        LogoutUserUseCase(sessions).execute(session.id)
        assertNull(auth.execute(created.token))
        users.deactivate(created.id)
        assertNull(auth.execute(loginResponse.token))
        assertEquals(errors.first(), assertFailsWith<AuthenticationException> { login.execute(LoginRequest("30123456", "password123")) }.message)
        users.entries[created.id] = users.entries.getValue(created.id).copy(isActive = true, dni = null)
        assertNull(auth.execute(loginResponse.token))
    }

    @Test fun `HTTP registro login perfil logout y baja usan DNI y sesion real`() = testApplication {
        val users = Users()
        val sessions = Sessions()
        val reports = CreateReportUseCaseTest.FakeReportRepository()
        application {
            install(Koin) {
                modules(repositoryModule, applicationModule, module {
                    single<UserRepository> { users }
                    single<SessionRepository> { sessions }
                    single<ReportRepository> { reports }
                    single<CategoryRepository> { CreateReportUseCaseTest.FakeCategoryRepository() }
                })
            }
            configurePlugins()
            configureSecurity()
            configureRateLimiting()
            configureRouting()
        }
        val invalid = client.post("/api/v1/users/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"old@example.com","displayName":"Vecino","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        val registered = client.post("/api/v1/users/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"dni":"30.123.456","displayName":"Vecino","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.Created, registered.status)
        val token = Json.parseToJsonElement(registered.bodyAsText()).jsonObject.getValue("token").jsonPrimitive.content
        val profile = client.get("/api/v1/users/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, profile.status)
        val profileJson = Json.parseToJsonElement(profile.bodyAsText()).jsonObject
        assertEquals("30123456", profileJson.getValue("dni").jsonPrimitive.content)
        assertFalse(profileJson.containsKey("passwordHash"))
        assertFalse(profileJson.containsKey("email"))
        val another = client.post("/api/v1/users/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"dni":"32123456","displayName":"Otro vecino","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.Created, another.status)
        val anotherToken = Json.parseToJsonElement(another.bodyAsText()).jsonObject.getValue("token").jsonPrimitive.content
        val anotherProfile = client.get("/api/v1/users/me") { bearerAuth(anotherToken) }.bodyAsText()
        assertFalse(anotherProfile.contains("30123456"))
        assertTrue(anotherProfile.contains("32123456"))
        fun reportForm(mode: String) = MultiPartFormDataContent(formData {
            append("title", "Bache")
            append("description", "Pozo en la calzada")
            append("categorySlug", "bach")
            append("latitude", "-34.0")
            append("longitude", "-58.0")
            append("submissionMode", mode)
        })
        assertEquals(HttpStatusCode.BadRequest, client.post("/api/v1/reports") { setBody(reportForm("account")) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/reports") { bearerAuth("forged"); setBody(reportForm("account")) }.status)
        val accountReport = client.post("/api/v1/reports") { bearerAuth(token); setBody(reportForm("account")) }
        assertEquals(HttpStatusCode.Created, accountReport.status)
        assertEquals(profileJson.getValue("id").jsonPrimitive.content, reports.reports.single().userId)
        assertFalse(accountReport.bodyAsText().contains("30123456"))
        assertEquals(HttpStatusCode.Created, client.post("/api/v1/reports") { bearerAuth(token); setBody(reportForm("anonymous")) }.status)
        assertNull(reports.reports.last().userId)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users/me") { bearerAuth("forged") }.status)
        assertEquals(HttpStatusCode.NoContent, client.post("/api/v1/users/logout") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users/me") { bearerAuth(token) }.status)
        val loggedIn = client.post("/api/v1/users/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"dni":"30123456","password":"password123"}""")
        }
        assertEquals(HttpStatusCode.OK, loggedIn.status)
        val newToken = Json.parseToJsonElement(loggedIn.bodyAsText()).jsonObject.getValue("token").jsonPrimitive.content
        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/users/me") { bearerAuth(newToken) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users/me") { bearerAuth(newToken) }.status)
    }
}
