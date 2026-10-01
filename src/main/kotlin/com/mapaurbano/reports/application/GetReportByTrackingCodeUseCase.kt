package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.dto.TrackingCodeResponse
import com.mapaurbano.shared.domain.AuthenticationException
import java.security.MessageDigest

class GetReportByTrackingCodeUseCase(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun execute(trackingCode: String): TrackingCodeResponse {
        val digest = MessageDigest.getInstance("SHA-256")
        val trackingCodeHash = digest.digest(trackingCode.toByteArray(Charsets.UTF_8))

        val report = reportRepository.findByTrackingCodeHash(trackingCodeHash)
            ?: throw AuthenticationException("Código de seguimiento inválido")

        val category = categoryRepository.findById(report.categoryId)
        val categorySlug = category?.slug ?: "unknown"

        return TrackingCodeResponse(
            id = report.id,
            status = report.status.name,
            categorySlug = categorySlug,
            latitude = report.latitude,
            longitude = report.longitude,
            hasImage = false, // TODO: Check if image exists
            createdAt = report.createdAt.toString(),
            updatedAt = report.updatedAt.toString(),
            statusHistory = emptyList() // TODO: Fetch from audit log
        )
    }
}
