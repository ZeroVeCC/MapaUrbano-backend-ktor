package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.infrastructure.database.tables.CategoriesTable
import com.mapaurbano.infrastructure.database.tables.ReportAssignmentsTable
import com.mapaurbano.infrastructure.database.tables.ReportsTable
import com.mapaurbano.infrastructure.database.tables.TeamsTable
import com.mapaurbano.statistics.domain.StatisticsRepository
import com.mapaurbano.statistics.dto.CategoryCount
import com.mapaurbano.statistics.dto.StatisticsResponse
import com.mapaurbano.statistics.dto.TeamCount
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class StatisticsRepositoryImpl : StatisticsRepository {
    override suspend fun get(): StatisticsResponse = newSuspendedTransaction(Dispatchers.IO) {
        val reports = ReportsTable.selectAll().where { ReportsTable.deletedAt.isNull() }.toList()
        val categories = CategoriesTable.selectAll().associateBy { it[CategoriesTable.id].value }
        val reportIds = reports.map { it[ReportsTable.id].value }
        val assignments = if (reportIds.isEmpty()) emptyList() else ReportAssignmentsTable.selectAll().where {
            (ReportAssignmentsTable.reportId inList reportIds) and (ReportAssignmentsTable.unassignedAt.isNull())
        }.toList()
        val teams = TeamsTable.selectAll().associateBy { it[TeamsTable.id].value }

        StatisticsResponse(
            totalReports = reports.size.toLong(),
            byStatus = reports.groupingBy { it[ReportsTable.status].name }.eachCount()
                .mapValues { it.value.toLong() },
            byCategory = reports.groupBy { it[ReportsTable.categoryId].value }.map { (id, rows) ->
                val category = categories[id]
                CategoryCount(
                    categorySlug = category?.get(CategoriesTable.slug) ?: "unknown",
                    categoryName = category?.get(CategoriesTable.name) ?: "Desconocida",
                    count = rows.size.toLong(),
                )
            }.sortedByDescending { it.count },
            byPriority = reports.groupingBy { it[ReportsTable.priority].name }.eachCount()
                .mapValues { it.value.toLong() },
            byTeam = assignments.mapNotNull { row -> row[ReportAssignmentsTable.teamId]?.value }
                .groupingBy { it }.eachCount().map { (id, count) ->
                    TeamCount(
                        teamId = id.toString(),
                        teamName = teams[id]?.get(TeamsTable.name) ?: "Equipo eliminado",
                        count = count.toLong(),
                    )
                }.sortedByDescending { it.count },
        )
    }
}
