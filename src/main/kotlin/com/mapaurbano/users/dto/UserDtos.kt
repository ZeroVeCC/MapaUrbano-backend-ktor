package com.mapaurbano.users.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val displayName: String,
    val password: String,
)

@Serializable
data class RegisterResponse(
    val id: String,
    val token: String,
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val displayName: String,
    val email: String,
    val createdAt: String,
)
