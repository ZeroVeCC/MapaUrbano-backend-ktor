package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.infrastructure.database.tables.UsersTable
import com.mapaurbano.users.domain.User
import com.mapaurbano.users.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override suspend fun findById(id: String): User? = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction null }
        UsersTable.selectAll().where { UsersTable.id eq uuid }.singleOrNull()?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = newSuspendedTransaction(Dispatchers.IO) {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    override suspend fun create(user: User): User = newSuspendedTransaction(Dispatchers.IO) {
        UsersTable.insert {
            it[id] = UUID.fromString(user.id)
            it[email] = user.email
            it[displayName] = user.displayName
            it[passwordHash] = user.passwordHash
            it[isActive] = user.isActive
            it[emailVerifiedAt] = user.emailVerifiedAt
            it[lastLoginAt] = user.lastLoginAt
            it[createdAt] = user.createdAt
            it[updatedAt] = user.updatedAt
        }
        user
    }

    override suspend fun deactivate(id: String): Unit = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction }
        UsersTable.update({ UsersTable.id eq uuid }) {
            it[isActive] = false
            it[deletedAt] = Instant.now()
        }
    }

    private fun ResultRow.toUser(): User = User(
        id = this[UsersTable.id].value.toString(),
        email = this[UsersTable.email],
        displayName = this[UsersTable.displayName],
        passwordHash = this[UsersTable.passwordHash],
        isActive = this[UsersTable.isActive],
        emailVerifiedAt = this[UsersTable.emailVerifiedAt],
        lastLoginAt = this[UsersTable.lastLoginAt],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )
}
