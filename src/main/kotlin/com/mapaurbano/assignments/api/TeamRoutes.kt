package com.mapaurbano.assignments.api

import com.mapaurbano.assignments.application.CreateTeamUseCase
import com.mapaurbano.assignments.application.ManageTeamMembersUseCase
import com.mapaurbano.assignments.application.UpdateTeamUseCase
import com.mapaurbano.assignments.application.TeamQueryUseCase
import com.mapaurbano.application.AdminPrincipal
import com.mapaurbano.application.hasValidAdminCsrf
import com.mapaurbano.shared.api.respondApiError
import com.mapaurbano.assignments.dto.CreateTeamRequest
import com.mapaurbano.assignments.dto.UpdateTeamRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.auth.principal
import org.koin.ktor.ext.inject

fun Route.teamRoutes() {
    val createTeamUseCase by inject<CreateTeamUseCase>()
    val updateTeamUseCase by inject<UpdateTeamUseCase>()
    val manageTeamMembersUseCase by inject<ManageTeamMembersUseCase>()
    val teamQueryUseCase by inject<TeamQueryUseCase>()

    route("/teams") {
        get {
            call.respond(HttpStatusCode.OK, teamQueryUseCase.list())
        }

        post {
            if (!call.requireTeamCsrf()) return@post
            val request = call.receive<CreateTeamRequest>()
            val response = createTeamUseCase.execute(request)
            call.respond(HttpStatusCode.Created, response)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            call.respond(HttpStatusCode.OK, teamQueryUseCase.detail(id))
        }

        patch("/{id}") {
            if (!call.requireTeamCsrf()) return@patch
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val request = call.receive<UpdateTeamRequest>()
            val response = updateTeamUseCase.execute(id, request)
            call.respond(HttpStatusCode.OK, response)
        }

        put("/{teamId}/members/{adminUserId}") {
            if (!call.requireTeamCsrf()) return@put
            val teamId = call.parameters["teamId"] ?: throw IllegalArgumentException("teamId is required")
            val targetUserId = call.parameters["adminUserId"] ?: throw IllegalArgumentException("adminUserId is required")

            manageTeamMembersUseCase.addMember(teamId, targetUserId)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{teamId}/members/{adminUserId}") {
            if (!call.requireTeamCsrf()) return@delete
            val teamId = call.parameters["teamId"] ?: throw IllegalArgumentException("teamId is required")
            val targetUserId = call.parameters["adminUserId"] ?: throw IllegalArgumentException("adminUserId is required")

            manageTeamMembersUseCase.removeMember(teamId, targetUserId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    get("/assignees") {
        call.respond(HttpStatusCode.OK, teamQueryUseCase.assignees())
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.requireTeamCsrf(): Boolean {
    val principal = principal<AdminPrincipal>() ?: run {
        respond(HttpStatusCode.Unauthorized)
        return false
    }
    if (!hasValidAdminCsrf(principal)) {
        respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
        return false
    }
    return true
}
