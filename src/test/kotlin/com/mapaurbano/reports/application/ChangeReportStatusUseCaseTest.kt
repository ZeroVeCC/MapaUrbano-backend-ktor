package com.mapaurbano.reports.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.shared.domain.ConflictException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class ChangeReportStatusUseCaseTest {
    
    class FakeReportRepository(private var report: Report) : ReportRepository {
        override suspend fun findById(id: String): Report? = report
        override suspend fun findByUserId(userId: String, cursor: String?, limit: Int): List<Report> = emptyList()
        override suspend fun findByTrackingCodeHash(trackingCodeHash: ByteArray): Report? = null
        override suspend fun findByBbox(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, status: ReportStatus?, categoryId: String?, limit: Int): List<Report> = emptyList()
        override suspend fun create(r: Report): Report = r
        override suspend fun updateStatus(id: String, status: ReportStatus, version: Long): Boolean {
            if (version == report.version) {
                report = report.copy(status = status, version = version + 1)
                return true
            }
            return false
        }
        override suspend fun updatePriority(id: String, priority: ReportPriority, dueAt: Instant?, version: Long): Boolean = true
        override suspend fun softDelete(id: String): Boolean = true
    }

    class FakeAuditRepository : AuditRepository {
        val events = mutableListOf<AuditEvent>()
        override suspend fun create(event: AuditEvent): AuditEvent {
            events.add(event)
            return event
        }
        override suspend fun findAll(cursor: String?, limit: Int): List<AuditEvent> = events
    }

    @Test
    fun `version desactualizada arroja conflicto`() = runBlocking {
        val report = Report(
            id = "r-1", categoryId = "c-1", userId = null, status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM, title = "T", description = "D", latitude = 0.0, longitude = 0.0,
            trackingCodeHash = null, trackingCodeHint = null, version = 2L, createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val reportRepo = FakeReportRepository(report)
        val auditRepo = FakeAuditRepository()
        val useCase = ChangeReportStatusUseCase(reportRepo, auditRepo)

        assertThrows<ConflictException> {
            useCase.execute("r-1", "IN_PROGRESS", 1L, "Nota", "admin-1")
        }
    }

    @Test
    fun `transicion exitosa crea evento de auditoria`() = runBlocking {
        val report = Report(
            id = "r-1", categoryId = "c-1", userId = null, status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM, title = "T", description = "D", latitude = 0.0, longitude = 0.0,
            trackingCodeHash = null, trackingCodeHint = null, version = 1L, createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val reportRepo = FakeReportRepository(report)
        val auditRepo = FakeAuditRepository()
        val useCase = ChangeReportStatusUseCase(reportRepo, auditRepo)

        val result = useCase.execute("r-1", "IN_PROGRESS", 1L, "Nota", "admin-1")
        assertTrue(result)
        assertTrue(auditRepo.events.size == 1)
        assertTrue(auditRepo.events.first().action == "status_changed")
    }
}
