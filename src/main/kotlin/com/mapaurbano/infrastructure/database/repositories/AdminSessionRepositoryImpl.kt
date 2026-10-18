package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.auth.domain.AdminSession
import com.mapaurbano.auth.domain.AdminSessionRepository
import com.mapaurbano.infrastructure.database.tables.AdminSessionsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class AdminSessionRepositoryImpl : AdminSessionRepository {
    override suspend fun createSession(session: AdminSession): AdminSession = newSuspendedTransaction(Dispatchers.IO) {
        AdminSessionsTable.insert {
            it[id] = UUID.fromString(session.id)
            it[adminUserId] = UUID.fromString(session.adminUserId)
            it[csrfToken] = session.csrfToken
            it[expiresAt] = session.expiresAt
            it[revokedAt] = session.revokedAt
            it[createdAt] = session.createdAt
        }
        session
    }

    override suspend fun findById(id: String): AdminSession? = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return@newSuspendedTransaction null
        AdminSessionsTable.selectAll().where {
            (AdminSessionsTable.id eq uuid) and (AdminSessionsTable.revokedAt.isNull())
        }.singleOrNull()?.toAdminSession()?.takeIf { it.isValid() }
    }

    override suspend fun deleteSession(id: String): Unit = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return@newSuspendedTransaction
        AdminSessionsTable.update({ AdminSessionsTable.id eq uuid }) {
            it[revokedAt] = Instant.now()
        }
    }

    private fun ResultRow.toAdminSession() = AdminSession(
        id = this[AdminSessionsTable.id].value.toString(),
        adminUserId = this[AdminSessionsTable.adminUserId].value.toString(),
        csrfToken = this[AdminSessionsTable.csrfToken],
        expiresAt = this[AdminSessionsTable.expiresAt],
        revokedAt = this[AdminSessionsTable.revokedAt],
        createdAt = this[AdminSessionsTable.createdAt],
    )
}
