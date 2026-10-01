package com.mapaurbano.health.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.healthRoutes() {
    route("/health") {
        get("/live") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        get("/ready") {
            // TODO: Verify DB connection
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
    }
}
