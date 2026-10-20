package com.mapaurbano.shared.api

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StableCursorCodecTest {
    @Test
    fun `round trip preserves timestamp and UUID`() {
        val timestamp = Instant.parse("2026-10-19T12:34:56.123456Z")
        val id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")

        assertEquals(StableCursor(timestamp, id), StableCursorCodec.decode(StableCursorCodec.encode(timestamp, id)))
    }

    @Test
    fun `accepts legacy timestamp cursors and rejects malformed values`() {
        val timestamp = Instant.parse("2026-10-19T12:34:56Z")

        assertEquals(StableCursor(timestamp, null), StableCursorCodec.decode(timestamp.toString()))
        assertNull(StableCursorCodec.decode("not-a-cursor"))
    }
}
