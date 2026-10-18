package com.mapaurbano.health.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Route.healthRoutes() {
    route("/health") {
        get("/live") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        get("/ready") {
            val databaseReady = runCatching {
                newSuspendedTransaction(Dispatchers.IO) {
                    exec("SELECT 1") { result -> result.next() && result.getInt(1) == 1 } == true
                }
            }.getOrDefault(false)
            if (databaseReady) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "DOWN"))
            }
        }
    }
}
