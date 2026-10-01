package com.mapaurbano.reports.dto

import kotlinx.serialization.Serializable

/* ── Respuestas administrativas ── */

@Serializable
data class ReportAdminSummary(
    val id: String,
    val title: String,
    val categorySlug: String,
    val categoryName: String,
    val status: String,
    val priority: String,
    val dueAt: String? = null,
    val teamName: String? = null,
    val responsibleAdminName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val hasImage: Boolean,
    val version: Long,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ReportAdminDetail(
    val id: String,
    val title: String,
    val description: String,
    val categorySlug: String,
    val categoryName: String,
    val status: String,
    val priority: String,
    val dueAt: String? = null,
    val submissionMode: String,
    val assignment: AssignmentInfo? = null,
    val latitude: Double,
    val longitude: Double,
    val hasImage: Boolean,
    val imageChecksum: String? = null,
    val version: Long,
    val createdAt: String,
    val updatedAt: String,
    val statusHistory: List<AdminStatusHistoryEntry> = emptyList(),
    val priorityHistory: List<PriorityHistoryEntry> = emptyList(),
)

@Serializable
data class AssignmentInfo(
    val teamId: String? = null,
    val teamName: String? = null,
    val responsibleAdminUserId: String? = null,
    val responsibleAdminName: String? = null,
    val assignedBy: String,
    val assignedAt: String,
)

@Serializable
data class AdminStatusHistoryEntry(
    val fromStatus: String? = null,
    val toStatus: String,
    val changedBy: String? = null,
    val note: String? = null,
    val changedAt: String,
)

@Serializable
data class PriorityHistoryEntry(
    val fromPriority: String? = null,
    val toPriority: String,
    val fromDueAt: String? = null,
    val toDueAt: String? = null,
    val changedBy: String,
    val changedAt: String,
)

/* ── Requests administrativos ── */

@Serializable
data class UpdateStatusRequest(
    val status: String,
    val version: Long,
    val note: String? = null,
)

@Serializable
data class UpdatePriorityRequest(
    val priority: String,
    val dueAt: String? = null,
    val version: Long,
)

@Serializable
data class AssignmentRequest(
    val teamId: String? = null,
    val responsibleAdminUserId: String? = null,
    val version: Long,
)
