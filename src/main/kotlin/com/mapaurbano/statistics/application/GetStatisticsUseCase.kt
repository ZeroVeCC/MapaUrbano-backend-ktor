package com.mapaurbano.statistics.application

import com.mapaurbano.statistics.dto.StatisticsResponse
import com.mapaurbano.statistics.domain.StatisticsRepository

class GetStatisticsUseCase(private val repository: StatisticsRepository) {
    suspend fun execute(): StatisticsResponse = repository.get()
}
