package com.mapaurbano.reports.api

import com.mapaurbano.application.AdminSession
import com.mapaurbano.reports.application.ChangeReportPriorityUseCase
import com.mapaurbano.reports.application.ChangeReportStatusUseCase
import com.mapaurbano.reports.application.DeleteReportUseCase
import com.mapaurbano.reports.dto.UpdatePriorityRequest
import com.mapaurbano.reports.dto.UpdateStatusRequest
import com.mapaurbano.shared.api.respondEndpointNotImplemented
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.time.Instant

fun Route.adminReportRoutes() {
    val changeReportStatusUseCase by inject<ChangeReportStatusUseCase>()
    val changeReportPriorityUseCase by inject<ChangeReportPriorityUseCase>()
    val deleteReportUseCase by inject<DeleteReportUseCase>()

    route("/reports") {
        get {
            call.respondEndpointNotImplemented("listAdminReports")
        }

        get("/{id}") {
            call.respondEndpointNotImplemented("getAdminReport")
        }

        patch("/{id}/status") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val request = call.receive<UpdateStatusRequest>()
            val session = call.principal<AdminSession>()
                ?: return@patch call.respond(HttpStatusCode.Unauthorized)

            changeReportStatusUseCase.execute(
                reportId = id,
                newStatusString = request.status,
                version = request.version,
                note = request.note,
                adminUserId = session.userId
            )
            call.respond(HttpStatusCode.NoContent)
        }

        patch("/{id}/priority") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val request = call.receive<UpdatePriorityRequest>()
            val session = call.principal<AdminSession>()
                ?: return@patch call.respond(HttpStatusCode.Unauthorized)

            val dueAt = request.dueAt?.let { Instant.parse(it) }

            changeReportPriorityUseCase.execute(
                reportId = id,
                newPriorityString = request.priority,
                dueAt = dueAt,
                version = request.version,
                adminUserId = session.userId
            )
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val session = call.principal<AdminSession>()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized)

            deleteReportUseCase.execute(id, session.userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
