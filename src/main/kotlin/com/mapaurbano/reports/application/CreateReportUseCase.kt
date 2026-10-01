package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.reports.domain.SubmissionMode
import com.mapaurbano.reports.dto.CreateReportResponse
import com.mapaurbano.shared.domain.FieldError
import com.mapaurbano.shared.domain.ValidationException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

class CreateReportUseCase(
    private val reportRepository: ReportRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend fun execute(
        title: String,
        description: String,
        categorySlug: String,
        latitude: Double,
        longitude: Double,
        submissionMode: SubmissionMode,
        userId: String?,
        // TODO: image parameter when processing media
    ): CreateReportResponse {
        val errors = mutableListOf<FieldError>()
        
        if (title.isBlank()) errors.add(FieldError("title", "El título es obligatorio"))
        if (description.isBlank()) errors.add(FieldError("description", "La descripción es obligatoria"))
        if (latitude < -90 || latitude > 90) errors.add(FieldError("latitude", "Latitud inválida"))
        if (longitude < -180 || longitude > 180) errors.add(FieldError("longitude", "Longitud inválida"))
        
        val category = categoryRepository.findBySlug(categorySlug)
        if (category == null) {
            errors.add(FieldError("categorySlug", "Categoría no encontrada"))
        }

        if (submissionMode == SubmissionMode.ACCOUNT && userId == null) {
            errors.add(FieldError("submissionMode", "Se requiere sesión para el modo cuenta"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(details = errors)
        }

        var trackingCode: String? = null
        var trackingCodeHash: ByteArray? = null
        var trackingCodeHint: String? = null

        if (submissionMode == SubmissionMode.ANONYMOUS) {
            trackingCode = generateTrackingCode()
            val digest = MessageDigest.getInstance("SHA-256")
            trackingCodeHash = digest.digest(trackingCode.toByteArray(Charsets.UTF_8))
            trackingCodeHint = trackingCode.substring(0, 4) + "****"
        }

        val now = Instant.now()
        val report = Report(
            id = UUID.randomUUID().toString(),
            categoryId = category!!.id,
            userId = if (submissionMode == SubmissionMode.ACCOUNT) userId else null,
            status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM,
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            trackingCodeHash = trackingCodeHash,
            trackingCodeHint = trackingCodeHint,
            createdAt = now,
            updatedAt = now
        )

        val createdReport = reportRepository.create(report)
        // TODO: Guardar imagen si viene adjunta y publicar evento de creación

        return CreateReportResponse(
            id = createdReport.id,
            submissionMode = createdReport.submissionMode.name,
            status = createdReport.status.name,
            createdAt = createdReport.createdAt.toString(),
            trackingCode = trackingCode // Solo se devuelve al crearlo anónimamente
        )
    }

    private fun generateTrackingCode(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).uppercase().take(10)
    }
}
