package com.mapaurbano.database

import org.flywaydb.core.Flyway

object DatabaseMigrations {
    fun configure(jdbcUrl: String, username: String, password: String): Flyway =
        Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("classpath:db/migration")
            .defaultSchema("public")
            .schemas("public")
            .cleanDisabled(true)
            .baselineOnMigrate(false)
            .validateMigrationNaming(true)
            .load()
}

fun main() {
    fun required(name: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("Falta la variable de entorno $name")

    val flyway = DatabaseMigrations.configure(
        required("DATABASE_JDBC_URL"),
        required("DATABASE_USER"),
        required("DATABASE_PASSWORD"),
    )
    val result = flyway.migrate()
    println("Migraciones aplicadas: ${result.migrationsExecuted}")
}
