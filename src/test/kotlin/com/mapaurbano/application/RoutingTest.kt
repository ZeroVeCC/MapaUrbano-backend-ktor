package com.mapaurbano.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.reports.application.CreateReportUseCaseTest
import com.mapaurbano.reports.domain.ReportRepository
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RoutingTest {
    private fun io.ktor.server.application.Application.configureTestApplication() {
        install(Koin) {
            modules(repositoryModule, applicationModule, module {
                single<ReportRepository> { CreateReportUseCaseTest.FakeReportRepository() }
                single<CategoryRepository> { CreateReportUseCaseTest.FakeCategoryRepository() }
            })
        }
        configurePlugins()
        configureRateLimiting()
        configureSecurity()
        configureRouting()
    }

    @Test
    fun `liveness reports that the process is running`() = testApplication {
        application { configureTestApplication() }

        val response = client.request("/health/live")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"status\":\"UP\"")
    }

    @Test
    fun `public and user route groups return their expected status`() = testApplication {
        application { configureTestApplication() }

        val endpoints = listOf(
            Endpoint(HttpMethod.Get, "/api/v1/categories", HttpStatusCode.OK),
            Endpoint(HttpMethod.Get, "/api/v1/reports", HttpStatusCode.OK),
            Endpoint(HttpMethod.Get, "/api/v1/reports/123", HttpStatusCode.NotFound),
            Endpoint(HttpMethod.Post, "/api/v1/users/logout", HttpStatusCode.Unauthorized),
            Endpoint(HttpMethod.Get, "/api/v1/users/me", HttpStatusCode.Unauthorized),
            Endpoint(HttpMethod.Delete, "/api/v1/users/me", HttpStatusCode.Unauthorized),
            Endpoint(HttpMethod.Get, "/api/v1/users/me/reports", HttpStatusCode.Unauthorized),
            Endpoint(HttpMethod.Get, "/api/v1/users/me/reports/123", HttpStatusCode.Unauthorized),
        )

        endpoints.forEach { endpoint ->
            val response = client.request(endpoint.path) {
                method = endpoint.method
            }

            assertEquals(endpoint.expectedStatus, response.status, "${endpoint.method.value} ${endpoint.path}")
        }
    }

    @Test
    fun `legacy ambiguous admin login route is not registered`() = testApplication {
        application { configureTestApplication() }

        val response = client.request("/api/v1/auth/login") {
            method = HttpMethod.Post
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    private data class Endpoint(
        val method: HttpMethod,
        val path: String,
        val expectedStatus: HttpStatusCode,
    )
}
