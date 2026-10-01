package com.mapaurbano.categories.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: String,
    val slug: String,
    val name: String,
    val colorHex: String,
    val isActive: Boolean,
    val sortOrder: Int,
)

@Serializable
data class CreateCategoryRequest(
    val slug: String,
    val name: String,
    val colorHex: String,
    val sortOrder: Int? = null,
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val colorHex: String? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null,
)
