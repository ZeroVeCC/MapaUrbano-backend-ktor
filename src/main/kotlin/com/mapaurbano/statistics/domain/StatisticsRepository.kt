package com.mapaurbano.statistics.domain

import com.mapaurbano.statistics.dto.StatisticsResponse

interface StatisticsRepository {
    suspend fun get(): StatisticsResponse
}
