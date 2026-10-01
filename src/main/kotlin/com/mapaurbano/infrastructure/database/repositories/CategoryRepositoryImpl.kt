package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.categories.domain.Category
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.infrastructure.database.tables.CategoriesTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class CategoryRepositoryImpl : CategoryRepository {
    override suspend fun findAll(): List<Category> = newSuspendedTransaction(Dispatchers.IO) {
        CategoriesTable.selectAll().map { it.toCategory() }
    }

    override suspend fun findById(id: String): Category? = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction null }
        CategoriesTable.selectAll().where { CategoriesTable.id eq uuid }.singleOrNull()?.toCategory()
    }

    override suspend fun findBySlug(slug: String): Category? = newSuspendedTransaction(Dispatchers.IO) {
        CategoriesTable.selectAll().where { CategoriesTable.slug eq slug }.singleOrNull()?.toCategory()
    }

    override suspend fun create(category: Category): Category = newSuspendedTransaction(Dispatchers.IO) {
        CategoriesTable.insert {
            it[id] = UUID.fromString(category.id)
            it[slug] = category.slug
            it[name] = category.name
            it[colorHex] = category.colorHex
            it[isActive] = category.isActive
            it[sortOrder] = category.sortOrder
            it[createdAt] = category.createdAt
        }
        category
    }

    override suspend fun update(category: Category): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(category.id) } catch (_: Exception) { return@newSuspendedTransaction false }
        val updatedRows = CategoriesTable.update({ CategoriesTable.id eq uuid }) {
            it[slug] = category.slug
            it[name] = category.name
            it[colorHex] = category.colorHex
            it[isActive] = category.isActive
            it[sortOrder] = category.sortOrder
        }
        updatedRows > 0
    }

    private fun ResultRow.toCategory(): Category = Category(
        id = this[CategoriesTable.id].value.toString(),
        slug = this[CategoriesTable.slug],
        name = this[CategoriesTable.name],
        colorHex = this[CategoriesTable.colorHex],
        isActive = this[CategoriesTable.isActive],
        sortOrder = this[CategoriesTable.sortOrder],
        createdAt = this[CategoriesTable.createdAt]
    )
}
