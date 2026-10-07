package com.mapaurbano.application

import com.mapaurbano.shared.domain.PersistenceException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class PersistenceErrorTest {
    @Test
    fun `persistence failure returns controlled API error`() = testApplication {
        application {
            configurePlugins()
            routing {
                get("/persistence-failure") {
                    throw PersistenceException(
                        message = "No pudimos guardar el reporte. Intentá nuevamente.",
                        errorCode = "REPORT_PERSISTENCE_ERROR",
                    )
                }
            }
        }

        val response = client.get("/persistence-failure")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertContains(response.bodyAsText(), "\"code\":\"REPORT_PERSISTENCE_ERROR\"")
        assertContains(response.bodyAsText(), "No pudimos guardar el reporte. Intentá nuevamente.")
    }
}
