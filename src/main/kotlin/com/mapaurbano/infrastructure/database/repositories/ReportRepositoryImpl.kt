package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.infrastructure.database.tables.GeoPoint
import com.mapaurbano.infrastructure.database.tables.GeographyPointColumnType
import com.mapaurbano.infrastructure.database.tables.ReportsTable
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.doubleParam
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.intParam
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ReportRepositoryImpl : ReportRepository {
    override suspend fun findById(id: String): Report? = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction null }
        ReportsTable.selectAll().where { ReportsTable.id eq uuid }.singleOrNull()?.toReport()
    }

    override suspend fun findByUserId(userId: String, cursor: String?, limit: Int): List<Report> = newSuspendedTransaction(Dispatchers.IO) {
        val userUuid = try { UUID.fromString(userId) } catch (_: Exception) { return@newSuspendedTransaction emptyList() }

        val query = ReportsTable.selectAll().where {
            (ReportsTable.userId eq userUuid) and (ReportsTable.deletedAt.isNull())
        }

        if (cursor != null) {
            val cursorInstant = try { Instant.parse(cursor) } catch (_: Exception) { null }
            if (cursorInstant != null) {
                query.andWhere { ReportsTable.createdAt less cursorInstant }
            }
        }

        query.orderBy(ReportsTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toReport() }
    }

    override suspend fun findByTrackingCodeHash(trackingCodeHash: ByteArray): Report? = newSuspendedTransaction(Dispatchers.IO) {
        ReportsTable.selectAll().where { ReportsTable.trackingCodeHash eq trackingCodeHash }.singleOrNull()?.toReport()
    }

    override suspend fun findByBbox(
        minLat: Double, minLng: Double, maxLat: Double, maxLng: Double,
        status: ReportStatus?, categoryId: String?, limit: Int
    ): List<Report> = newSuspendedTransaction(Dispatchers.IO) {
        // Use ST_Intersects(location, ST_MakeEnvelope(minLng, minLat, maxLng, maxLat, 4326)::geography)
        val envelope = CustomFunction<GeoPoint>(
            "ST_MakeEnvelope", GeographyPointColumnType(),
            doubleParam(minLng), doubleParam(minLat), doubleParam(maxLng), doubleParam(maxLat), intParam(4326)
        )
        val bboxFilter = CustomFunction<Boolean>(
            "ST_Intersects", BooleanColumnType(),
            ReportsTable.location, envelope
        )

        val query = ReportsTable.selectAll().where {
            (bboxFilter eq true) and (ReportsTable.deletedAt.isNull())
        }

        if (status != null) {
            query.andWhere { ReportsTable.status eq status }
        }

        if (categoryId != null) {
            val catUuid = try { UUID.fromString(categoryId) } catch (_: Exception) { null }
            if (catUuid != null) {
                query.andWhere { ReportsTable.categoryId eq catUuid }
            }
        }

        query.orderBy(ReportsTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toReport() }
    }

    override suspend fun create(report: Report): Report = newSuspendedTransaction(Dispatchers.IO) {
        ReportsTable.insert {
            it[id] = UUID.fromString(report.id)
            it[categoryId] = UUID.fromString(report.categoryId)
            it[userId] = report.userId?.let { uid -> UUID.fromString(uid) }
            it[status] = report.status
            it[priority] = report.priority
            it[title] = report.title
            it[description] = report.description
            it[location] = GeoPoint(latitude = report.latitude, longitude = report.longitude)
            it[dueAt] = report.dueAt
            it[trackingCodeHash] = report.trackingCodeHash
            it[trackingCodeHint] = report.trackingCodeHint
            it[version] = report.version
            it[createdAt] = report.createdAt
            it[updatedAt] = report.updatedAt
        }
        report
    }

    override suspend fun updateStatus(id: String, status: ReportStatus, version: Long): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction false }
        val updatedRows = ReportsTable.update({
            (ReportsTable.id eq uuid) and (ReportsTable.version eq version)
        }) {
            it[ReportsTable.status] = status
            it[ReportsTable.version] = version + 1
            it[ReportsTable.updatedAt] = Instant.now()
        }
        updatedRows > 0
    }

    override suspend fun updatePriority(id: String, priority: ReportPriority, dueAt: Instant?, version: Long): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction false }
        val updatedRows = ReportsTable.update({
            (ReportsTable.id eq uuid) and (ReportsTable.version eq version)
        }) {
            it[ReportsTable.priority] = priority
            it[ReportsTable.dueAt] = dueAt
            it[ReportsTable.version] = version + 1
            it[ReportsTable.updatedAt] = Instant.now()
        }
        updatedRows > 0
    }

    override suspend fun softDelete(id: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction false }
        val updatedRows = ReportsTable.update({ ReportsTable.id eq uuid }) {
            it[ReportsTable.deletedAt] = Instant.now()
        }
        updatedRows > 0
    }

    private fun ResultRow.toReport(): Report {
        val pt = this[ReportsTable.location]
        return Report(
            id = this[ReportsTable.id].value.toString(),
            categoryId = this[ReportsTable.categoryId].value.toString(),
            userId = this[ReportsTable.userId]?.value?.toString(),
            status = this[ReportsTable.status],
            priority = this[ReportsTable.priority],
            title = this[ReportsTable.title],
            description = this[ReportsTable.description],
            latitude = pt.latitude,
            longitude = pt.longitude,
            dueAt = this[ReportsTable.dueAt],
            trackingCodeHash = this[ReportsTable.trackingCodeHash],
            trackingCodeHint = this[ReportsTable.trackingCodeHint],
            version = this[ReportsTable.version],
            createdAt = this[ReportsTable.createdAt],
            updatedAt = this[ReportsTable.updatedAt]
        )
    }
}
