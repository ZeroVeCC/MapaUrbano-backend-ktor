package com.mapaurbano.shared.api

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class StableCursor(
    val timestamp: Instant,
    val id: UUID?,
)

/**
 * Opaque cursor ordered by timestamp and UUID. Timestamp-only cursors emitted by
 * older versions remain readable during a rolling deployment.
 */
object StableCursorCodec {
    fun encode(timestamp: Instant, id: UUID): String {
        val payload = "$timestamp|$id".toByteArray(StandardCharsets.UTF_8)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun decode(raw: String): StableCursor? {
        runCatching { Instant.parse(raw) }.getOrNull()?.let {
            return StableCursor(timestamp = it, id = null)
        }

        val decoded = runCatching {
            String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null
        val separator = decoded.lastIndexOf('|')
        if (separator <= 0 || separator == decoded.lastIndex) return null
        val timestamp = runCatching { Instant.parse(decoded.substring(0, separator)) }.getOrNull() ?: return null
        val id = runCatching { UUID.fromString(decoded.substring(separator + 1)) }.getOrNull() ?: return null
        return StableCursor(timestamp = timestamp, id = id)
    }
}
