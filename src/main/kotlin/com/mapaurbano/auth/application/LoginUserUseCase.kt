package com.mapaurbano.auth.application

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.auth.dto.LoginRequest
import com.mapaurbano.auth.dto.LoginResponse
import com.mapaurbano.shared.domain.AuthenticationException
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.UserRepository
import com.mapaurbano.users.domain.UserSession
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun execute(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw AuthenticationException("Usuario o contraseña incorrectos")

        if (!user.isActive) {
            throw AuthenticationException("El usuario está desactivado")
        }

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw AuthenticationException("Usuario o contraseña incorrectos")
        }

        // Generar token opaco
        val secureRandom = SecureRandom()
        val tokenBytes = ByteArray(32)
        secureRandom.nextBytes(tokenBytes)
        val tokenString = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)

        // Calcular hash SHA-256 para guardar en BD
        val digest = MessageDigest.getInstance("SHA-256")
        val tokenHash = digest.digest(tokenString.toByteArray(Charsets.UTF_8))

        val now = Instant.now()
        val session = UserSession(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = now.plus(30, ChronoUnit.DAYS),
            lastUsedAt = now,
            createdAt = now
        )

        sessionRepository.createSession(session)

        return LoginResponse(
            token = tokenString,
            userId = user.id
        )
    }
}
