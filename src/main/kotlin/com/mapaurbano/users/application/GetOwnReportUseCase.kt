package com.mapaurbano.users.application

import com.mapaurbano.reports.domain.AdminReportQueryRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.dto.ReportPublicDetail
import com.mapaurbano.reports.dto.StatusHistoryEntry
import com.mapaurbano.shared.domain.NotFoundException

class GetOwnReportUseCase(
    private val reportRepository: ReportRepository,
    private val adminReportQueryRepository: AdminReportQueryRepository,
) {
    suspend fun execute(userId: String, reportId: String): ReportPublicDetail {
        val report = reportRepository.findById(reportId)
            ?.takeIf { it.userId == userId }
            ?: throw NotFoundException("Reporte no encontrado")
        val detail = adminReportQueryRepository.findDetail(report.id)
            ?: throw NotFoundException("Reporte no encontrado")
        return ReportPublicDetail(
            id = detail.id,
            title = detail.title,
            description = detail.description,
            categorySlug = detail.categorySlug,
            categoryName = detail.categoryName,
            latitude = detail.latitude,
            longitude = detail.longitude,
            status = detail.status,
            hasImage = detail.hasImage,
            createdAt = detail.createdAt,
            updatedAt = detail.updatedAt,
            statusHistory = detail.statusHistory.map {
                StatusHistoryEntry(it.fromStatus, it.toStatus, it.changedAt)
            },
        )
    }
}
