package com.mapaurbano.reports.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.NotFoundException
import java.time.Instant
import java.util.UUID
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ChangeReportPriorityUseCase(
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
    private val eventBus: EventBus,
) {
    suspend fun execute(
        reportId: String,
        newPriorityString: String,
        dueAt: Instant?,
        version: Long,
        adminUserId: String
    ): Report {
        if (version < 0) throw com.mapaurbano.shared.domain.ValidationException("La versión no puede ser negativa")
        val newPriority = try {
            ReportPriority.valueOf(newPriorityString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw com.mapaurbano.shared.domain.ValidationException(message = "Prioridad inválida")
        }

        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        if (report.priority == newPriority && report.dueAt == dueAt) {
            return report
        }

        val updated = reportRepository.updatePriority(
            reportId, report.priority, newPriority, report.dueAt, dueAt, version, adminUserId,
        )
        if (!updated) {
            throw ConflictException("El reporte ha sido modificado por otro usuario. Recargue los datos.")
        }

        auditRepository.create(
            AuditEvent(
                id = UUID.randomUUID().toString(),
                actorAdminUserId = adminUserId,
                action = "priority_changed",
                entityType = "report",
                entityId = reportId,
                metadata = """{"from": "${report.priority.name}", "to": "${newPriority.name}"}""",
                occurredAt = Instant.now()
            )
        )

        val updatedReport = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado después de actualizar")
        eventBus.publish(
            WsEvent(
                type = "report.priority_changed",
                occurredAt = Instant.now().toString(),
                payload = buildJsonObject {
                    put("reportId", reportId)
                    put("priority", newPriority.name)
                    put("version", updatedReport.version)
                },
            ),
        )
        return updatedReport
    }
}
