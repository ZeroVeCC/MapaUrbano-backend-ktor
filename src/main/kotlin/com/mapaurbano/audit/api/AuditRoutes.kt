package com.mapaurbano.audit.api

import com.mapaurbano.shared.api.respondEndpointNotImplemented
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.auditRoutes() {
    get("/audit") {
        call.respondEndpointNotImplemented("listAuditEvents")
    }
}
