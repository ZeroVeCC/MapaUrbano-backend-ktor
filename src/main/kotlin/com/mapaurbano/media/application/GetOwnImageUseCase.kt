package com.mapaurbano.media.application

import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.shared.domain.NotFoundException

class GetOwnImageUseCase(
    private val reportRepository: ReportRepository,
    private val imageRepository: ImageRepository,
) {
    suspend fun execute(userId: String, reportId: String): ReportImage {
        reportRepository.findById(reportId)?.takeIf { it.userId == userId }
            ?: throw NotFoundException("Imagen no encontrada")
        return imageRepository.findByReportId(reportId)
            ?: throw NotFoundException("Imagen no encontrada")
    }
}
