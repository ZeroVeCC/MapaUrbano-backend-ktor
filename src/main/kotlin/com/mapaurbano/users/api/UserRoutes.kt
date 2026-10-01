package com.mapaurbano.users.api

import com.mapaurbano.auth.dto.LoginRequest
import com.mapaurbano.shared.api.respondEndpointNotImplemented
import com.mapaurbano.users.application.DeactivateUserUseCase
import com.mapaurbano.users.application.GetCurrentUserUseCase
import com.mapaurbano.users.application.ListUserReportsUseCase
import com.mapaurbano.users.application.RegisterUserUseCase
import com.mapaurbano.auth.application.LoginUserUseCase
import com.mapaurbano.auth.application.LogoutUserUseCase
import com.mapaurbano.users.dto.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val registerUserUseCase by inject<RegisterUserUseCase>()
    val loginUserUseCase by inject<LoginUserUseCase>()
    val logoutUserUseCase by inject<LogoutUserUseCase>()
    val getCurrentUserUseCase by inject<GetCurrentUserUseCase>()
    val deactivateUserUseCase by inject<DeactivateUserUseCase>()
    val listUserReportsUseCase by inject<ListUserReportsUseCase>()

    route("/users") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = registerUserUseCase.execute(request)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = loginUserUseCase.execute(request)
            call.respond(HttpStatusCode.OK, response)
        }

        authenticate("user-bearer") {
            post("/logout") {
                val token = call.principal<UserIdPrincipal>()?.name
                if (token != null) {
                    logoutUserUseCase.execute(token)
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }

            get("/me") {
                val userId = call.principal<UserIdPrincipal>()?.name
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val response = getCurrentUserUseCase.execute(userId)
                call.respond(HttpStatusCode.OK, response)
            }

            delete("/me") {
                val userId = call.principal<UserIdPrincipal>()?.name
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                deactivateUserUseCase.execute(userId)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/me/reports") {
                val userId = call.principal<UserIdPrincipal>()?.name
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val cursor = call.request.queryParameters["cursor"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val response = listUserReportsUseCase.execute(userId, cursor, limit)
                call.respond(HttpStatusCode.OK, response)
            }

            get("/me/reports/{id}") {
                call.respondEndpointNotImplemented("getCurrentUserReport")
            }
        }
    }
}
