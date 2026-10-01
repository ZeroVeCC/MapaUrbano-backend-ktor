package com.mapaurbano.statistics.api

import com.mapaurbano.statistics.application.GetStatisticsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.statisticsRoutes() {
    val getStatisticsUseCase by inject<GetStatisticsUseCase>()

    get("/statistics") {
        val response = getStatisticsUseCase.execute()
        call.respond(HttpStatusCode.OK, response)
    }
}
