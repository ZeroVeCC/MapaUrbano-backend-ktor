package com.mapaurbano.auth.application

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.shared.domain.AuthenticationException
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.User
import com.mapaurbano.users.domain.UserRepository
import com.mapaurbano.users.domain.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.mapaurbano.auth.dto.LoginRequest
import java.time.Instant

class LoginUserUseCaseTest {
    class FakeUserRepository(private val user: User?) : UserRepository {
        override suspend fun findById(id: String): User? = if (user?.id == id) user else null
        override suspend fun findByEmail(email: String): User? = if (user?.email == email) user else null
        override suspend fun create(user: User): User = user
        override suspend fun deactivate(id: String) {}
    }

    class FakeSessionRepository : SessionRepository {
        override suspend fun createSession(session: UserSession): UserSession = session
        override suspend fun findByTokenHash(tokenHash: ByteArray): UserSession? = null
        override suspend fun revokeSession(sessionId: String, revokedAt: Instant) {}
        override suspend fun revokeAllForUser(userId: String, revokedAt: Instant) {}
        override suspend fun touchLastUsed(sessionId: String, lastUsedAt: Instant) {}
    }

    @Test
    fun `credenciales correctas devuelven token`() = runBlocking {
        val password = "mypassword"
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = User(
            id = "u-1", email = "test@test.com", displayName = "Test", passwordHash = hash,
            isActive = true, createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val useCase = LoginUserUseCase(FakeUserRepository(user), FakeSessionRepository())

        val response = useCase.execute(LoginRequest("test@test.com", password))
        assertNotNull(response.token)
    }

    @Test
    fun `cuenta desactivada falla`() = runBlocking {
        val password = "mypassword"
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = User(
            id = "u-1", email = "test@test.com", displayName = "Test", passwordHash = hash,
            isActive = false, createdAt = Instant.now(), updatedAt = Instant.now()
        )
        val useCase = LoginUserUseCase(FakeUserRepository(user), FakeSessionRepository())

        assertThrows<AuthenticationException> {
            useCase.execute(LoginRequest("test@test.com", password))
        }
    }
}
