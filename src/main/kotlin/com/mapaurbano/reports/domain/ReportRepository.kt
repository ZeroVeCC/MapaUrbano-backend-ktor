package com.mapaurbano.reports.domain

interface ReportRepository {
    suspend fun findById(id: String): Report?
    suspend fun findByUserId(userId: String, cursor: String?, limit: Int): List<Report>
    suspend fun findByTrackingCodeHash(trackingCodeHash: ByteArray): Report?
    suspend fun findByBbox(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, status: ReportStatus?, categoryId: String?, limit: Int): List<Report>
    suspend fun create(report: Report): Report
    suspend fun updateStatus(id: String, status: ReportStatus, version: Long): Boolean
    suspend fun updatePriority(id: String, priority: ReportPriority, dueAt: java.time.Instant?, version: Long): Boolean
    suspend fun softDelete(id: String): Boolean
}
