package com.mapaurbano.audit.domain

interface AuditRepository {
    suspend fun create(event: AuditEvent): AuditEvent
    suspend fun findAll(cursor: String?, limit: Int): List<AuditEvent>
}
