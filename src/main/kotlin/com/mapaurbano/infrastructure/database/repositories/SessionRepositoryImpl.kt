package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.infrastructure.database.tables.UserSessionsTable
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.UserSession
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class SessionRepositoryImpl : SessionRepository {
    override suspend fun createSession(session: UserSession): UserSession = newSuspendedTransaction(Dispatchers.IO) {
        UserSessionsTable.insert {
            it[id] = UUID.fromString(session.id)
            it[userId] = UUID.fromString(session.userId)
            it[tokenHash] = session.tokenHash
            it[expiresAt] = session.expiresAt
            it[lastUsedAt] = session.lastUsedAt
            it[revokedAt] = session.revokedAt
            it[createdAt] = session.createdAt
        }
        session
    }

    override suspend fun findByTokenHash(tokenHash: ByteArray): UserSession? = newSuspendedTransaction(Dispatchers.IO) {
        UserSessionsTable.selectAll().where { UserSessionsTable.tokenHash eq tokenHash }.singleOrNull()?.toUserSession()
    }

    override suspend fun revokeSession(sessionId: String, revokedAt: Instant): Unit = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(sessionId) } catch (_: Exception) { return@newSuspendedTransaction }
        UserSessionsTable.update({ UserSessionsTable.id eq uuid }) {
            it[UserSessionsTable.revokedAt] = revokedAt
        }
    }

    override suspend fun revokeAllForUser(userId: String, revokedAt: Instant): Unit = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(userId) } catch (_: Exception) { return@newSuspendedTransaction }
        UserSessionsTable.update({ UserSessionsTable.userId eq uuid }) {
            it[UserSessionsTable.revokedAt] = revokedAt
        }
    }

    override suspend fun touchLastUsed(sessionId: String, lastUsedAt: Instant): Unit = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(sessionId) } catch (_: Exception) { return@newSuspendedTransaction }
        UserSessionsTable.update({ UserSessionsTable.id eq uuid }) {
            it[UserSessionsTable.lastUsedAt] = lastUsedAt
        }
    }

    private fun ResultRow.toUserSession(): UserSession = UserSession(
        id = this[UserSessionsTable.id].value.toString(),
        userId = this[UserSessionsTable.userId].value.toString(),
        tokenHash = this[UserSessionsTable.tokenHash],
        expiresAt = this[UserSessionsTable.expiresAt],
        lastUsedAt = this[UserSessionsTable.lastUsedAt],
        revokedAt = this[UserSessionsTable.revokedAt],
        createdAt = this[UserSessionsTable.createdAt]
    )
}
