package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.auth.domain.AdminUser
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.infrastructure.database.tables.AdminUsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class AdminUserRepositoryImpl : AdminUserRepository {
    override suspend fun findById(id: String): AdminUser? = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction null }
        AdminUsersTable.selectAll().where { AdminUsersTable.id eq uuid }.singleOrNull()?.toAdminUser()
    }

    override suspend fun findByUsername(username: String): AdminUser? = newSuspendedTransaction(Dispatchers.IO) {
        AdminUsersTable.selectAll().where { AdminUsersTable.username eq username }.singleOrNull()?.toAdminUser()
    }

    override suspend fun updateLastLogin(id: String, lastLoginAt: Instant): Unit = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction }
        AdminUsersTable.update({ AdminUsersTable.id eq uuid }) {
            it[AdminUsersTable.lastLoginAt] = lastLoginAt
        }
    }

    private fun ResultRow.toAdminUser(): AdminUser = AdminUser(
        id = this[AdminUsersTable.id].value.toString(),
        username = this[AdminUsersTable.username],
        passwordHash = this[AdminUsersTable.passwordHash],
        isActive = this[AdminUsersTable.isActive],
        lastLoginAt = this[AdminUsersTable.lastLoginAt],
        createdAt = this[AdminUsersTable.createdAt],
        updatedAt = this[AdminUsersTable.updatedAt]
    )
}
