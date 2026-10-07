package com.mapaurbano.reports.application



import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.shared.domain.ConflictException
import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

import com.mapaurbano.notifications.application.EventBus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals

class ChangeReportStatusUseCaseTest {
    
    class FakeReportRepository(private var report: Report) : ReportRepository {
        override suspend fun findById(id: String): Report? = report
        override suspend fun findByUserId(userId: String, cursor: String?, limit: Int): List<Report> = emptyList()
        override suspend fun findByTrackingCodeHash(trackingCodeHash: ByteArray): Report? = null
        override suspend fun findByBbox(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, status: ReportStatus?, categoryId: String?, limit: Int): List<Report> = emptyList()
        override suspend fun create(report: Report): Report = report
        override suspend fun updateStatus(id: String, oldStatus: ReportStatus, newStatus: ReportStatus, version: Long, adminUserId: String, note: String?): Boolean {
            if (version == report.version) {
                report = report.copy(status = newStatus, version = version + 1)
                return true
            }
            return false
        }
        override suspend fun updatePriority(id: String, priority: ReportPriority, dueAt: Instant?, version: Long): Boolean = true
        override suspend fun softDelete(id: String): Boolean = true
    }


    @Test
    fun `version desactualizada arroja conflicto`(): Unit = runBlocking {
        val report = Report(
            id = "r-1", categoryId = "c-1", userId = "u-1", status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM, title = "T", description = "D", latitude = 0.0, longitude = 0.0,
            trackingCodeHash = null, trackingCodeHint = null, version = 2L, createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val reportRepo = FakeReportRepository(report)

        val useCase = ChangeReportStatusUseCase(reportRepo, EventBus())

        assertThrows<ConflictException> {
            useCase.execute("r-1", "IN_PROGRESS", 1L, "Nota", "admin-1")
        }
    }

    @Test
    fun `transicion exitosa incrementa version y publica evento`(): Unit = runBlocking {
        val report = Report(
            id = "r-1", categoryId = "c-1", userId = "u-1", status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM, title = "T", description = "D", latitude = 0.0, longitude = 0.0,
            trackingCodeHash = null, trackingCodeHint = null, version = 1L, createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val reportRepo = FakeReportRepository(report)

        val bus = EventBus()
        val event = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(2000) { bus.subscribeAdmin().first() } }
        val useCase = ChangeReportStatusUseCase(reportRepo, bus)

        val result = useCase.execute("r-1", "IN_PROGRESS", 1L, "Nota", "admin-1")
        assertEquals(ReportStatus.IN_PROGRESS, result.status)
        assertEquals(2L, result.version)
        assertEquals("report.updated", event.await().type)
    }
}
