package com.mapaurbano.assignments.dto

import kotlinx.serialization.Serializable

/* ── Equipos ── */

@Serializable
data class TeamSummary(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean,
    val memberCount: Int,
    val createdAt: String,
)

@Serializable
data class TeamDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean,
    val members: List<TeamMemberInfo>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TeamMemberInfo(
    val adminUserId: String,
    val username: String,
    val joinedAt: String,
)

@Serializable
data class CreateTeamRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class UpdateTeamRequest(
    val name: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null,
)

/* ── Responsables posibles ── */

@Serializable
data class AssigneeResponse(
    val adminUserId: String,
    val username: String,
    val teamIds: List<String> = emptyList(),
)
