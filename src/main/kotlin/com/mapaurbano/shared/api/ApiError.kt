package com.mapaurbano.shared.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ApiErrorEnvelope(
    val error: ApiError,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: List<ApiErrorDetail> = emptyList(),
    val requestId: String,
)

@Serializable
data class ApiErrorDetail(
    val field: String? = null,
    val reason: String,
)

suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    code: String,
    message: String,
    details: List<ApiErrorDetail> = emptyList(),
) {
    respond(
        status,
        ApiErrorEnvelope(
            error = ApiError(
                code = code,
                message = message,
                details = details,
                requestId = requestId(),
            ),
        ),
    )
}

suspend fun ApplicationCall.respondInternalServerError() {
    respondApiError(
        status = HttpStatusCode.InternalServerError,
        code = "INTERNAL_ERROR",
        message = "No pudimos procesar la solicitud.",
    )
}

private fun ApplicationCall.requestId(): String {
    val suppliedRequestId = request.header("X-Request-ID")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it.length <= 128 }

    return suppliedRequestId ?: UUID.randomUUID().toString()
}
