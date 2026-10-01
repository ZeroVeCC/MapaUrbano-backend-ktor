package com.mapaurbano.application

import com.mapaurbano.database.DatabaseMigrations
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

fun Application.configureDatabase() {
    val log = LoggerFactory.getLogger("com.mapaurbano.application.Database")
    val dbUrl = environment.config.property("database.url").getString()
    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()

    log.info("Corriendo migraciones Flyway en $dbUrl...")
    val flyway = DatabaseMigrations.configure(dbUrl, dbUser, dbPassword)
    flyway.migrate()

    log.info("Configurando pool de conexiones HikariCP...")
    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    val dataSource = HikariDataSource(config)

    log.info("Conectando Exposed...")
    Database.connect(dataSource)
}
