package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.infrastructure.database.tables.AuditEventsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import com.mapaurbano.shared.api.StableCursorCodec

class AuditRepositoryImpl : AuditRepository {
    override suspend fun create(event: AuditEvent): AuditEvent = newSuspendedTransaction(Dispatchers.IO) {
        AuditEventsTable.insert {
            it[id] = UUID.fromString(event.id)
            it[actorAdminUserId] = event.actorAdminUserId?.let { id -> UUID.fromString(id) }
            it[actorUserId] = event.actorUserId?.let { id -> UUID.fromString(id) }
            it[action] = event.action
            it[entityType] = event.entityType
            it[entityId] = event.entityId?.let { id -> UUID.fromString(id) }
            it[metadata] = event.metadata // Text mapped from JSON
            it[sourceIp] = event.sourceIp
            it[occurredAt] = event.occurredAt
        }
        event
    }

    override suspend fun findAll(cursor: String?, limit: Int): List<AuditEvent> = newSuspendedTransaction(Dispatchers.IO) {
        val query = AuditEventsTable.selectAll()
        cursor?.let { raw ->
            StableCursorCodec.decode(raw)?.let { cursor ->
                query.andWhere {
                    val older = AuditEventsTable.occurredAt less cursor.timestamp
                    if (cursor.id == null) older
                    else older or ((AuditEventsTable.occurredAt eq cursor.timestamp) and (AuditEventsTable.id less cursor.id))
                }
            }
        }
        query
            .orderBy(AuditEventsTable.occurredAt to SortOrder.DESC, AuditEventsTable.id to SortOrder.DESC)
            .limit(limit)
            .map { it.toAuditEvent() }
    }

    override suspend fun count(): Long = newSuspendedTransaction(Dispatchers.IO) {
        AuditEventsTable.selectAll().count()
    }

    private fun ResultRow.toAuditEvent(): AuditEvent = AuditEvent(
        id = this[AuditEventsTable.id].value.toString(),
        actorAdminUserId = this[AuditEventsTable.actorAdminUserId]?.value?.toString(),
        actorUserId = this[AuditEventsTable.actorUserId]?.value?.toString(),
        action = this[AuditEventsTable.action],
        entityType = this[AuditEventsTable.entityType],
        entityId = this[AuditEventsTable.entityId]?.toString(),
        metadata = this[AuditEventsTable.metadata],
        sourceIp = this[AuditEventsTable.sourceIp],
        occurredAt = this[AuditEventsTable.occurredAt]
    )
}
