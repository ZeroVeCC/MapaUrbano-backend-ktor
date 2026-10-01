package com.mapaurbano.notifications.application

import com.mapaurbano.notifications.dto.WsEvent
import com.mapaurbano.reports.domain.Report
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

class EventPublisher(private val eventBus: EventBus) {

    suspend fun reportCreated(report: Report) {
        val payload = buildReportPayload(report)
        val event = WsEvent(
            type = "report.created",
            occurredAt = Instant.now().toString(),
            payload = payload
        )
        eventBus.publish(event)
    }

    suspend fun reportUpdated(report: Report) {
        val payload = buildReportPayload(report)
        val event = WsEvent(
            type = "report.updated",
            occurredAt = Instant.now().toString(),
            payload = payload
        )
        eventBus.publish(event)
    }

    suspend fun reportStatusChanged(report: Report, oldStatus: String) {
        val payload = buildReportPayload(report).toMutableMap()
        payload["oldStatus"] = JsonPrimitive(oldStatus)
        
        val event = WsEvent(
            type = "report.updated", // or status_changed depending on clients
            occurredAt = Instant.now().toString(),
            payload = JsonObject(payload)
        )
        eventBus.publish(event)
    }

    suspend fun reportDeleted(reportId: String) {
        val payload = JsonObject(mapOf("id" to JsonPrimitive(reportId)))
        val event = WsEvent(
            type = "report.deleted",
            occurredAt = Instant.now().toString(),
            payload = payload
        )
        eventBus.publish(event)
    }

    private fun buildReportPayload(report: Report): JsonObject {
        // Only public safe attributes
        return JsonObject(
            mapOf(
                "id" to JsonPrimitive(report.id),
                "title" to JsonPrimitive(report.title),
                "latitude" to JsonPrimitive(report.latitude),
                "longitude" to JsonPrimitive(report.longitude),
                "status" to JsonPrimitive(report.status.name),
                "categoryId" to JsonPrimitive(report.categoryId)
            )
        )
    }
}
