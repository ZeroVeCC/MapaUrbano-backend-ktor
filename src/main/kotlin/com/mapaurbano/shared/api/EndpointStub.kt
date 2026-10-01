package com.mapaurbano.shared.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

suspend fun ApplicationCall.respondEndpointNotImplemented(operationId: String) {
    respondApiError(
        status = HttpStatusCode.NotImplemented,
        code = "ENDPOINT_NOT_IMPLEMENTED",
        message = "El endpoint '$operationId' está registrado, pero su caso de uso todavía no fue implementado.",
    )
}
