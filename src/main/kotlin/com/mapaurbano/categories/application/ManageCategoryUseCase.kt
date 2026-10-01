package com.mapaurbano.categories.application

import com.mapaurbano.categories.domain.Category
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.categories.dto.CategoryResponse
import com.mapaurbano.categories.dto.CreateCategoryRequest
import com.mapaurbano.categories.dto.UpdateCategoryRequest
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ValidationException
import java.time.Instant
import java.util.UUID

class ManageCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend fun create(request: CreateCategoryRequest): CategoryResponse {
        if (request.slug.isBlank() || request.name.isBlank()) {
            throw ValidationException(message = "Slug y nombre son obligatorios")
        }

        if (categoryRepository.findBySlug(request.slug) != null) {
            throw ConflictException("El slug ya está en uso")
        }

        val category = Category(
            id = UUID.randomUUID().toString(),
            slug = request.slug.trim(),
            name = request.name.trim(),
            colorHex = request.colorHex.trim(),
            sortOrder = request.sortOrder ?: 0,
            createdAt = Instant.now()
        )

        val created = categoryRepository.create(category)
        return mapToResponse(created)
    }

    suspend fun update(id: String, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            ?: throw NotFoundException("Categoría no encontrada")

        val updated = category.copy(
            name = request.name?.trim() ?: category.name,
            colorHex = request.colorHex?.trim() ?: category.colorHex,
            isActive = request.isActive ?: category.isActive,
            sortOrder = request.sortOrder ?: category.sortOrder
        )

        categoryRepository.update(updated)
        return mapToResponse(updated)
    }

    private fun mapToResponse(cat: Category): CategoryResponse {
        return CategoryResponse(
            id = cat.id,
            slug = cat.slug,
            name = cat.name,
            colorHex = cat.colorHex,
            isActive = cat.isActive,
            sortOrder = cat.sortOrder
        )
    }
}
