package com.mapaurbano.media.domain

interface ImageRepository {
    suspend fun findByReportId(reportId: String): ReportImage?
    suspend fun create(image: ReportImage): ReportImage
    suspend fun deleteByReportId(reportId: String): Boolean
}
