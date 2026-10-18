package com.mapaurbano.audit.api

import com.mapaurbano.audit.application.ListAuditEventsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.auditRoutes() {
    val listAuditEventsUseCase by inject<ListAuditEventsUseCase>()
    get("/audit") {
        val cursor = call.request.queryParameters["cursor"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        call.respond(HttpStatusCode.OK, listAuditEventsUseCase.execute(cursor, limit))
    }
}
