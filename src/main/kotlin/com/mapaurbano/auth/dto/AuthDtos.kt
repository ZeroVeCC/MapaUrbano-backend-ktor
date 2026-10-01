package com.mapaurbano.auth.dto

import kotlinx.serialization.Serializable

/* ── Vecinos (Android – Bearer token) ── */

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
)

/* ── Administradores (Panel – Cookie) ── */

@Serializable
data class AdminLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AdminLoginResponse(
    val adminUserId: String,
    val username: String,
)
