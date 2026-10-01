package com.mapaurbano.assignments.domain

interface AssignmentRepository {
    suspend fun getActiveAssignmentForReport(reportId: String): ReportAssignment?
    suspend fun assign(assignment: ReportAssignment): ReportAssignment
    suspend fun unassign(reportId: String, unassignedAt: java.time.Instant = java.time.Instant.now()): Boolean
}
