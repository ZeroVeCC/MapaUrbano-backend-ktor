package com.mapaurbano.reports.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.NotFoundException
import java.time.Instant
import java.util.UUID

class ChangeReportPriorityUseCase(
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
) {
    suspend fun execute(
        reportId: String,
        newPriorityString: String,
        dueAt: Instant?,
        version: Long,
        adminUserId: String
    ): Boolean {
        val newPriority = try {
            ReportPriority.valueOf(newPriorityString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw com.mapaurbano.shared.domain.ValidationException(message = "Prioridad inválida")
        }

        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        if (report.priority == newPriority && report.dueAt == dueAt) {
            return true // Idempotent
        }

        val updated = reportRepository.updatePriority(reportId, newPriority, dueAt, version)
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

        // TODO: Publicar evento
        return true
    }
}
