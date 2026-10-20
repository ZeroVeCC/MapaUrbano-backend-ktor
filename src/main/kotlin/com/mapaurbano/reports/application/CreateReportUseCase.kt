package com.mapaurbano.reports.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.domain.Report
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportSubmissionRepository
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.reports.domain.SubmissionMode
import com.mapaurbano.reports.dto.CreateReportResponse
import com.mapaurbano.media.application.ProcessImageUseCase
import com.mapaurbano.shared.domain.FieldError
import com.mapaurbano.shared.domain.ValidationException
import java.time.Instant
import java.util.UUID
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CreateReportUseCase(
    private val submissionRepository: ReportSubmissionRepository,
    private val categoryRepository: CategoryRepository,
    private val processImageUseCase: ProcessImageUseCase,
    private val eventBus: EventBus,
) {
    suspend fun execute(
        title: String,
        description: String,
        categorySlug: String,
        latitude: Double,
        longitude: Double,
        submissionMode: SubmissionMode,
        userId: String?,
        imageData: ByteArray? = null,
        imageContentType: String? = null,
        imageOriginalFilename: String? = null,
    ): CreateReportResponse {
        val errors = mutableListOf<FieldError>()
        
        if (title.isBlank() || title.length > 150) errors.add(FieldError("title", "El título debe tener entre 1 y 150 caracteres"))
        if (description.isBlank()) errors.add(FieldError("description", "La descripciÃ³n es obligatoria"))
        if (!latitude.isFinite() || latitude < -90 || latitude > 90) errors.add(FieldError("latitude", "Latitud inválida"))
        if (!longitude.isFinite() || longitude < -180 || longitude > 180) errors.add(FieldError("longitude", "Longitud inválida"))
        
        val category = categoryRepository.findBySlug(categorySlug)
        if (category == null || !category.isActive) {
            errors.add(FieldError("categorySlug", "Categoría no encontrada o inactiva"))
        }

        if (submissionMode != SubmissionMode.ACCOUNT) {
            errors.add(FieldError("submissionMode", "Los reportes requieren una cuenta autenticada"))
        }
        if (userId == null) {
            errors.add(FieldError("submissionMode", "Se requiere una sesión válida"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(details = errors)
        }

        val now = Instant.now()
        val report = Report(
            id = UUID.randomUUID().toString(),
            categoryId = category!!.id,
            userId = requireNotNull(userId),
            status = ReportStatus.PENDING,
            priority = ReportPriority.MEDIUM,
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            trackingCodeHash = null,
            trackingCodeHint = null,
            createdAt = now,
            updatedAt = now
        )

        if ((imageData == null) != (imageContentType == null)) {
            throw ValidationException("La imagen requiere contenido y Content-Type")
        }
        val image = if (imageData != null && imageContentType != null) {
            processImageUseCase.prepare(report.id, imageData, imageContentType, imageOriginalFilename)
        } else null
        val createdReport = submissionRepository.create(report, image)
        eventBus.publish(
            WsEvent(
                type = "report.created",
                occurredAt = now.toString(),
                payload = buildJsonObject { put("reportId", createdReport.id) },
            ),
        )

        return CreateReportResponse(
            id = createdReport.id,
            submissionMode = createdReport.submissionMode.name,
            status = createdReport.status.name,
            createdAt = createdReport.createdAt.toString(),
            trackingCode = null,
        )
    }
}


