package com.mapaurbano.application

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(repositoryModule, applicationModule)
    }
    configureDatabase()
    configurePlugins()
    configureSecurity()
    configureRateLimiting()
    configureRouting()
}
