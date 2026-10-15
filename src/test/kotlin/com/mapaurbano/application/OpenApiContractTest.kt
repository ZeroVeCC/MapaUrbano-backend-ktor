package com.mapaurbano.application

import it.krzeminski.snakeyaml.engine.kmp.api.Load
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiContractTest {
    private val httpMethods = setOf("get", "post", "put", "patch", "delete")

    @Test
    fun `OpenAPI is valid YAML and covers every registered HTTP path`() {
        val specification = loadSpecification()
        val paths = specification.map("paths")

        val registeredPaths = setOf(
            "/health/live",
            "/health/ready",
            "/api/v1/categories",
            "/api/v1/reports",
            "/api/v1/reports/{id}",
            "/api/v1/reports/{id}/image",
            "/api/v1/report-status",
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/users/logout",
            "/api/v1/users/me",
            "/api/v1/users/me/reports",
            "/api/v1/users/me/reports/{id}",
            "/api/v1/ws/public",
            "/api/v1/ws/admin",
            "/api/v1/admin/auth/login",
            "/api/v1/admin/auth/logout",
            "/api/v1/admin/auth/me",
            "/api/v1/admin/reports",
            "/api/v1/admin/reports/{id}",
            "/api/v1/admin/reports/{id}/status",
            "/api/v1/admin/reports/{id}/priority",
            "/api/v1/admin/reports/{id}/assignment",
            "/api/v1/admin/teams",
            "/api/v1/admin/teams/{id}",
            "/api/v1/admin/teams/{teamId}/members/{adminUserId}",
            "/api/v1/admin/assignees",
            "/api/v1/admin/categories",
            "/api/v1/admin/categories/{id}",
            "/api/v1/admin/statistics",
            "/api/v1/admin/audit",
        )

        assertEquals("3.0.3", specification["openapi"])
        assertEquals(registeredPaths, paths.keys)
    }

    @Test
    fun `every operation has a unique id and an implementation status`() {
        val paths = loadSpecification().map("paths")
        val operations = paths.values
            .map { it.asMap() }
            .flatMap { path -> path.filterKeys(httpMethods::contains).values }
            .map { it.asMap() }

        val operationIds = operations.map { it["operationId"] as? String }
        assertTrue(operationIds.all { !it.isNullOrBlank() })
        assertEquals(operationIds.size, operationIds.toSet().size)

        val validStatuses = setOf("implemented", "partial", "stub", "blocked", "placeholder")
        assertTrue(operations.all { it["x-implementation-status"] in validStatuses })
    }

    @Test
    fun `all local references resolve and the citizen contract uses DNI and image`() {
        val specification = loadSpecification()
        val unresolved = mutableListOf<String>()

        walk(specification) { value ->
            if (value is Map<*, *>) {
                val reference = value["\$ref"] as? String
                if (reference?.startsWith("#/") == true && resolve(specification, reference) == null) {
                    unresolved += reference
                }
            }
        }

        assertTrue(unresolved.isEmpty(), "Referencias locales sin resolver: $unresolved")

        val schemas = specification.map("components").map("schemas")
        val registerProperties = schemas.map("RegisterRequest").map("properties")
        assertTrue("dni" in registerProperties)
        assertFalse("email" in registerProperties)

        val reportProperties = schemas.map("CreateReportMultipart").map("properties")
        assertTrue("image" in reportProperties)
        assertFalse("photo" in reportProperties)
        assertNotNull(specification.map("components").map("securitySchemes")["userBearer"])
    }

    private fun loadSpecification(): Map<String, Any?> {
        val stream = assertNotNull(javaClass.classLoader.getResourceAsStream("openapi/documentation.yaml"))
        return stream.use { Load().loadOne(it).asMap() }
    }

    private fun resolve(root: Map<String, Any?>, reference: String): Any? =
        reference.removePrefix("#/")
            .split('/')
            .fold(root as Any?) { current, segment -> current.asMap()[segment.replace("~1", "/").replace("~0", "~")] }

    private fun walk(value: Any?, visit: (Any?) -> Unit) {
        visit(value)
        when (value) {
            is Map<*, *> -> value.values.forEach { walk(it, visit) }
            is Iterable<*> -> value.forEach { walk(it, visit) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap(): Map<String, Any?> = this as Map<String, Any?>

    private fun Map<String, Any?>.map(key: String): Map<String, Any?> = this[key].asMap()
}
