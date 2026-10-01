package com.mapaurbano.assignments.api

import com.mapaurbano.application.AdminSession
import com.mapaurbano.assignments.application.CreateTeamUseCase
import com.mapaurbano.assignments.application.ManageTeamMembersUseCase
import com.mapaurbano.assignments.application.UpdateTeamUseCase
import com.mapaurbano.assignments.dto.CreateTeamRequest
import com.mapaurbano.assignments.dto.UpdateTeamRequest
import com.mapaurbano.shared.api.respondEndpointNotImplemented
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
import org.koin.ktor.ext.inject

fun Route.teamRoutes() {
    val createTeamUseCase by inject<CreateTeamUseCase>()
    val updateTeamUseCase by inject<UpdateTeamUseCase>()
    val manageTeamMembersUseCase by inject<ManageTeamMembersUseCase>()

    route("/teams") {
        get {
            call.respondEndpointNotImplemented("listTeams")
        }

        post {
            val request = call.receive<CreateTeamRequest>()
            val response = createTeamUseCase.execute(request)
            call.respond(HttpStatusCode.Created, response)
        }

        get("/{id}") {
            call.respondEndpointNotImplemented("getTeam")
        }

        patch("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val request = call.receive<UpdateTeamRequest>()
            val response = updateTeamUseCase.execute(id, request)
            call.respond(HttpStatusCode.OK, response)
        }

        put("/{teamId}/members/{adminUserId}") {
            val teamId = call.parameters["teamId"] ?: throw IllegalArgumentException("teamId is required")
            val targetUserId = call.parameters["adminUserId"] ?: throw IllegalArgumentException("adminUserId is required")

            manageTeamMembersUseCase.addMember(teamId, targetUserId)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{teamId}/members/{adminUserId}") {
            val teamId = call.parameters["teamId"] ?: throw IllegalArgumentException("teamId is required")
            val targetUserId = call.parameters["adminUserId"] ?: throw IllegalArgumentException("adminUserId is required")

            manageTeamMembersUseCase.removeMember(teamId, targetUserId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    get("/assignees") {
        call.respondEndpointNotImplemented("listAssignees")
    }
}
