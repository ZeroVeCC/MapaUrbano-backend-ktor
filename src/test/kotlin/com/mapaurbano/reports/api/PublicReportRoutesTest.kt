package com.mapaurbano.reports.api

import com.mapaurbano.application.applicationModule
import com.mapaurbano.application.configurePlugins
import com.mapaurbano.application.configureRateLimiting
import com.mapaurbano.application.configureRouting
import com.mapaurbano.application.configureSecurity
import com.mapaurbano.application.repositoryModule
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.application.CreateReportUseCaseTest
import com.mapaurbano.reports.domain.ReportRepository
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicReportRoutesTest {

    private fun Application.configureTestApplication() {
        install(Koin) {
            modules(repositoryModule, applicationModule, module {
                single<ReportRepository> { CreateReportUseCaseTest.FakeReportRepository() }
                single<CategoryRepository> { CreateReportUseCaseTest.FakeCategoryRepository() }
            })
        }
        configurePlugins()
        configureSecurity()
        configureRateLimiting()
        configureRouting()
    }

    @Test
    fun `GET reports parses query params`() = testApplication {
        application { configureTestApplication() }

        val response = client.get("/api/v1/reports?minLat=-35&maxLat=-34&minLng=-59&maxLng=-58")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST reports accepts multipart`() = testApplication {
        application { configureTestApplication() }

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

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST report-status accepts json`() = testApplication {
        application { configureTestApplication() }

        val response = client.post("/api/v1/report-status") {
            headers.append(HttpHeaders.ContentType, "application/json")
            setBody("""{"trackingCode": "1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
