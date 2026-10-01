package com.mapaurbano.auth.application

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.auth.domain.AdminSession
import com.mapaurbano.auth.domain.AdminSessionRepository
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.auth.dto.AdminLoginRequest
import com.mapaurbano.auth.dto.AdminLoginResponse
import com.mapaurbano.shared.domain.AuthenticationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class LoginAdminUseCase(
    private val adminUserRepository: AdminUserRepository,
    private val adminSessionRepository: AdminSessionRepository
) {
    suspend fun execute(request: AdminLoginRequest): Pair<AdminLoginResponse, AdminSession> {
        val user = adminUserRepository.findByUsername(request.username)
            ?: throw AuthenticationException("Usuario o contraseña incorrectos")

        if (!user.isActive) {
            throw AuthenticationException("El usuario está desactivado")
        }

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw AuthenticationException("Usuario o contraseña incorrectos")
        }

        val now = Instant.now()
        val session = AdminSession(
            id = UUID.randomUUID().toString(),
            adminUserId = user.id,
            expiresAt = now.plus(1, ChronoUnit.DAYS),
            createdAt = now
        )

        val createdSession = adminSessionRepository.createSession(session)
        adminUserRepository.updateLastLogin(user.id, now)

        return AdminLoginResponse(
            adminUserId = user.id,
            username = user.username
        ) to createdSession
    }
}
