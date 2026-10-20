package com.mapaurbano.audit.application

import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.audit.dto.AuditEventResponse
import com.mapaurbano.shared.api.CursorPage
import com.mapaurbano.shared.domain.ValidationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import com.mapaurbano.shared.api.StableCursorCodec

class ListAuditEventsUseCase(private val repository: AuditRepository) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun execute(cursor: String?, limit: Int): CursorPage<AuditEventResponse> {
        if (limit !in 1..100) throw ValidationException("El límite debe estar entre 1 y 100.")
        cursor?.let {
            if (StableCursorCodec.decode(it) == null) {
                throw ValidationException("Cursor inválido.")
            }
        }
        val events = repository.findAll(cursor, limit + 1)
        val hasNext = events.size > limit
        val page = if (hasNext) events.dropLast(1) else events
        return CursorPage(
            items = page.map { event ->
                AuditEventResponse(
                    id = event.id,
                    actorAdminUserId = event.actorAdminUserId,
                    actorUserId = event.actorUserId,
                    action = event.action,
                    entityType = event.entityType,
                    entityId = event.entityId,
                    metadata = runCatching { json.parseToJsonElement(event.metadata) }
                        .getOrElse { JsonPrimitive(event.metadata) },
                    sourceIp = event.sourceIp,
                    occurredAt = event.occurredAt.toString(),
                )
            },
            nextCursor = if (hasNext) page.lastOrNull()?.let {
                StableCursorCodec.encode(it.occurredAt, UUID.fromString(it.id))
            } else null,
            totalCount = repository.count(),
        )
    }
}
