package com.mapaurbano.auth.application

import com.mapaurbano.auth.domain.AdminSessionRepository

class LogoutAdminUseCase(
    private val adminSessionRepository: AdminSessionRepository
) {
    suspend fun execute(sessionId: String) {
        adminSessionRepository.deleteSession(sessionId)
    }
}
