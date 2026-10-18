package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.assignments.domain.ReportAssignment
import com.mapaurbano.infrastructure.database.tables.ReportAssignmentsTable
import com.mapaurbano.infrastructure.database.tables.ReportsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class AssignmentRepositoryImpl : AssignmentRepository {
    override suspend fun getActiveAssignmentForReport(reportId: String): ReportAssignment? = newSuspendedTransaction(Dispatchers.IO) {
        val reportUuid = try { UUID.fromString(reportId) } catch (_: Exception) { return@newSuspendedTransaction null }
        ReportAssignmentsTable.selectAll().where {
            (ReportAssignmentsTable.reportId eq reportUuid) and (ReportAssignmentsTable.unassignedAt.isNull())
        }.singleOrNull()?.toReportAssignment()
    }

    override suspend fun replace(assignment: ReportAssignment, expectedVersion: Long): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val reportUuid = try { UUID.fromString(assignment.reportId) } catch (_: Exception) { return@newSuspendedTransaction false }
        val now = assignment.assignedAt
        val reportUpdated = ReportsTable.update({
            (ReportsTable.id eq reportUuid) and
                (ReportsTable.version eq expectedVersion) and
                ReportsTable.deletedAt.isNull()
        }) {
            it[ReportsTable.version] = expectedVersion + 1
            it[ReportsTable.updatedAt] = now
        }
        if (reportUpdated == 0) return@newSuspendedTransaction false

        ReportAssignmentsTable.update({
            (ReportAssignmentsTable.reportId eq reportUuid) and ReportAssignmentsTable.unassignedAt.isNull()
        }) {
            it[ReportAssignmentsTable.unassignedAt] = now
        }
        ReportAssignmentsTable.insert {
            it[id] = UUID.fromString(assignment.id)
            it[reportId] = UUID.fromString(assignment.reportId)
            it[teamId] = assignment.teamId?.let { id -> UUID.fromString(id) }
            it[responsibleAdminUserId] = assignment.responsibleAdminUserId?.let { id -> UUID.fromString(id) }
            it[assignedBy] = UUID.fromString(assignment.assignedBy)
            it[assignedAt] = assignment.assignedAt
            it[unassignedAt] = assignment.unassignedAt
        }
        true
    }

    override suspend fun unassign(reportId: String, expectedVersion: Long, unassignedAt: Instant): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val reportUuid = try { UUID.fromString(reportId) } catch (_: Exception) { return@newSuspendedTransaction false }
        val hasActiveAssignment = ReportAssignmentsTable.selectAll().where {
            (ReportAssignmentsTable.reportId eq reportUuid) and ReportAssignmentsTable.unassignedAt.isNull()
        }.limit(1).any()
        if (!hasActiveAssignment) return@newSuspendedTransaction true

        val reportUpdated = ReportsTable.update({
            (ReportsTable.id eq reportUuid) and
                (ReportsTable.version eq expectedVersion) and
                ReportsTable.deletedAt.isNull()
        }) {
            it[ReportsTable.version] = expectedVersion + 1
            it[ReportsTable.updatedAt] = unassignedAt
        }
        if (reportUpdated == 0) return@newSuspendedTransaction false

        val updatedRows = ReportAssignmentsTable.update({
            (ReportAssignmentsTable.reportId eq reportUuid) and (ReportAssignmentsTable.unassignedAt.isNull())
        }) {
            it[ReportAssignmentsTable.unassignedAt] = unassignedAt
        }
        updatedRows > 0
    }

    private fun ResultRow.toReportAssignment(): ReportAssignment = ReportAssignment(
        id = this[ReportAssignmentsTable.id].value.toString(),
        reportId = this[ReportAssignmentsTable.reportId].value.toString(),
        teamId = this[ReportAssignmentsTable.teamId]?.value?.toString(),
        responsibleAdminUserId = this[ReportAssignmentsTable.responsibleAdminUserId]?.value?.toString(),
        assignedBy = this[ReportAssignmentsTable.assignedBy].value.toString(),
        assignedAt = this[ReportAssignmentsTable.assignedAt],
        unassignedAt = this[ReportAssignmentsTable.unassignedAt]
    )
}
