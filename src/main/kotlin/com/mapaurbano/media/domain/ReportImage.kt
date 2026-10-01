package com.mapaurbano.media.domain

import java.time.Instant

data class ReportImage(
    val id: String,
    val reportId: String,
    val data: ByteArray,
    val contentType: String,
    val sizeBytes: Long,
    val widthPx: Int,
    val heightPx: Int,
    val originalFilename: String?,
    val checksumSha256: String,
    val createdAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReportImage

        if (id != other.id) return false
        if (reportId != other.reportId) return false
        if (!data.contentEquals(other.data)) return false
        if (contentType != other.contentType) return false
        if (sizeBytes != other.sizeBytes) return false
        if (widthPx != other.widthPx) return false
        if (heightPx != other.heightPx) return false
        if (originalFilename != other.originalFilename) return false
        if (checksumSha256 != other.checksumSha256) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + reportId.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + (originalFilename?.hashCode() ?: 0)
        result = 31 * result + checksumSha256.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
