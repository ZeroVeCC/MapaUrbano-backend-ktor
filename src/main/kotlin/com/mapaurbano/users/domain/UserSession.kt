package com.mapaurbano.users.domain

import java.time.Instant

data class UserSession(
    val id: String,
    val userId: String,
    val tokenHash: ByteArray,
    val expiresAt: Instant,
    val lastUsedAt: Instant,
    val revokedAt: Instant? = null,
    val createdAt: Instant,
) {
    fun isValid(now: Instant): Boolean {
        return revokedAt == null && expiresAt.isAfter(now)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserSession

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (!tokenHash.contentEquals(other.tokenHash)) return false
        if (expiresAt != other.expiresAt) return false
        if (lastUsedAt != other.lastUsedAt) return false
        if (revokedAt != other.revokedAt) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + tokenHash.contentHashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + lastUsedAt.hashCode()
        result = 31 * result + (revokedAt?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
