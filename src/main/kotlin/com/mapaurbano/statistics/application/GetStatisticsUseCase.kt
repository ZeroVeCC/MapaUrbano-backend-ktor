package com.mapaurbano.statistics.application

import com.mapaurbano.statistics.dto.StatisticsResponse

class GetStatisticsUseCase {
    suspend fun execute(): StatisticsResponse {
        // TODO: Implement with actual DB queries (probably requires a custom repository or direct Exposed query)
        return StatisticsResponse(
            totalReports = 0,
            byStatus = emptyMap(),
            byCategory = emptyList(),
            byPriority = emptyMap(),
            byTeam = emptyList()
        )
    }
}
