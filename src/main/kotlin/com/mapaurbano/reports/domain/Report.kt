package com.mapaurbano.reports.domain

import java.time.Instant

enum class ReportStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED
}

enum class ReportPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class SubmissionMode {
    ACCOUNT,
    ANONYMOUS
}

data class Report(
    val id: String,
    val categoryId: String,
    val userId: String? = null,
    val status: ReportStatus = ReportStatus.PENDING,
    val priority: ReportPriority = ReportPriority.MEDIUM,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val dueAt: Instant? = null,
    val trackingCodeHash: ByteArray? = null,
    val trackingCodeHint: String? = null,
    val version: Long = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
) {
    init {
        require(title.isNotBlank()) { "El título no puede estar vacío" }
        require(description.isNotBlank()) { "La descripción no puede estar vacía" }
        require(
            (userId != null && trackingCodeHash == null && trackingCodeHint == null) ||
            (userId == null && trackingCodeHash != null && trackingCodeHint != null)
        ) { "Un reporte debe ser registrado por un usuario o tener un código anónimo." }
    }

    val submissionMode: SubmissionMode
        get() = if (userId != null) SubmissionMode.ACCOUNT else SubmissionMode.ANONYMOUS

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Report

        if (id != other.id) return false
        if (categoryId != other.categoryId) return false
        if (userId != other.userId) return false
        if (status != other.status) return false
        if (priority != other.priority) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (dueAt != other.dueAt) return false
        if (trackingCodeHash != null) {
            if (other.trackingCodeHash == null) return false
            if (!trackingCodeHash.contentEquals(other.trackingCodeHash)) return false
        } else if (other.trackingCodeHash != null) return false
        if (trackingCodeHint != other.trackingCodeHint) return false
        if (version != other.version) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (deletedAt != other.deletedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + categoryId.hashCode()
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + status.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + (dueAt?.hashCode() ?: 0)
        result = 31 * result + (trackingCodeHash?.contentHashCode() ?: 0)
        result = 31 * result + (trackingCodeHint?.hashCode() ?: 0)
        result = 31 * result + version.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (deletedAt?.hashCode() ?: 0)
        return result
    }
}
