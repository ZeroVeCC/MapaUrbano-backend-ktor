package com.mapaurbano.users.application

import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.User
import com.mapaurbano.users.domain.UserRepository
import com.mapaurbano.users.domain.UserSession
import com.mapaurbano.users.dto.RegisterRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class RegisterUserUseCaseTest {
    class FakeUserRepository : UserRepository {
        private val users = mutableListOf<User>()
        override suspend fun findById(id: String): User? = users.find { it.id == id }
        override suspend fun findByDni(dni: String): User? = users.find { it.dni == dni }
        override suspend fun create(user: User): User {
            users.add(user)
            return user
        }
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
    fun `registro exitoso`(): Unit = runBlocking {
        val useCase = RegisterUserUseCase(FakeUserRepository(), FakeSessionRepository())
        val request = RegisterRequest(dni = "30123456", displayName = "Juan", password = "password123")
        
        val response = useCase.execute(request)
        assertNotNull(response.id)
        assertNotNull(response.token)
    }

    @Test
    fun `dni duplicado falla`(): Unit = runBlocking {
        val repo = FakeUserRepository()
        repo.create(User("1", "30123456", "Test", "hash", true, createdAt = Instant.now(), updatedAt = Instant.now()))
        val useCase = RegisterUserUseCase(repo, FakeSessionRepository())
        
        val request = RegisterRequest(dni = "30123456", displayName = "Juan", password = "password123")
        assertThrows<ConflictException> {
            useCase.execute(request)
        }
    }
}
