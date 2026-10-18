package com.mapaurbano.application

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mapaurbano.infrastructure.database.tables.AdminUsersTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

/** Crea el primer administrador únicamente cuando se proporcionan ambas variables de entorno. */
fun Application.bootstrapAdminIfConfigured() {
    val username = environment.config.propertyOrNull("security.adminBootstrap.username")
        ?.getString()?.trim()?.takeIf(String::isNotEmpty) ?: return
    val password = environment.config.propertyOrNull("security.adminBootstrap.password")
        ?.getString()?.takeIf(String::isNotEmpty) ?: return

    require(password.toByteArray(Charsets.UTF_8).size in 12..72) {
        "ADMIN_BOOTSTRAP_PASSWORD debe contener entre 12 y 72 bytes UTF-8."
    }

    transaction {
        val exists = AdminUsersTable.selectAll()
            .any { it[AdminUsersTable.username].equals(username, ignoreCase = true) }
        if (!exists) {
            val now = Instant.now()
            AdminUsersTable.insert {
                it[id] = UUID.randomUUID()
                it[AdminUsersTable.username] = username
                it[passwordHash] = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
            environment.log.info("Administrador inicial '$username' creado desde variables de entorno.")
        }
    }
}
