package com.mapaurbano.users.application

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.FieldError
import com.mapaurbano.shared.domain.ValidationException
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.Dni
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
        val dni = Dni.normalize(request.dni)
        val displayName = request.displayName.trim()
        val password = request.password

        val errors = mutableListOf<FieldError>()
        if (dni == null) {
            errors.add(FieldError("dni", "El DNI debe tener 7 u 8 dígitos"))
        }
        if (displayName.isBlank() || displayName.length > 100) {
            errors.add(FieldError("displayName", "El nombre debe tener entre 1 y 100 caracteres"))
        }
        if (password.length < 8 || password.toByteArray(Charsets.UTF_8).size > 72) {
            errors.add(FieldError("password", "La contraseña debe tener al menos 8 caracteres y hasta 72 bytes UTF-8"))
        }
        
        if (errors.isNotEmpty()) {
            throw ValidationException(details = errors)
        }

        val normalizedDni = requireNotNull(dni)
        val existingUser = userRepository.findByDni(normalizedDni)
        if (existingUser != null) {
            throw ConflictException("El DNI ya está registrado", "DNI_ALREADY_REGISTERED")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        
        val now = Instant.now()
        val user = User(
            id = UUID.randomUUID().toString(),
            dni = normalizedDni,
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
