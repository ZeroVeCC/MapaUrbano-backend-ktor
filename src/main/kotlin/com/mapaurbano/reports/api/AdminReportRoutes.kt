package com.mapaurbano.reports.api

import com.mapaurbano.application.AdminPrincipal
import com.mapaurbano.application.hasValidAdminCsrf
import com.mapaurbano.reports.application.AdminReportQueryUseCase
import com.mapaurbano.reports.application.ChangeReportPriorityUseCase
import com.mapaurbano.reports.application.ChangeReportStatusUseCase
import com.mapaurbano.reports.application.DeleteReportUseCase
import com.mapaurbano.reports.dto.UpdatePriorityRequest
import com.mapaurbano.reports.dto.UpdateStatusRequest
import com.mapaurbano.reports.dto.ReportMutationResponse
import com.mapaurbano.shared.api.respondApiError
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
    val queryUseCase by inject<AdminReportQueryUseCase>()

    route("/reports") {
        get {
            val response = queryUseCase.list(
                status = call.request.queryParameters["status"],
                category = call.request.queryParameters["categorySlug"],
                priority = call.request.queryParameters["priority"],
                teamId = call.request.queryParameters["teamId"],
                responsibleAdminUserId = call.request.queryParameters["responsibleAdminUserId"],
                search = call.request.queryParameters["search"],
                cursor = call.request.queryParameters["cursor"],
                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
            )
            call.respond(HttpStatusCode.OK, response)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            call.respond(HttpStatusCode.OK, queryUseCase.detail(id))
        }

        patch("/{id}/status") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val request = call.receive<UpdateStatusRequest>()
            val session = call.principal<AdminPrincipal>()
                ?: return@patch call.respond(HttpStatusCode.Unauthorized)
            if (!call.hasValidAdminCsrf(session)) {
                return@patch call.respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
            }

            val updatedReport = changeReportStatusUseCase.execute(
                reportId = id,
                newStatusString = request.status,
                version = request.version,
                note = request.note,
                adminUserId = session.adminUserId
            )
            call.respond(HttpStatusCode.OK, updatedReport.toMutationResponse())
        }

        patch("/{id}/priority") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val request = call.receive<UpdatePriorityRequest>()
            val session = call.principal<AdminPrincipal>()
                ?: return@patch call.respond(HttpStatusCode.Unauthorized)
            if (!call.hasValidAdminCsrf(session)) {
                return@patch call.respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
            }

            val dueAt = request.dueAt?.let {
                runCatching { Instant.parse(it) }
                    .getOrElse { throw com.mapaurbano.shared.domain.ValidationException("dueAt debe ser una fecha ISO-8601 válida") }
            }

            val updatedReport = changeReportPriorityUseCase.execute(
                reportId = id,
                newPriorityString = request.priority,
                dueAt = dueAt,
                version = request.version,
                adminUserId = session.adminUserId
            )
            call.respond(HttpStatusCode.OK, updatedReport.toMutationResponse())
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val session = call.principal<AdminPrincipal>()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized)
            if (!call.hasValidAdminCsrf(session)) {
                return@delete call.respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
            }

            deleteReportUseCase.execute(id, session.adminUserId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun com.mapaurbano.reports.domain.Report.toMutationResponse() = ReportMutationResponse(
    id = id,
    status = status.name,
    priority = priority.name,
    dueAt = dueAt?.toString(),
    version = version,
    updatedAt = updatedAt.toString(),
)

