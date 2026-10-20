package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportSubmissionRepository
import com.mapaurbano.shared.domain.PersistenceException
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ReportSubmissionRepositoryImpl : ReportSubmissionRepository {
    override suspend fun create(report: Report, image: ReportImage?): Report = try {
        newSuspendedTransaction(Dispatchers.IO) {
            insertReport(report)
            image?.let(::insertReportImage)
            report
        }
    } catch (cause: ExposedSQLException) {
        throw PersistenceException(
            message = "No pudimos guardar el reporte y su imagen. Intentá nuevamente.",
            errorCode = "REPORT_PERSISTENCE_ERROR",
            cause = cause,
        )
    }
}
