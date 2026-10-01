package com.mapaurbano.application

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

fun Application.configureRateLimiting() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }
        
        register(RateLimitName("public")) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
        }
    }
}
