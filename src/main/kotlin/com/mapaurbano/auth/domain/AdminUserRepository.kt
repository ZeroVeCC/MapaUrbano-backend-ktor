package com.mapaurbano.auth.domain

interface AdminUserRepository {
    suspend fun findById(id: String): AdminUser?
    suspend fun findByUsername(username: String): AdminUser?
    suspend fun updateLastLogin(id: String, lastLoginAt: java.time.Instant)
}

interface AdminSessionRepository {
    suspend fun createSession(session: AdminSession): AdminSession
    suspend fun findById(id: String): AdminSession?
    suspend fun deleteSession(id: String)
}
