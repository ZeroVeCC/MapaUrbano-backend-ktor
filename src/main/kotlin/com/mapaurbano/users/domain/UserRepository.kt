package com.mapaurbano.users.domain

interface UserRepository {
    suspend fun findById(id: String): User?
    suspend fun findByDni(dni: String): User?
    suspend fun create(user: User): User
    suspend fun deactivate(id: String)
}
