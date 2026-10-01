package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException
import java.time.Instant
import java.util.UUID

class UnassignReportUseCase(
    private val assignmentRepository: AssignmentRepository,
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository
) {
    suspend fun execute(
        reportId: String,
        version: Long,
        adminUserId: String
    ): Boolean {
        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        val unassigned = assignmentRepository.unassign(reportId)
        if (!unassigned) {
            return true // Idempotent if nothing was assigned
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

        // TODO: Update report version and check optimistic lock
        // TODO: Publisher event

        return true
    }
}
