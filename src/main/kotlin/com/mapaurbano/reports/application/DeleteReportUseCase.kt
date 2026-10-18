package com.mapaurbano.reports.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

class DeleteReportUseCase(
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
    private val eventBus: EventBus,
) {
    suspend fun execute(reportId: String, adminUserId: String): Boolean {
        reportRepository.findById(reportId) ?: throw NotFoundException("Reporte no encontrado")
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

        eventBus.publish(WsEvent(
            type = "report.deleted",
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject { put("reportId", reportId) },
        ))
        return true
    }
}
