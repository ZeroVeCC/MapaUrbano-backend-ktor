package com.mapaurbano.auth.application

import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.UserRepository
import java.security.MessageDigest
import java.time.Instant

data class UserPrincipal(val userId: String, val sessionId: String)

class AuthenticateUserUseCase(
    private val users: UserRepository,
    private val sessions: SessionRepository,
) {
    suspend fun execute(token: String): UserPrincipal? {
        if (!Regex("[A-Za-z0-9_-]{43}").matches(token)) return null
        val hash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        val session = sessions.findByTokenHash(hash) ?: return null
        val now = Instant.now()
        if (!session.isValid(now)) return null
        val user = users.findById(session.userId) ?: return null
        if (!user.isActive || user.deletedAt != null || user.dni == null) return null
        sessions.touchLastUsed(session.id, now)
        return UserPrincipal(user.id, session.id)
    }
}
