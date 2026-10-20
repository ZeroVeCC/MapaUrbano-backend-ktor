package com.mapaurbano.users.application

import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.dto.ReportPublicSummary
import com.mapaurbano.shared.api.CursorPage
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.shared.domain.ValidationException
import com.mapaurbano.shared.api.StableCursorCodec
import java.util.UUID

class ListUserReportsUseCase(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository,
    private val imageRepository: ImageRepository,
) {
    suspend fun execute(userId: String, cursor: String?, limit: Int = 20): CursorPage<ReportPublicSummary> {
        if (limit !in 1..100) throw ValidationException("El límite debe estar entre 1 y 100")
        cursor?.let {
            if (StableCursorCodec.decode(it) == null) throw ValidationException("Cursor inválido")
        }
        val reports = reportRepository.findByUserId(userId, cursor, limit + 1)
        
        val hasNextPage = reports.size > limit
        val itemsToReturn = if (hasNextPage) reports.dropLast(1) else reports
        
        val nextCursor = if (hasNextPage) itemsToReturn.last().let {
            StableCursorCodec.encode(it.createdAt, UUID.fromString(it.id))
        } else null
        val categories = categoryRepository.findAll().associateBy { it.id }
        
        val items = itemsToReturn.map { report ->
            ReportPublicSummary(
                id = report.id,
                title = report.title,
                categorySlug = categories[report.categoryId]?.slug ?: "unknown",
                latitude = report.latitude,
                longitude = report.longitude,
                status = report.status.name,
                hasImage = imageRepository.findByReportId(report.id) != null,
                createdAt = report.createdAt.toString(),
                updatedAt = report.updatedAt.toString()
            )
        }

        return CursorPage(
            items = items,
            nextCursor = nextCursor
        )
    }
}
