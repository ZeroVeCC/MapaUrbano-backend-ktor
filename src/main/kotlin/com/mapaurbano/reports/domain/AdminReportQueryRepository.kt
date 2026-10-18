package com.mapaurbano.reports.domain

import com.mapaurbano.reports.dto.ReportAdminDetail
import com.mapaurbano.reports.dto.ReportAdminSummary
import com.mapaurbano.shared.api.CursorPage

data class AdminReportFilter(
    val status: ReportStatus? = null,
    val categorySlug: String? = null,
    val priority: ReportPriority? = null,
    val teamId: String? = null,
    val responsibleAdminUserId: String? = null,
    val search: String? = null,
    val cursor: String? = null,
    val limit: Int = 50,
)

interface AdminReportQueryRepository {
    suspend fun list(filter: AdminReportFilter): CursorPage<ReportAdminSummary>
    suspend fun findDetail(id: String): ReportAdminDetail?
}
