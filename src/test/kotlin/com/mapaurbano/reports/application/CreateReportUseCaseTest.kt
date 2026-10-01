package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.Category
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.reports.domain.SubmissionMode
import com.mapaurbano.shared.domain.ValidationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class CreateReportUseCaseTest {

    class FakeReportRepository : ReportRepository {
        val reports = mutableListOf<Report>()
        
        override suspend fun findById(id: String): Report? = reports.find { it.id == id }
        override suspend fun findByUserId(userId: String, cursor: String?, limit: Int): List<Report> = reports.filter { it.userId == userId }
        override suspend fun findByTrackingCodeHash(trackingCodeHash: ByteArray): Report? = reports.find { it.trackingCodeHash?.contentEquals(trackingCodeHash) == true }
        override suspend fun findByBbox(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, status: ReportStatus?, categoryId: String?, limit: Int): List<Report> = reports
        override suspend fun create(report: Report): Report {
            reports.add(report)
            return report
        }
        override suspend fun updateStatus(id: String, status: ReportStatus, version: Long): Boolean = true
        override suspend fun updatePriority(id: String, priority: ReportPriority, dueAt: Instant?, version: Long): Boolean = true
        override suspend fun softDelete(id: String): Boolean = true
    }

    class FakeCategoryRepository : CategoryRepository {
        override suspend fun findById(id: String): Category? = Category("cat-1", "bach", "Baches", "#FFF", true, 1, Instant.now())
        override suspend fun findBySlug(slug: String): Category? = Category("cat-1", "bach", "Baches", "#FFF", true, 1, Instant.now())
        override suspend fun findAll(): List<Category> = listOf()
        override suspend fun create(category: Category): Category = category
        override suspend fun update(category: Category): Boolean = true
    }

    private val reportRepository = FakeReportRepository()
    private val categoryRepository = FakeCategoryRepository()
    private val useCase = CreateReportUseCase(reportRepository, categoryRepository)

    @Test
    fun `modo cuenta sin sesion deberia fallar`() = runBlocking {
        val ex = assertThrows<ValidationException> {
            useCase.execute(
                title = "Bache",
                description = "Grande",
                categorySlug = "bach",
                latitude = -34.0,
                longitude = -58.0,
                submissionMode = SubmissionMode.ACCOUNT,
                userId = null
            )
        }
        assertTrue(ex.details.any { it.field == "submissionMode" })
    }

    @Test
    fun `modo anonimo genera tracking code`() = runBlocking {
        val response = useCase.execute(
            title = "Bache",
            description = "Grande",
            categorySlug = "bach",
            latitude = -34.0,
            longitude = -58.0,
            submissionMode = SubmissionMode.ANONYMOUS,
            userId = null
        )
        assertNotNull(response.trackingCode)
        val saved = reportRepository.reports.first()
        assertNotNull(saved.trackingCodeHash)
    }
}
