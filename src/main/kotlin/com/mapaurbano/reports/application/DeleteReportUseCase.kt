package com.mapaurbano.reports.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException
import java.time.Instant
import java.util.UUID

class DeleteReportUseCase(
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
) {
    suspend fun execute(reportId: String, adminUserId: String): Boolean {
        val deleted = reportRepository.softDelete(reportId)
        
        if (!deleted) {
            throw NotFoundException("Reporte no encontrado")
        }

        auditRepository.create(
            AuditEvent(
                id = UUID.randomUUID().toString(),
                actorAdminUserId = adminUserId,
                action = "deleted",
                entityType = "report",
                entityId = reportId,
                occurredAt = Instant.now()
            )
        )

        // TODO: Publicar evento
        return true
    }
}
