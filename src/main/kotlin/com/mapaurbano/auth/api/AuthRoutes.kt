package com.mapaurbano.auth.api

import com.mapaurbano.application.AdminSession
import com.mapaurbano.auth.application.LoginAdminUseCase
import com.mapaurbano.auth.application.LogoutAdminUseCase
import com.mapaurbano.auth.dto.AdminLoginRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.koin.ktor.ext.inject
fun Route.adminAuthRoutes() {
    val loginAdminUseCase by inject<LoginAdminUseCase>()
    val logoutAdminUseCase by inject<LogoutAdminUseCase>()

    route("/auth") {
        post("/login") {
            val request = call.receive<AdminLoginRequest>()
            val (response, session) = loginAdminUseCase.execute(request)
            call.sessions.set(AdminSession(session.id, session.adminUserId))
            call.respond(HttpStatusCode.OK, response)
        }

        post("/logout") {
            val session = call.sessions.get<AdminSession>()
            if (session != null) {
                logoutAdminUseCase.execute(session.id)
                call.sessions.clear<AdminSession>()
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get("/me") {
            val session = call.sessions.get<AdminSession>()
            if (session != null) {
                call.respond(HttpStatusCode.OK, mapOf("adminUserId" to session.userId))
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
