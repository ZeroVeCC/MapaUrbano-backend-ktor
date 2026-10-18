package com.mapaurbano.auth.api

import com.mapaurbano.application.AdminPrincipal
import com.mapaurbano.application.AdminSessionCookie
import com.mapaurbano.application.hasValidAdminCsrf
import com.mapaurbano.auth.application.LoginAdminUseCase
import com.mapaurbano.auth.application.LogoutAdminUseCase
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.auth.dto.AdminLoginRequest
import com.mapaurbano.auth.dto.AdminSessionResponse
import com.mapaurbano.shared.api.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
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
fun Route.adminLoginRoutes() {
    val loginAdminUseCase by inject<LoginAdminUseCase>()
    post("/login") {
        val request = call.receive<AdminLoginRequest>()
        val (response, session) = loginAdminUseCase.execute(request)
        call.sessions.set(AdminSessionCookie(session.id))
        call.respond(HttpStatusCode.OK, response)
    }
}

fun Route.adminSessionRoutes() {
    val logoutAdminUseCase by inject<LogoutAdminUseCase>()
    val adminUserRepository by inject<AdminUserRepository>()

    post("/logout") {
        val principal = call.principal<AdminPrincipal>()
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        if (!call.hasValidAdminCsrf(principal)) {
            return@post call.respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
        }
        logoutAdminUseCase.execute(principal.sessionId)
        call.sessions.clear<AdminSessionCookie>()
        call.respond(HttpStatusCode.NoContent)
    }

    get("/me") {
        val principal = call.principal<AdminPrincipal>()
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val admin = adminUserRepository.findById(principal.adminUserId)
            ?: return@get call.respond(HttpStatusCode.Unauthorized)
        call.respond(
            HttpStatusCode.OK,
            AdminSessionResponse(admin.id, admin.username, principal.csrfToken),
        )
    }
}
