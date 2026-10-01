package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.auth.domain.AdminSession
import com.mapaurbano.auth.domain.AdminSessionRepository
import java.util.concurrent.ConcurrentHashMap

class AdminSessionRepositoryImpl : AdminSessionRepository {
    private val sessions = ConcurrentHashMap<String, AdminSession>()

    override suspend fun createSession(session: AdminSession): AdminSession {
        sessions[session.id] = session
        return session
    }

    override suspend fun findById(id: String): AdminSession? {
        val session = sessions[id] ?: return null
        if (session.expiresAt.isBefore(java.time.Instant.now())) {
            sessions.remove(id)
            return null
        }
        return session
    }

    override suspend fun deleteSession(id: String) {
        sessions.remove(id)
    }
}
