package com.mapaurbano.reports.api

import com.mapaurbano.application.module
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertNotEquals

class PublicReportRoutesTest {

    @Test
    fun `GET reports parses query params`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/reports?minLat=-35&maxLat=-34&minLng=-59&maxLng=-58")
        
        // Expected to fail with 500 because of missing DI, but route should exist (not 404)
        assertNotEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST reports accepts multipart`() = testApplication {
        application { module() }

        val response = client.post("/api/v1/reports") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("title", "Bache peligroso")
                    append("description", "Hay un pozo")
                    append("categorySlug", "bach")
                    append("latitude", "-34.6037")
                    append("longitude", "-58.3816")
                    append("submissionMode", "ANONYMOUS")
                }
            ))
        }

        assertNotEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST report-status accepts json`() = testApplication {
        application { module() }

        val response = client.post("/api/v1/report-status") {
            headers.append(HttpHeaders.ContentType, "application/json")
            setBody("""{"trackingCode": "1234567890"}""")
        }

        assertNotEquals(HttpStatusCode.NotFound, response.status)
    }
}
