package com.mapaurbano.reports.domain

import com.mapaurbano.media.domain.ReportImage

interface ReportSubmissionRepository {
    /** Persists the report and its optional evidence in one database transaction. */
    suspend fun create(report: Report, image: ReportImage?): Report
}
