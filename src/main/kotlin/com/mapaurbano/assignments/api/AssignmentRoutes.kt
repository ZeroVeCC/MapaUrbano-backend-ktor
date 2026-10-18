package com.mapaurbano.assignments.api

import com.mapaurbano.application.AdminPrincipal
import com.mapaurbano.application.hasValidAdminCsrf
import com.mapaurbano.shared.api.respondApiError
import com.mapaurbano.assignments.application.AssignReportUseCase
import com.mapaurbano.assignments.application.UnassignReportUseCase
import com.mapaurbano.reports.dto.AssignmentRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject

fun Route.assignmentRoutes() {
    val assignReportUseCase by inject<AssignReportUseCase>()
    val unassignReportUseCase by inject<UnassignReportUseCase>()

    put("/reports/{id}/assignment") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
        val request = call.receive<AssignmentRequest>()
        val session = call.principal<AdminPrincipal>()
            ?: return@put call.respond(HttpStatusCode.Unauthorized)
        if (!call.hasValidAdminCsrf(session)) {
            return@put call.respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
        }

        assignReportUseCase.execute(
            reportId = id,
            teamId = request.teamId,
            responsibleAdminUserId = request.responsibleAdminUserId,
            version = request.version,
            adminUserId = session.adminUserId
        )
        call.respond(HttpStatusCode.NoContent)
    }

    delete("/reports/{id}/assignment") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
        val version = call.request.queryParameters["version"]?.toLongOrNull()
            ?: throw com.mapaurbano.shared.domain.ValidationException(message = "version es obligatorio")
        val session = call.principal<AdminPrincipal>()
            ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        if (!call.hasValidAdminCsrf(session)) {
            return@delete call.respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
        }

        unassignReportUseCase.execute(id, version, session.adminUserId)
        call.respond(HttpStatusCode.NoContent)
    }
}
