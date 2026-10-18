package com.mapaurbano.reports.application

import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.NotFoundException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

class ChangeReportStatusUseCase(
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository,
    private val eventBus: EventBus
) {
    suspend fun execute(
        reportId: String,
        newStatusString: String,
        version: Long,
        note: String?,
        adminUserId: String
    ): Report {
        if (version < 0) throw com.mapaurbano.shared.domain.ValidationException("La versión no puede ser negativa")
        val newStatus = try {
            ReportStatus.valueOf(newStatusString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw com.mapaurbano.shared.domain.ValidationException(message = "Estado inválido")
        }

        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        if (report.status == newStatus) {
            return report
        }

        val updated = reportRepository.updateStatus(reportId, report.status, newStatus, version, adminUserId, note)
        if (!updated) {
            throw ConflictException("El reporte ha sido modificado por otro usuario. Recargue los datos.")
        }

        val updatedReport = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado despues de actualizar")

        auditRepository.create(AuditEvent(
            id = UUID.randomUUID().toString(),
            actorAdminUserId = adminUserId,
            action = "status_changed",
            entityType = "report",
            entityId = reportId,
            metadata = """{"from":"${report.status.name}","to":"${newStatus.name}"}""",
            occurredAt = Instant.now(),
        ))

        eventBus.publish(WsEvent(
            type = "report.updated",
            occurredAt = Instant.now().toString(),
            payload = buildJsonObject {
                put("reportId", reportId)
                put("newStatus", newStatus.name)
            }
        ))

        return updatedReport
    }
}
