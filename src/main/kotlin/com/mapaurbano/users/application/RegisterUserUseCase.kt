package com.mapaurbano.users.application

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.FieldError
import com.mapaurbano.shared.domain.ValidationException
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.User
import com.mapaurbano.users.domain.UserRepository
import com.mapaurbano.users.domain.UserSession
import com.mapaurbano.users.dto.RegisterRequest
import com.mapaurbano.users.dto.RegisterResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun execute(request: RegisterRequest): RegisterResponse {
        val email = request.email.trim().lowercase()
        val displayName = request.displayName.trim()
        val password = request.password

        val errors = mutableListOf<FieldError>()
        if (email.isBlank() || !email.contains("@")) {
            errors.add(FieldError("email", "Formato de correo inválido"))
        }
        if (displayName.isBlank()) {
            errors.add(FieldError("displayName", "El nombre visible es obligatorio"))
        }
        if (password.length < 8) {
            errors.add(FieldError("password", "La contraseña debe tener al menos 8 caracteres"))
        }
        
        if (errors.isNotEmpty()) {
            throw ValidationException(details = errors)
        }

        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            throw ConflictException("El correo ya está registrado", "EMAIL_ALREADY_REGISTERED")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        
        val now = Instant.now()
        val user = User(
            id = UUID.randomUUID().toString(),
            email = email,
            displayName = displayName,
            passwordHash = passwordHash,
            createdAt = now,
            updatedAt = now
        )

        val createdUser = userRepository.create(user)

        // Generar token opaco de sesión
        val secureRandom = SecureRandom()
        val tokenBytes = ByteArray(32)
        secureRandom.nextBytes(tokenBytes)
        val tokenString = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)

        val digest = MessageDigest.getInstance("SHA-256")
        val tokenHash = digest.digest(tokenString.toByteArray(Charsets.UTF_8))

        val session = UserSession(
            id = UUID.randomUUID().toString(),
            userId = createdUser.id,
            tokenHash = tokenHash,
            expiresAt = now.plus(30, ChronoUnit.DAYS),
            lastUsedAt = now,
            createdAt = now
        )

        sessionRepository.createSession(session)

        return RegisterResponse(
            id = createdUser.id,
            token = tokenString
        )
    }
}
