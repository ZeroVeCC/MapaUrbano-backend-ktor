package com.mapaurbano.application

import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit

import com.mapaurbano.assignments.api.assignmentRoutes
import com.mapaurbano.assignments.api.teamRoutes
import com.mapaurbano.audit.api.auditRoutes
import com.mapaurbano.auth.api.adminAuthRoutes
import com.mapaurbano.categories.api.adminCategoryRoutes
import com.mapaurbano.categories.api.publicCategoryRoutes
import com.mapaurbano.health.api.healthRoutes
import com.mapaurbano.media.api.adminImageRoutes
import com.mapaurbano.media.api.publicImageRoutes
import com.mapaurbano.notifications.api.webSocketRoutes
import com.mapaurbano.reports.api.adminReportRoutes
import com.mapaurbano.reports.api.publicReportRoutes
import com.mapaurbano.statistics.api.statisticsRoutes
import com.mapaurbano.users.api.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        healthRoutes()

        route("/api/v1") {
            publicCategoryRoutes()
            
            rateLimit(RateLimitName("public")) {
                publicReportRoutes()
                publicImageRoutes()
            }
            
            userRoutes()
            webSocketRoutes()

            authenticate("admin-session") {
                route("/admin") {
                    adminAuthRoutes()
                    adminReportRoutes()
                    adminImageRoutes()
                    assignmentRoutes()
                    teamRoutes()
                    adminCategoryRoutes()
                    statisticsRoutes()
                    auditRoutes()
                }
            }
        }
    }
}
