package com.mapaurbano.categories.domain

import java.time.Instant

data class Category(
    val id: String,
    val slug: String,
    val name: String,
    val colorHex: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant,
)
