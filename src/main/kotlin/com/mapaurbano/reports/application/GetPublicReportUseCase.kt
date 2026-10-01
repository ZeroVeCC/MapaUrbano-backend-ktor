package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.dto.ReportPublicDetail
import com.mapaurbano.shared.domain.NotFoundException

class GetPublicReportUseCase(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend fun execute(id: String): ReportPublicDetail {
        val report = reportRepository.findById(id)
            ?: throw NotFoundException("Reporte no encontrado")

        val category = categoryRepository.findById(report.categoryId)
        val categorySlug = category?.slug ?: "unknown"
        val categoryName = category?.name ?: "Desconocida"

        return ReportPublicDetail(
            id = report.id,
            title = report.title,
            description = report.description,
            categorySlug = categorySlug,
            categoryName = categoryName,
            latitude = report.latitude,
            longitude = report.longitude,
            status = report.status.name,
            hasImage = false, // TODO
            createdAt = report.createdAt.toString(),
            updatedAt = report.updatedAt.toString(),
            statusHistory = emptyList() // TODO
        )
    }
}
