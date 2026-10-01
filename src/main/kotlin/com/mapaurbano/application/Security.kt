package com.mapaurbano.application

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*

fun Application.configureSecurity() {
    install(Authentication) {
        bearer("user-bearer") {
            realm = "Access to user API"
            authenticate { tokenCredential ->
                // TODO: Validate token in repository
                UserIdPrincipal(tokenCredential.token)
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
