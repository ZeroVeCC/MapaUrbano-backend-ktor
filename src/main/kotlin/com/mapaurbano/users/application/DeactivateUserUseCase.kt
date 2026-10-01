package com.mapaurbano.users.application

import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.UserRepository
import java.time.Instant

class DeactivateUserUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun execute(userId: String) {
        userRepository.deactivate(userId)
        sessionRepository.revokeAllForUser(userId, Instant.now())
    }
}
