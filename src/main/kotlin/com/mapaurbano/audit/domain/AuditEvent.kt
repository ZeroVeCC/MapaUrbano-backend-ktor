package com.mapaurbano.audit.domain

import java.time.Instant

data class AuditEvent(
    val id: String,
    val actorAdminUserId: String? = null,
    val actorUserId: String? = null,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val metadata: String = "{}", // JSON
    val sourceIp: String? = null,
    val occurredAt: Instant,
) {
    init {
        require(actorAdminUserId == null || actorUserId == null) {
            "An audit event can only have one type of actor"
        }
    }
}
