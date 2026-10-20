package com.mapaurbano.application

import com.mapaurbano.shared.api.ApiErrorDetail
import com.mapaurbano.shared.api.respondApiError
import com.mapaurbano.shared.api.respondInternalServerError
import com.mapaurbano.shared.domain.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.websocket.WebSockets
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

fun Application.configurePlugins() {
    val applicationLogger = environment.log
    val allowedHosts = environment.config.propertyOrNull("security.cors.allowedHosts")?.getString()
        ?: "localhost:3000,localhost:4200,localhost:5173"
    val parsedAllowedHosts = allowedHosts
        .split(',').map(String::trim).filter(String::isNotEmpty)
    val sessionSecret = environment.config.propertyOrNull("security.session.secret")?.getString()
        ?: "local-development-secret-change-me"
    require(sessionSecret.length >= 32) {
        "SESSION_SECRET debe contener al menos 32 caracteres."
    }
    val secureCookie = environment.config.propertyOrNull("security.session.secureCookie")?.getString()?.toBoolean() ?: false

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-CSRF-Token")
        allowCredentials = true
        parsedAllowedHosts.forEach { allowHost(it) }
    }

    install(Sessions) {
        cookie<AdminSessionCookie>(ADMIN_SESSION_COOKIE) {
            // La misma sesión protege REST (/admin/**) y el handshake WebSocket
            // (/ws/admin). Un path más estrecho impide que el navegador envíe la
            // cookie al WebSocket aunque el login haya sido correcto.
            cookie.path = "/api/v1"
            cookie.httpOnly = true
            cookie.secure = secureCookie
            cookie.extensions["SameSite"] = "Lax"
            transform(SessionTransportTransformerMessageAuthentication(sessionSecret.toByteArray()))
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                ignoreUnknownKeys = false
                prettyPrint = false
            },
        )
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
            call.respondApiError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Revisa el formato y los campos enviados.")
        }
        exception<DomainException> { call, cause ->
            val status = when (cause) {
                is NotFoundException -> HttpStatusCode.NotFound
                is ValidationException -> HttpStatusCode.BadRequest
                is ConflictException -> HttpStatusCode.Conflict
                is AuthenticationException -> HttpStatusCode.Unauthorized
                is AuthorizationException -> HttpStatusCode.Forbidden
                is PersistenceException -> HttpStatusCode.InternalServerError
            }
            if (cause is PersistenceException) {
                applicationLogger.error("Controlled persistence failure [${cause.errorCode}]", cause)
            }
            val details = cause.details.map { ApiErrorDetail(it.field, it.reason) }
            call.respondApiError(status, cause.errorCode, cause.message, details)
        }
        exception<ExposedSQLException> { call, cause ->
            applicationLogger.error("Unhandled database persistence failure", cause)
            call.respondApiError(
                HttpStatusCode.InternalServerError,
                "PERSISTENCE_ERROR",
                "No pudimos guardar los datos. Intentá nuevamente.",
            )
        }
        exception<Throwable> { call, cause ->
            applicationLogger.error("Unhandled request failure", cause)
            call.respondInternalServerError()
        }
    }

    install(WebSockets) {
        pingPeriodMillis = 20.seconds.inWholeMilliseconds
        timeoutMillis = 15.seconds.inWholeMilliseconds
        maxFrameSize = 1L * 1024 * 1024
        masking = false
    }
}

