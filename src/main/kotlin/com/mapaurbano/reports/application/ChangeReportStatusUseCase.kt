package com.mapaurbano.reports.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.NotFoundException
import java.time.Instant
import java.util.UUID

class ChangeReportStatusUseCase(
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
) {
    suspend fun execute(
        reportId: String,
        newStatusString: String,
        version: Long,
        note: String?,
        adminUserId: String
    ): Boolean {
        val newStatus = try {
            ReportStatus.valueOf(newStatusString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw com.mapaurbano.shared.domain.ValidationException(message = "Estado inválido")
        }

        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        if (report.status == newStatus) {
            return true // Idempotent
        }

        val updated = reportRepository.updateStatus(reportId, newStatus, version)
        if (!updated) {
            throw ConflictException("El reporte ha sido modificado por otro usuario. Recargue los datos.")
        }

        auditRepository.create(
            AuditEvent(
                id = UUID.randomUUID().toString(),
                actorAdminUserId = adminUserId,
                action = "status_changed",
                entityType = "report",
                entityId = reportId,
                metadata = """{"from": "${report.status.name}", "to": "${newStatus.name}", "note": "${note ?: ""}"}""",
                occurredAt = Instant.now()
            )
        )

        // TODO: Publicar evento
        return true
    }
}
