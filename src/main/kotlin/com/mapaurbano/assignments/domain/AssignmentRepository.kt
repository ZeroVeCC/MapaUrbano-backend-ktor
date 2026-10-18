package com.mapaurbano.assignments.domain

interface AssignmentRepository {
    suspend fun getActiveAssignmentForReport(reportId: String): ReportAssignment?
    suspend fun replace(assignment: ReportAssignment, expectedVersion: Long): Boolean
    suspend fun unassign(
        reportId: String,
        expectedVersion: Long,
        unassignedAt: java.time.Instant = java.time.Instant.now()
    ): Boolean
}
