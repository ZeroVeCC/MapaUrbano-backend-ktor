package com.mapaurbano.categories.domain

interface CategoryRepository {
    suspend fun findAll(): List<Category>
    suspend fun findById(id: String): Category?
    suspend fun findBySlug(slug: String): Category?
    suspend fun create(category: Category): Category
    suspend fun update(category: Category): Boolean
}
