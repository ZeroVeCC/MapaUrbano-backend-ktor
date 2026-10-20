package com.mapaurbano.application

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import com.mapaurbano.auth.application.AuthenticateUserUseCase
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.shared.api.respondApiError
import io.ktor.http.HttpStatusCode
import org.koin.ktor.ext.inject
import com.mapaurbano.auth.domain.AdminSessionRepository
import java.security.MessageDigest
import io.ktor.server.request.header
import kotlinx.serialization.Serializable

fun Application.configureSecurity() {
    val authenticateUser by inject<AuthenticateUserUseCase>()
    val adminSessionRepository by inject<AdminSessionRepository>()
    val adminUserRepository by inject<AdminUserRepository>()
    install(Authentication) {
        bearer("user-bearer") {
            realm = "Access to user API"
            authenticate { tokenCredential ->
                authenticateUser.execute(tokenCredential.token)
            }
        }
        
        session<AdminSessionCookie>("admin-session") {
            validate { cookie ->
                adminSessionRepository.findById(cookie.id)?.let { session ->
                    val admin = adminUserRepository.findById(session.adminUserId)
                    if (admin?.isActive != true) return@validate null
                    AdminPrincipal(
                        sessionId = session.id,
                        adminUserId = session.adminUserId,
                        csrfToken = session.csrfToken,
                    )
                }
            }
            challenge {
                call.respondApiError(
                    HttpStatusCode.Unauthorized,
                    "AUTHENTICATION_REQUIRED",
                    "Se requiere una sesión administrativa válida.",
                )
            }
        }
    }
}

const val ADMIN_SESSION_COOKIE = "mapa_urbano_admin"
const val ADMIN_CSRF_HEADER = "X-CSRF-Token"

@Serializable
data class AdminSessionCookie(val id: String)

data class AdminPrincipal(
    val sessionId: String,
    val adminUserId: String,
    val csrfToken: String,
)

fun io.ktor.server.application.ApplicationCall.hasValidAdminCsrf(principal: AdminPrincipal): Boolean {
    val supplied = request.header(ADMIN_CSRF_HEADER) ?: return false
    return MessageDigest.isEqual(
        supplied.toByteArray(Charsets.UTF_8),
        principal.csrfToken.toByteArray(Charsets.UTF_8),
    )
}
