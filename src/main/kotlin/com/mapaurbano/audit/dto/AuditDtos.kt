package com.mapaurbano.audit.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuditEventResponse(
    val id: String,
    val actorAdminUserId: String? = null,
    val actorUserId: String? = null,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val metadata: JsonElement? = null,
    val sourceIp: String? = null,
    val occurredAt: String,
)
