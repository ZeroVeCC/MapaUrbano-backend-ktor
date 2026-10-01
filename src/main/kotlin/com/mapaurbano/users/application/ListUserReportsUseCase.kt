package com.mapaurbano.users.application

import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.dto.ReportPublicSummary
import com.mapaurbano.shared.api.CursorPage

class ListUserReportsUseCase(
    private val reportRepository: ReportRepository
) {
    suspend fun execute(userId: String, cursor: String?, limit: Int = 20): CursorPage<ReportPublicSummary> {
        val reports = reportRepository.findByUserId(userId, cursor, limit + 1)
        
        val hasNextPage = reports.size > limit
        val itemsToReturn = if (hasNextPage) reports.dropLast(1) else reports
        
        val nextCursor = if (hasNextPage) itemsToReturn.last().id else null
        
        val items = itemsToReturn.map { report ->
            ReportPublicSummary(
                id = report.id,
                title = report.title,
                categorySlug = report.categoryId, // asumiendo que categoryId guarda el slug para simplificar o requerirá join
                latitude = report.latitude,
                longitude = report.longitude,
                status = report.status.name,
                hasImage = false, // TODO: Check if image exists
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
