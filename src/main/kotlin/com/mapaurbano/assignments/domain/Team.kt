package com.mapaurbano.assignments.domain

import java.time.Instant

data class Team(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TeamMember(
    val teamId: String,
    val adminUserId: String,
    val joinedAt: Instant,
)

data class ReportAssignment(
    val id: String,
    val reportId: String,
    val teamId: String? = null,
    val responsibleAdminUserId: String? = null,
    val assignedBy: String,
    val assignedAt: Instant,
    val unassignedAt: Instant? = null,
) {
    init {
        require(teamId != null || responsibleAdminUserId != null) {
            "An assignment must have at least a team or a responsible admin"
        }
    }
}
