package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.assignments.domain.ReportAssignment
import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ValidationException
import java.time.Instant
import java.util.UUID

class AssignReportUseCase(
    private val assignmentRepository: AssignmentRepository,
    private val reportRepository: ReportRepository,
    private val auditRepository: AuditRepository
) {
    suspend fun execute(
        reportId: String,
        teamId: String?,
        responsibleAdminUserId: String?,
        version: Long,
        adminUserId: String
    ): Boolean {
        if (teamId == null && responsibleAdminUserId == null) {
            throw ValidationException(message = "Se requiere al menos un equipo o un responsable")
        }

        val report = reportRepository.findById(reportId)
            ?: throw NotFoundException("Reporte no encontrado")

        // First unassign any current active assignment
        assignmentRepository.unassign(reportId)

        val newAssignment = ReportAssignment(
            id = UUID.randomUUID().toString(),
            reportId = report.id,
            teamId = teamId,
            responsibleAdminUserId = responsibleAdminUserId,
            assignedBy = adminUserId,
            assignedAt = Instant.now()
        )

        assignmentRepository.assign(newAssignment)

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

        // TODO: Update report version and check optimistic lock
        // TODO: Publisher event

        return true
    }
}
