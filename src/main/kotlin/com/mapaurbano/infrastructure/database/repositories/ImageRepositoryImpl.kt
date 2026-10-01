package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.infrastructure.database.tables.ReportImagesTable
import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.media.domain.ReportImage
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ImageRepositoryImpl : ImageRepository {
    override suspend fun findByReportId(reportId: String): ReportImage? = newSuspendedTransaction(Dispatchers.IO) {
        val reportUuid = try { UUID.fromString(reportId) } catch (_: Exception) { return@newSuspendedTransaction null }
        ReportImagesTable.selectAll().where { ReportImagesTable.reportId eq reportUuid }.singleOrNull()?.toReportImage()
    }

    override suspend fun create(image: ReportImage): ReportImage = newSuspendedTransaction(Dispatchers.IO) {
        ReportImagesTable.insert {
            it[id] = UUID.fromString(image.id)
            it[reportId] = UUID.fromString(image.reportId)
            it[data] = image.data
            it[contentType] = image.contentType
            it[sizeBytes] = image.sizeBytes
            it[widthPx] = image.widthPx
            it[heightPx] = image.heightPx
            it[originalFilename] = image.originalFilename
            it[checksumSha256] = image.checksumSha256
            it[createdAt] = image.createdAt
        }
        image
    }

    override suspend fun deleteByReportId(reportId: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val reportUuid = try { UUID.fromString(reportId) } catch (_: Exception) { return@newSuspendedTransaction false }
        val deletedRows = ReportImagesTable.deleteWhere { ReportImagesTable.reportId eq reportUuid }
        deletedRows > 0
    }

    private fun ResultRow.toReportImage(): ReportImage = ReportImage(
        id = this[ReportImagesTable.id].value.toString(),
        reportId = this[ReportImagesTable.reportId].value.toString(),
        data = this[ReportImagesTable.data],
        contentType = this[ReportImagesTable.contentType],
        sizeBytes = this[ReportImagesTable.sizeBytes],
        widthPx = this[ReportImagesTable.widthPx],
        heightPx = this[ReportImagesTable.heightPx],
        originalFilename = this[ReportImagesTable.originalFilename],
        checksumSha256 = this[ReportImagesTable.checksumSha256],
        createdAt = this[ReportImagesTable.createdAt]
    )
}
