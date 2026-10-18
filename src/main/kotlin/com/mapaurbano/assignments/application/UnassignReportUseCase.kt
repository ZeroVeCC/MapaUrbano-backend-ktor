package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ConflictException
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UnassignReportUseCase(
    private val assignmentRepository: AssignmentRepository,
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
    private val eventBus: EventBus
) {
    suspend fun execute(
        reportId: String,
        version: Long,
        adminUserId: String
    ): Boolean {
        if (version < 0) throw com.mapaurbano.shared.domain.ValidationException(message = "La versión no puede ser negativa")
        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        val unassigned = assignmentRepository.unassign(reportId, version)
        if (!unassigned) {
            throw ConflictException("El reporte ha sido modificado por otro usuario. Recargue los datos.")
        }

        auditRepository.create(
            AuditEvent(
                id = UUID.randomUUID().toString(),
                actorAdminUserId = adminUserId,
                action = "unassigned",
                entityType = "report",
                entityId = reportId,
                occurredAt = Instant.now()
            )
        )

        eventBus.publish(WsEvent(
            type = "report.assignment_changed",
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject {
                put("reportId", reportId)
                put("unassigned", true)
            }
        ))

        return true
    }
}
