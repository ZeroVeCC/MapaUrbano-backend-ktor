package com.mapaurbano.auth.application

import com.mapaurbano.users.domain.SessionRepository
import java.time.Instant

class LogoutUserUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend fun execute(sessionId: String) {
        sessionRepository.revokeSession(sessionId, Instant.now())
    }
}
