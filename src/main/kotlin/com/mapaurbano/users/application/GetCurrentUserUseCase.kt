package com.mapaurbano.users.application

import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.users.domain.UserRepository
import com.mapaurbano.users.dto.UserProfileResponse

class GetCurrentUserUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(userId: String): UserProfileResponse {
        val user = userRepository.findById(userId)
            ?: throw NotFoundException("Usuario no encontrado")

        return UserProfileResponse(
            id = user.id,
            displayName = user.displayName,
            email = user.email,
            createdAt = user.createdAt.toString()
        )
    }
}
