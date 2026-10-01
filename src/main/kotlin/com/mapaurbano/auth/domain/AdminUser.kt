package com.mapaurbano.auth.domain

import java.time.Instant

data class AdminUser(
    val id: String,
    val username: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val lastLoginAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AdminSession(
    val id: String,
    val adminUserId: String,
    val expiresAt: Instant,
    val createdAt: Instant,
)
