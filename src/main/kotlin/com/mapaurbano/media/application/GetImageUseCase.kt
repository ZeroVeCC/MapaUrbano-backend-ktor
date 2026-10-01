package com.mapaurbano.media.application

import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.shared.domain.NotFoundException

class GetImageUseCase(
    private val imageRepository: ImageRepository
) {
    suspend fun execute(reportId: String): ReportImage {
        return imageRepository.findByReportId(reportId)
            ?: throw NotFoundException("Imagen no encontrada")
    }
}
