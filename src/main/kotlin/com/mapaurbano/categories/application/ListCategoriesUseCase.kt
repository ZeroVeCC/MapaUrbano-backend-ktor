package com.mapaurbano.categories.application

import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.categories.dto.CategoryResponse

class ListCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend fun execute(): List<CategoryResponse> {
        return categoryRepository.findAll().map { cat ->
            CategoryResponse(
                id = cat.id,
                slug = cat.slug,
                name = cat.name,
                colorHex = cat.colorHex,
                isActive = cat.isActive,
                sortOrder = cat.sortOrder
            )
        }
    }
}
