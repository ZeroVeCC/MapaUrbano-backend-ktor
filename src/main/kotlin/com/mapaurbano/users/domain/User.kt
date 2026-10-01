package com.mapaurbano.users.domain

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val emailVerifiedAt: Instant? = null,
    val lastLoginAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)
