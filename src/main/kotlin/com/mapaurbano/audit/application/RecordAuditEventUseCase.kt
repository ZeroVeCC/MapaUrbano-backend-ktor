package com.mapaurbano.audit.application

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import java.time.Instant
import java.util.UUID

class RecordAuditEventUseCase(
    private val auditRepository: AuditRepository
) {
    suspend fun execute(
        actorAdminUserId: String? = null,
        actorUserId: String? = null,
        action: String,
        entityType: String,
        entityId: String? = null,
        metadata: String = "{}",
        sourceIp: String? = null
    ) {
        auditRepository.create(
            AuditEvent(
                id = UUID.randomUUID().toString(),
                actorAdminUserId = actorAdminUserId,
                actorUserId = actorUserId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                metadata = metadata,
                sourceIp = sourceIp,
                occurredAt = Instant.now()
            )
        )
    }
}
