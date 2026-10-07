package com.mapaurbano.application

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import com.mapaurbano.auth.application.AuthenticateUserUseCase
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val authenticateUser by inject<AuthenticateUserUseCase>()
    install(Authentication) {
        bearer("user-bearer") {
            realm = "Access to user API"
            authenticate { tokenCredential ->
                authenticateUser.execute(tokenCredential.token)
            }
        }
        
        session<AdminSession>("admin-session") {
            validate { session ->
                // TODO: Validate session against repository
                session
            }
            challenge {
                // TODO: Handle unauthorized admin access
            }
        }
    }
}

data class AdminSession(val id: String, val userId: String) : Principal
