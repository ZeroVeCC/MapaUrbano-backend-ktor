package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.Category
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.users.application.ListUserReportsUseCase
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ReportCoordinateResponseTest {
    private val trackingCode = "TRACK12345"
    private val trackingHash = MessageDigest.getInstance("SHA-256")
        .digest(trackingCode.toByteArray(Charsets.UTF_8))
    private val category = Category(
        id = "category-1",
        slug = "bache",
        name = "Bache",
        colorHex = "#FF0000",
        createdAt = Instant.parse("2026-10-07T12:00:00Z"),
    )
    private val anonymousReport = report(
        id = "report-anonymous",
        userId = null,
        latitude = -32.9113169,
        longitude = -68.8457851,
        trackingCodeHash = trackingHash,
        trackingCodeHint = "TRAC****",
    )
    private val accountReport = report(
        id = "report-account",
        userId = "user-1",
        latitude = -32.92843591716063,
        longitude = -68.8341889752306,
    )
    private val reports = FakeReportRepository(listOf(anonymousReport, accountReport))
    private val categories = FakeCategoryRepository(category)

    @Test
    fun `all report responses preserve latitude and longitude in JSON`(): Unit = runBlocking {
        val list = ListPublicReportsUseCase(reports, categories).execute(
            minLat = -90.0,
            minLng = -180.0,
            maxLat = 90.0,
            maxLng = 180.0,
            status = null,
            categorySlug = null,
            cursor = null,
        )
        val detail = GetPublicReportUseCase(reports, categories).execute(anonymousReport.id)
        val userReports = ListUserReportsUseCase(reports).execute(accountReport.userId!!, cursor = null)
        val tracking = GetReportByTrackingCodeUseCase(reports, categories).execute(trackingCode)

        assertEquals(anonymousReport.latitude, list.items.first().latitude)
        assertEquals(anonymousReport.longitude, detail.longitude)
        assertEquals(accountReport.latitude, userReports.items.single().latitude)
        assertEquals(anonymousReport.longitude, tracking.longitude)

        val json = listOf(
            Json.encodeToString(list),
            Json.encodeToString(detail),
            Json.encodeToString(userReports),
            Json.encodeToString(tracking),
        ).joinToString("\n")
        assertContains(json, "\"latitude\":-32.9113169")
        assertContains(json, "\"longitude\":-68.8457851")
        assertContains(json, "\"latitude\":-32.92843591716063")
        assertContains(json, "\"longitude\":-68.8341889752306")
    }

    private fun report(
        id: String,
        userId: String?,
        latitude: Double,
        longitude: Double,
        trackingCodeHash: ByteArray? = null,
        trackingCodeHint: String? = null,
    ) = Report(
        id = id,
        categoryId = category.id,
        userId = userId,
        status = ReportStatus.PENDING,
        priority = ReportPriority.MEDIUM,
        title = "Reporte de prueba",
        description = "Coordenadas persistidas",
        latitude = latitude,
        longitude = longitude,
        trackingCodeHash = trackingCodeHash,
        trackingCodeHint = trackingCodeHint,
        createdAt = Instant.parse("2026-10-07T12:00:00Z"),
        updatedAt = Instant.parse("2026-10-07T12:00:00Z"),
    )

    private class FakeReportRepository(private val reports: List<Report>) : ReportRepository {
        override suspend fun findById(id: String) = reports.find { it.id == id }
        override suspend fun findByUserId(userId: String, cursor: String?, limit: Int) =
            reports.filter { it.userId == userId }.take(limit)
        override suspend fun findByTrackingCodeHash(trackingCodeHash: ByteArray) =
            reports.find { it.trackingCodeHash?.contentEquals(trackingCodeHash) == true }
        override suspend fun findByBbox(
            minLat: Double,
            minLng: Double,
            maxLat: Double,
            maxLng: Double,
            status: ReportStatus?,
            categoryId: String?,
            limit: Int,
        ) = reports.take(limit)
        override suspend fun create(report: Report) = report
        override suspend fun updateStatus(
            id: String,
            oldStatus: ReportStatus,
            newStatus: ReportStatus,
            version: Long,
            adminUserId: String,
            note: String?,
        ) = true
        override suspend fun updatePriority(id: String, priority: ReportPriority, dueAt: Instant?, version: Long) = true
        override suspend fun softDelete(id: String) = true
    }

    private class FakeCategoryRepository(private val category: Category) : CategoryRepository {
        override suspend fun findAll() = listOf(category)
        override suspend fun findById(id: String) = category.takeIf { it.id == id }
        override suspend fun findBySlug(slug: String) = category.takeIf { it.slug == slug }
        override suspend fun create(category: Category) = category
        override suspend fun update(category: Category) = true
    }
}
