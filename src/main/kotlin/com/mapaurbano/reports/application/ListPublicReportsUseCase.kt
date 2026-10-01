package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.reports.dto.ReportPublicSummary
import com.mapaurbano.shared.api.CursorPage

class ListPublicReportsUseCase(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend fun execute(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        status: String?,
        categorySlug: String?,
        cursor: String?,
        limit: Int = 50
    ): CursorPage<ReportPublicSummary> {
        val parsedStatus = status?.let {
            try {
                ReportStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        
        val categoryId = categorySlug?.let { slug ->
            categoryRepository.findBySlug(slug)?.id
        }

        val reports = reportRepository.findByBbox(
            minLat = minLat,
            minLng = minLng,
            maxLat = maxLat,
            maxLng = maxLng,
            status = parsedStatus,
            categoryId = categoryId,
            limit = limit + 1
        )

        val hasNextPage = reports.size > limit
        val itemsToReturn = if (hasNextPage) reports.dropLast(1) else reports
        val nextCursor = if (hasNextPage) itemsToReturn.last().id else null

        // This could be optimized to not do N queries, but for now we fetch one by one or assume we have it
        val categories = categoryRepository.findAll().associateBy { it.id }

        val items = itemsToReturn.map { report ->
            val cat = categories[report.categoryId]
            ReportPublicSummary(
                id = report.id,
                title = report.title,
                categorySlug = cat?.slug ?: "unknown",
                latitude = report.latitude,
                longitude = report.longitude,
                status = report.status.name,
                hasImage = false, // TODO: Image logic
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
