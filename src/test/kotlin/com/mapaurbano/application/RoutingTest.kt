package com.mapaurbano.application

import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RoutingTest {
    @Test
    fun `liveness reports that the process is running`() = testApplication {
        application { 
            install(org.koin.ktor.plugin.Koin) { modules(repositoryModule, applicationModule) }
            configurePlugins()
            configureRateLimiting()
            configureSecurity()
            configureRouting() 
        }

        val response = client.request("/health/live")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"status\":\"UP\"")
    }

    @Test
    fun `business endpoints are correctly mapped and do not return 404`() = testApplication {
        application { 
            install(org.koin.ktor.plugin.Koin) { modules(repositoryModule, applicationModule) }
            configurePlugins()
            configureRateLimiting()
            configureSecurity()
            configureRouting() 
        }

        val endpoints = listOf(
            Endpoint(HttpMethod.Get, "/api/v1/categories"),
            Endpoint(HttpMethod.Post, "/api/v1/reports"),
            Endpoint(HttpMethod.Get, "/api/v1/reports/123"),
            Endpoint(HttpMethod.Post, "/api/v1/report-status"),
            Endpoint(HttpMethod.Post, "/api/v1/users/register"),
            Endpoint(HttpMethod.Post, "/api/v1/users/login"),
            Endpoint(HttpMethod.Post, "/api/v1/users/logout"),
            Endpoint(HttpMethod.Get, "/api/v1/users/me"),
            Endpoint(HttpMethod.Delete, "/api/v1/users/me"),
            Endpoint(HttpMethod.Get, "/api/v1/users/me/reports"),
            Endpoint(HttpMethod.Get, "/api/v1/users/me/reports/123"),
            Endpoint(HttpMethod.Post, "/api/v1/admin/auth/login"),
            Endpoint(HttpMethod.Post, "/api/v1/admin/auth/logout"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/auth/me"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/reports"),
            Endpoint(HttpMethod.Patch, "/api/v1/admin/reports/123/status"),
            Endpoint(HttpMethod.Put, "/api/v1/admin/reports/123/assignment"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/teams"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/assignees"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/categories"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/statistics"),
            Endpoint(HttpMethod.Get, "/api/v1/admin/audit"),
        )

        endpoints.forEach { endpoint ->
            val response = client.request(endpoint.path) {
                method = endpoint.method
            }

            // Authentication wrapper returns 401, missing DI returns 500
            // Just verifying that the route is actually mapped (not 404)
            assertNotEquals(
                HttpStatusCode.NotFound,
                response.status,
                "${endpoint.method.value} ${endpoint.path} is not mapped",
            )
        }
    }

    @Test
    fun `legacy ambiguous admin login route is not registered`() = testApplication {
        application { 
            install(org.koin.ktor.plugin.Koin) { modules(repositoryModule, applicationModule) }
            configurePlugins()
            configureRateLimiting()
            configureSecurity()
            configureRouting() 
        }

        val response = client.request("/api/v1/auth/login") {
            method = HttpMethod.Post
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    private data class Endpoint(
        val method: HttpMethod,
        val path: String,
    )
}
