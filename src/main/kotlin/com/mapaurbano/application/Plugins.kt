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
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

fun Application.configurePlugins() {
    val applicationLogger = environment.log


    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost("localhost:3000")
        allowHost("localhost:4200")
        allowHost("localhost:5173")
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

