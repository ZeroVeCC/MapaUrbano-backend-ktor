package com.mapaurbano.reports.application

import java.util.UUID

import com.mapaurbano.reports.domain.AdminReportFilter
import com.mapaurbano.reports.domain.AdminReportQueryRepository
import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportStatus
import com.mapaurbano.reports.dto.ReportAdminDetail
import com.mapaurbano.reports.dto.ReportAdminSummary
import com.mapaurbano.shared.api.CursorPage
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ValidationException
import com.mapaurbano.shared.api.StableCursorCodec

class AdminReportQueryUseCase(private val repository: AdminReportQueryRepository) {
    suspend fun list(
        status: String?, category: String?, priority: String?, teamId: String?,
        responsibleAdminUserId: String?, search: String?, cursor: String?, limit: Int,
    ): CursorPage<ReportAdminSummary> {
        if (limit !in 1..100) throw ValidationException("El límite debe estar entre 1 y 100.")
        cursor?.let {
            if (StableCursorCodec.decode(it) == null) throw ValidationException("Cursor inválido.")
        }
        validateOptionalUuid(teamId, "teamId")
        validateOptionalUuid(responsibleAdminUserId, "responsibleAdminUserId")
        val parsedStatus = status?.let {
            runCatching { ReportStatus.valueOf(it.uppercase()) }
                .getOrElse { throw ValidationException("Estado inválido.") }
        }
        val parsedPriority = priority?.let {
            runCatching { ReportPriority.valueOf(it.uppercase()) }
                .getOrElse { throw ValidationException("Prioridad inválida.") }
        }
        return repository.list(
            AdminReportFilter(parsedStatus, category, parsedPriority, teamId, responsibleAdminUserId, search, cursor, limit),
        )
    }

    suspend fun detail(id: String): ReportAdminDetail = repository.findDetail(id)
        ?: throw NotFoundException("Reporte no encontrado.")

    private fun validateOptionalUuid(value: String?, field: String) {
        value ?: return
        if (runCatching { UUID.fromString(value) }.isFailure) {
            throw ValidationException("$field debe ser un UUID válido.")
        }
    }
}
