package com.mapaurbano.users.domain

import java.time.Instant

interface SessionRepository {
    suspend fun createSession(session: UserSession): UserSession
    suspend fun findByTokenHash(tokenHash: ByteArray): UserSession?
    suspend fun revokeSession(sessionId: String, revokedAt: Instant = Instant.now())
    suspend fun revokeAllForUser(userId: String, revokedAt: Instant = Instant.now())
    suspend fun touchLastUsed(sessionId: String, lastUsedAt: Instant = Instant.now())
}
