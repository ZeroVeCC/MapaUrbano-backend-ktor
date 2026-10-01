package com.mapaurbano.reports.dto

import kotlinx.serialization.Serializable

/* ── Respuestas públicas ── */

@Serializable
data class CreateReportResponse(
    val id: String,
    val submissionMode: String,
    val status: String,
    val createdAt: String,
    val trackingCode: String? = null,
)

@Serializable
data class ReportPublicSummary(
    val id: String,
    val title: String,
    val categorySlug: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val hasImage: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ReportPublicDetail(
    val id: String,
    val title: String,
    val description: String,
    val categorySlug: String,
    val categoryName: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val hasImage: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val statusHistory: List<StatusHistoryEntry> = emptyList(),
)

@Serializable
data class StatusHistoryEntry(
    val fromStatus: String? = null,
    val toStatus: String,
    val changedAt: String,
)

/* ── Seguimiento anónimo ── */

@Serializable
data class TrackingCodeRequest(
    val trackingCode: String,
)

@Serializable
data class TrackingCodeResponse(
    val id: String,
    val status: String,
    val categorySlug: String,
    val latitude: Double,
    val longitude: Double,
    val hasImage: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val statusHistory: List<StatusHistoryEntry> = emptyList(),
)
