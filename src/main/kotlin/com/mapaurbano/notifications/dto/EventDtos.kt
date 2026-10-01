package com.mapaurbano.notifications.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WsEvent(
    val type: String,
    val occurredAt: String,
    val payload: JsonElement? = null,
)

// Allowed event types according to docs:
// "connected", "report.created", "report.updated", "report.assignment_changed",
// "report.priority_changed", "report.deleted", "statistics.updated", "heartbeat", "error"
