package com.mapaurbano.statistics.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val totalReports: Long,
    val byStatus: Map<String, Long>,
    val byCategory: List<CategoryCount>,
    val byPriority: Map<String, Long>,
    val byTeam: List<TeamCount>,
)

@Serializable
data class CategoryCount(
    val categorySlug: String,
    val categoryName: String,
    val count: Long,
)

@Serializable
data class TeamCount(
    val teamId: String,
    val teamName: String,
    val count: Long,
)
