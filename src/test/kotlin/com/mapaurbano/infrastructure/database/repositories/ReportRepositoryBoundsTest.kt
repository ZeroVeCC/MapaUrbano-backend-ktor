package com.mapaurbano.infrastructure.database.repositories

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportRepositoryBoundsTest {
    @Test
    fun `default world bounds omit antipodal geography envelope`() {
        assertTrue(coversEntireWorld(-90.0, -180.0, 90.0, 180.0))
        assertTrue(coversEntireWorld(-100.0, -200.0, 100.0, 200.0))
        assertFalse(coversEntireWorld(-33.0, -69.0, -32.0, -68.0))
    }
}
