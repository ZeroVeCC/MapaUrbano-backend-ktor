package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.infrastructure.database.tables.*
import com.mapaurbano.reports.domain.AdminReportFilter
import com.mapaurbano.reports.domain.AdminReportQueryRepository
import com.mapaurbano.reports.dto.*
import com.mapaurbano.shared.api.CursorPage
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID
import com.mapaurbano.shared.api.StableCursorCodec

class AdminReportQueryRepositoryImpl : AdminReportQueryRepository {
    override suspend fun list(filter: AdminReportFilter): CursorPage<ReportAdminSummary> =
        newSuspendedTransaction(Dispatchers.IO) {
            var condition: Op<Boolean> = ReportsTable.deletedAt.isNull()
            filter.status?.let { condition = condition and (ReportsTable.status eq it) }
            filter.priority?.let { condition = condition and (ReportsTable.priority eq it) }
            filter.categorySlug?.let { slug ->
                val categoryId = CategoriesTable.selectAll().where { CategoriesTable.slug eq slug }
                    .singleOrNull()?.get(CategoriesTable.id)?.value
                    ?: return@newSuspendedTransaction CursorPage(emptyList(), totalCount = 0)
                condition = condition and (ReportsTable.categoryId eq categoryId)
            }

            val assignmentReportIds = assignmentReportIds(filter.teamId, filter.responsibleAdminUserId)
            if (filter.teamId != null || filter.responsibleAdminUserId != null) {
                if (assignmentReportIds.isEmpty()) {
                    return@newSuspendedTransaction CursorPage(emptyList(), totalCount = 0)
                }
                condition = condition and (ReportsTable.id inList assignmentReportIds)
            }
            filter.search?.trim()?.takeIf(String::isNotEmpty)?.let { value ->
                val pattern = "%$value%"
                condition = condition and ((ReportsTable.title like pattern) or (ReportsTable.description like pattern))
            }

            val totalCount = ReportsTable.selectAll().where { condition }.count()
            var pageCondition = condition
            filter.cursor?.let { raw ->
                StableCursorCodec.decode(raw)?.let { cursor ->
                    val older = ReportsTable.createdAt less cursor.timestamp
                    pageCondition = pageCondition and if (cursor.id == null) older else {
                        older or ((ReportsTable.createdAt eq cursor.timestamp) and (ReportsTable.id less cursor.id))
                    }
                }
            }
            val rows = ReportsTable.selectAll().where { pageCondition }
                .orderBy(ReportsTable.createdAt to SortOrder.DESC, ReportsTable.id to SortOrder.DESC)
                .limit(filter.limit + 1).toList()
            val hasNext = rows.size > filter.limit
            val pageRows = if (hasNext) rows.dropLast(1) else rows
            val ids = pageRows.map { it[ReportsTable.id].value }
            val categories = CategoriesTable.selectAll().associateBy { it[CategoriesTable.id].value }
            val imageReportIds = if (ids.isEmpty()) emptySet() else ReportImagesTable.selectAll()
                .where { ReportImagesTable.reportId inList ids }
                .mapTo(mutableSetOf()) { it[ReportImagesTable.reportId].value }
            val assignments = activeAssignments(ids)
            val teams = TeamsTable.selectAll().associate { it[TeamsTable.id].value to it[TeamsTable.name] }
            val admins = AdminUsersTable.selectAll().associate { it[AdminUsersTable.id].value to it[AdminUsersTable.username] }

            CursorPage(
                items = pageRows.map { row ->
                    val reportId = row[ReportsTable.id].value
                    val category = categories[row[ReportsTable.categoryId].value]
                    val assignment = assignments[reportId]
                    ReportAdminSummary(
                        id = reportId.toString(),
                        title = row[ReportsTable.title],
                        categorySlug = category?.get(CategoriesTable.slug) ?: "unknown",
                        categoryName = category?.get(CategoriesTable.name) ?: "Desconocida",
                        status = row[ReportsTable.status].name,
                        priority = row[ReportsTable.priority].name,
                        dueAt = row[ReportsTable.dueAt]?.toString(),
                        teamName = assignment?.get(ReportAssignmentsTable.teamId)?.value?.let(teams::get),
                        responsibleAdminName = assignment?.get(ReportAssignmentsTable.responsibleAdminUserId)?.value?.let(admins::get),
                        latitude = row[ReportsTable.location].latitude,
                        longitude = row[ReportsTable.location].longitude,
                        hasImage = reportId in imageReportIds,
                        version = row[ReportsTable.version],
                        createdAt = row[ReportsTable.createdAt].toString(),
                        updatedAt = row[ReportsTable.updatedAt].toString(),
                    )
                },
                nextCursor = if (hasNext) pageRows.lastOrNull()?.let {
                    StableCursorCodec.encode(it[ReportsTable.createdAt], it[ReportsTable.id].value)
                } else null,
                totalCount = totalCount,
            )
        }

    override suspend fun findDetail(id: String): ReportAdminDetail? = newSuspendedTransaction(Dispatchers.IO) {
        val reportId = runCatching { UUID.fromString(id) }.getOrNull() ?: return@newSuspendedTransaction null
        val row = ReportsTable.selectAll().where {
            (ReportsTable.id eq reportId) and (ReportsTable.deletedAt.isNull())
        }.singleOrNull() ?: return@newSuspendedTransaction null
        val category = CategoriesTable.selectAll().where {
            CategoriesTable.id eq row[ReportsTable.categoryId].value
        }.single()
        val assignment = ReportAssignmentsTable.selectAll().where {
            (ReportAssignmentsTable.reportId eq reportId) and (ReportAssignmentsTable.unassignedAt.isNull())
        }.singleOrNull()
        val teamName = assignment?.get(ReportAssignmentsTable.teamId)?.value?.let { teamId ->
            TeamsTable.selectAll().where { TeamsTable.id eq teamId }.singleOrNull()?.get(TeamsTable.name)
        }
        val responsibleName = assignment?.get(ReportAssignmentsTable.responsibleAdminUserId)?.value?.let { adminId ->
            AdminUsersTable.selectAll().where { AdminUsersTable.id eq adminId }.singleOrNull()?.get(AdminUsersTable.username)
        }
        val image = ReportImagesTable.selectAll().where { ReportImagesTable.reportId eq reportId }.singleOrNull()

        ReportAdminDetail(
            id = id,
            title = row[ReportsTable.title],
            description = row[ReportsTable.description],
            categorySlug = category[CategoriesTable.slug],
            categoryName = category[CategoriesTable.name],
            status = row[ReportsTable.status].name,
            priority = row[ReportsTable.priority].name,
            dueAt = row[ReportsTable.dueAt]?.toString(),
            submissionMode = if (row[ReportsTable.userId] == null) "ANONYMOUS" else "ACCOUNT",
            assignment = assignment?.let {
                AssignmentInfo(
                    teamId = it[ReportAssignmentsTable.teamId]?.value?.toString(),
                    teamName = teamName,
                    responsibleAdminUserId = it[ReportAssignmentsTable.responsibleAdminUserId]?.value?.toString(),
                    responsibleAdminName = responsibleName,
                    assignedBy = it[ReportAssignmentsTable.assignedBy].value.toString(),
                    assignedAt = it[ReportAssignmentsTable.assignedAt].toString(),
                )
            },
            latitude = row[ReportsTable.location].latitude,
            longitude = row[ReportsTable.location].longitude,
            hasImage = image != null,
            imageChecksum = image?.get(ReportImagesTable.checksumSha256),
            version = row[ReportsTable.version],
            createdAt = row[ReportsTable.createdAt].toString(),
            updatedAt = row[ReportsTable.updatedAt].toString(),
            statusHistory = ReportStatusHistoryTable.selectAll()
                .where { ReportStatusHistoryTable.reportId eq reportId }
                .orderBy(ReportStatusHistoryTable.changedAt to SortOrder.ASC)
                .map {
                    AdminStatusHistoryEntry(
                        fromStatus = it[ReportStatusHistoryTable.fromStatus]?.name,
                        toStatus = it[ReportStatusHistoryTable.toStatus].name,
                        changedBy = it[ReportStatusHistoryTable.changedBy]?.value?.toString(),
                        note = it[ReportStatusHistoryTable.note],
                        changedAt = it[ReportStatusHistoryTable.changedAt].toString(),
                    )
                },
            priorityHistory = ReportPriorityHistoryTable.selectAll()
                .where { ReportPriorityHistoryTable.reportId eq reportId }
                .orderBy(ReportPriorityHistoryTable.changedAt to SortOrder.ASC)
                .map {
                    PriorityHistoryEntry(
                        fromPriority = it[ReportPriorityHistoryTable.fromPriority]?.name,
                        toPriority = it[ReportPriorityHistoryTable.toPriority].name,
                        fromDueAt = it[ReportPriorityHistoryTable.fromDueAt]?.toString(),
                        toDueAt = it[ReportPriorityHistoryTable.toDueAt]?.toString(),
                        changedBy = it[ReportPriorityHistoryTable.changedBy].value.toString(),
                        changedAt = it[ReportPriorityHistoryTable.changedAt].toString(),
                    )
                },
        )
    }

    private fun assignmentReportIds(teamId: String?, adminUserId: String?): List<UUID> {
        if (teamId == null && adminUserId == null) return emptyList()
        var condition: Op<Boolean> = ReportAssignmentsTable.unassignedAt.isNull()
        teamId?.let {
            val id = runCatching { UUID.fromString(it) }.getOrNull() ?: return emptyList()
            condition = condition and (ReportAssignmentsTable.teamId eq id)
        }
        adminUserId?.let {
            val id = runCatching { UUID.fromString(it) }.getOrNull() ?: return emptyList()
            condition = condition and (ReportAssignmentsTable.responsibleAdminUserId eq id)
        }
        return ReportAssignmentsTable.selectAll().where { condition }
            .map { it[ReportAssignmentsTable.reportId].value }
    }

    private fun activeAssignments(ids: List<UUID>): Map<UUID, ResultRow> {
        if (ids.isEmpty()) return emptyMap()
        return ReportAssignmentsTable.selectAll().where {
            (ReportAssignmentsTable.reportId inList ids) and (ReportAssignmentsTable.unassignedAt.isNull())
        }.associateBy { it[ReportAssignmentsTable.reportId].value }
    }
}
