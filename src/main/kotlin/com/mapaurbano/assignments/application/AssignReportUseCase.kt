package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.assignments.domain.ReportAssignment
import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.ValidationException
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AssignReportUseCase(
    private val assignmentRepository: AssignmentRepository,
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
    private val teamRepository: TeamRepository,
    private val adminUserRepository: AdminUserRepository,
    private val eventBus: EventBus
) {
    suspend fun execute(
        reportId: String,
        teamId: String?,
        responsibleAdminUserId: String?,
        version: Long,
        adminUserId: String
    ): Boolean {
        if (version < 0) throw ValidationException(message = "La versión no puede ser negativa")
        if (teamId == null && responsibleAdminUserId == null) {
            throw ValidationException(message = "Se requiere al menos un equipo o un responsable")
        }

        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        if (teamId != null && teamRepository.findById(teamId)?.isActive != true) {
            throw ValidationException(message = "El equipo no existe o está inactivo")
        }
        if (responsibleAdminUserId != null && adminUserRepository.findById(responsibleAdminUserId)?.isActive != true) {
            throw ValidationException(message = "El responsable no existe o está inactivo")
        }

        val newAssignment = ReportAssignment(
            id = UUID.randomUUID().toString(),
            reportId = report.id,
            teamId = teamId,
            responsibleAdminUserId = responsibleAdminUserId,
            assignedBy = adminUserId,
            assignedAt = Instant.now()
        )

        if (!assignmentRepository.replace(newAssignment, version)) {
            throw ConflictException("El reporte ha sido modificado por otro usuario. Recargue los datos.")
        }

        auditRepository.create(
            AuditEvent(
                id = UUID.randomUUID().toString(),
                actorAdminUserId = adminUserId,
                action = "assignment_changed",
                entityType = "report",
                entityId = reportId,
                metadata = """{"teamId": "$teamId", "responsibleAdminUserId": "$responsibleAdminUserId"}""",
                occurredAt = Instant.now()
            )
        )

        eventBus.publish(WsEvent(
            type = "report.assignment_changed",
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject {
                put("reportId", reportId)
                teamId?.let { put("teamId", it) }
                responsibleAdminUserId?.let { put("responsibleAdminUserId", it) }
            }
        ))

        return true
    }
}
