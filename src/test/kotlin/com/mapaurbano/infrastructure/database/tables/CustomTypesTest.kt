package com.mapaurbano.infrastructure.database.tables

import com.mapaurbano.reports.domain.ReportStatus
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class CustomTypesTest {
    @Test
    fun `custom column types reuse PGobject produced by Exposed`() {
        val enumObject = pgObject("report_status", "pending")
        val enumCapture = capturingStatement()
        PgEnum("report_status", ReportStatus::class.java)
            .setParameter(enumCapture.statement, 1, enumObject)

        val geographyObject = pgObject("geography", "SRID=4326;POINT(-68.8458 -32.8895)")
        val geographyCapture = capturingStatement()
        GeographyPointColumnType()
            .setParameter(geographyCapture.statement, 1, geographyObject)

        assertSame(enumObject, enumCapture.value.get())
        assertSame(geographyObject, geographyCapture.value.get())
    }

    @Test
    fun `custom column types still convert domain values`() {
        val enumCapture = capturingStatement()
        PgEnum("report_status", ReportStatus::class.java)
            .setParameter(enumCapture.statement, 1, ReportStatus.PENDING)

        val geographyCapture = capturingStatement()
        GeographyPointColumnType()
            .setParameter(geographyCapture.statement, 1, GeoPoint(-32.8895, -68.8458))

        assertEquals("pending", assertIs<PGobject>(enumCapture.value.get()).value)
        assertEquals(
            "SRID=4326;POINT(-68.8458 -32.8895)",
            assertIs<PGobject>(geographyCapture.value.get()).value,
        )
    }

    private fun pgObject(type: String, value: String) = PGobject().apply {
        this.type = type
        this.value = value
    }

    private fun capturingStatement(): CapturingStatement {
        val captured = AtomicReference<Any>()
        val statement = Proxy.newProxyInstance(
            PreparedStatementApi::class.java.classLoader,
            arrayOf(PreparedStatementApi::class.java),
        ) { _, method, arguments ->
            if (method.name == "set") {
                captured.set(arguments!![1])
                null
            } else {
                error("Unexpected PreparedStatementApi call: ${method.name}")
            }
        } as PreparedStatementApi
        return CapturingStatement(statement, captured)
    }

    private data class CapturingStatement(
        val statement: PreparedStatementApi,
        val value: AtomicReference<Any>,
    )
}
