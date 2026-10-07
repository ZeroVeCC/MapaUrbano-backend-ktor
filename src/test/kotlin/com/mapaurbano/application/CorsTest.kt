package com.mapaurbano.application

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CorsTest {
    @Test
    fun `Angular development origin passes preflight but arbitrary origin does not`() = testApplication {
        application {
            configurePlugins()
            routing { get("/api/v1/categories") { call.respond(emptyList<String>()) } }
        }
        val allowed = client.options("/api/v1/categories") {
            header(HttpHeaders.Origin, "http://localhost:4200")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
            header(HttpHeaders.AccessControlRequestHeaders, "Authorization")
        }
        assertEquals(HttpStatusCode.OK, allowed.status)
        assertEquals("http://localhost:4200", allowed.headers[HttpHeaders.AccessControlAllowOrigin])
        val rejected = client.options("/api/v1/categories") {
            header(HttpHeaders.Origin, "https://untrusted.example")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }
        assertEquals(HttpStatusCode.Forbidden, rejected.status)
        assertNull(rejected.headers[HttpHeaders.AccessControlAllowOrigin])
    }
}
