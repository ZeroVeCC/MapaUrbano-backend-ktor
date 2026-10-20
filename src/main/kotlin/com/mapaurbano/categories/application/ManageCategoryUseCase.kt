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
        val slug = request.slug.trim()
        val name = request.name.trim()
        val colorHex = request.colorHex.trim()
        validateSlug(slug)
        validateName(name)
        validateColor(colorHex)

        if (categoryRepository.findBySlug(slug) != null) {
            throw ConflictException("El slug ya está en uso")
        }

        val category = Category(
            id = UUID.randomUUID().toString(),
            slug = slug,
            name = name,
            colorHex = colorHex,
            sortOrder = request.sortOrder ?: 0,
            createdAt = Instant.now()
        )

        val created = categoryRepository.create(category)
        return mapToResponse(created)
    }

    suspend fun update(id: String, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            ?: throw NotFoundException("Categoría no encontrada")

        val name = request.name?.trim()?.also(::validateName)
        val colorHex = request.colorHex?.trim()?.also(::validateColor)

        val updated = category.copy(
            name = name ?: category.name,
            colorHex = colorHex ?: category.colorHex,
            isActive = request.isActive ?: category.isActive,
            sortOrder = request.sortOrder ?: category.sortOrder
        )

        categoryRepository.update(updated)
        return mapToResponse(updated)
    }

    private fun validateSlug(slug: String) {
        if (slug.length !in 1..50 || !SLUG_PATTERN.matches(slug)) {
            throw ValidationException("El slug debe contener entre 1 y 50 caracteres: letras minúsculas, números o guiones")
        }
    }

    private fun validateName(name: String) {
        if (name.length !in 1..80) {
            throw ValidationException("El nombre de la categoría debe contener entre 1 y 80 caracteres")
        }
    }

    private fun validateColor(colorHex: String) {
        if (!COLOR_PATTERN.matches(colorHex)) {
            throw ValidationException("colorHex debe usar el formato #RRGGBB")
        }
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

    private companion object {
        val SLUG_PATTERN = Regex("^[a-z0-9-]+$")
        val COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
