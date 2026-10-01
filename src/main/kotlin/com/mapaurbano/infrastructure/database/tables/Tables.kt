package com.mapaurbano.infrastructure.database.tables

import com.mapaurbano.reports.domain.ReportPriority
import com.mapaurbano.reports.domain.ReportStatus
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object CategoriesTable : UUIDTable("categories") {
    val slug = varchar("slug", 50).uniqueIndex()
    val name = varchar("name", 80)
    val colorHex = char("color_hex", 7)
    val isActive = bool("is_active").default(true)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at")
}

object AdminUsersTable : UUIDTable("admin_users") {
    val username = varchar("username", 80).uniqueIndex("admin_users_username_lower_uq")
    val passwordHash = text("password_hash")
    val isActive = bool("is_active").default(true)
    val lastLoginAt = timestamp("last_login_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 254).uniqueIndex("users_email_lower_uq")
    val displayName = varchar("display_name", 100)
    val passwordHash = text("password_hash")
    val isActive = bool("is_active").default(true)
    val emailVerifiedAt = timestamp("email_verified_at").nullable()
    val lastLoginAt = timestamp("last_login_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
}

object UserSessionsTable : UUIDTable("user_sessions") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = binary("token_hash").uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val lastUsedAt = timestamp("last_used_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val createdAt = timestamp("created_at")
}

object TeamsTable : UUIDTable("teams") {
    val name = varchar("name", 120).uniqueIndex()
    val description = text("description").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object TeamMembersTable : Table("team_members") {
    val teamId = reference("team_id", TeamsTable)
    val adminUserId = reference("admin_user_id", AdminUsersTable)
    val joinedAt = timestamp("joined_at")

    override val primaryKey = PrimaryKey(teamId, adminUserId)
}

object ReportsTable : UUIDTable("reports") {
    val categoryId = reference("category_id", CategoriesTable)
    val userId = reference("user_id", UsersTable).nullable()
    val status = pgEnum<ReportStatus>("status", "report_status").default(ReportStatus.PENDING)
    val priority = pgEnum<ReportPriority>("priority", "report_priority").default(ReportPriority.MEDIUM)
    val title = varchar("title", 150)
    val description = text("description")
    val location = geoPoint("location")
    val dueAt = timestamp("due_at").nullable()
    val trackingCodeHash = binary("tracking_code_hash").nullable().uniqueIndex()
    val trackingCodeHint = varchar("tracking_code_hint", 8).nullable()
    val version = long("version").default(0L)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
}

object ReportImagesTable : UUIDTable("report_images") {
    val reportId = reference("report_id", ReportsTable).uniqueIndex()
    val data = binary("data")
    val contentType = varchar("content_type", 100)
    val sizeBytes = long("size_bytes")
    val widthPx = integer("width_px")
    val heightPx = integer("height_px")
    val originalFilename = varchar("original_filename", 255).nullable()
    val checksumSha256 = char("checksum_sha256", 64)
    val createdAt = timestamp("created_at")
}

object ReportStatusHistoryTable : UUIDTable("report_status_history") {
    val reportId = reference("report_id", ReportsTable)
    val changedBy = reference("changed_by", AdminUsersTable).nullable()
    val fromStatus = pgEnum<ReportStatus>("from_status", "report_status").nullable()
    val toStatus = pgEnum<ReportStatus>("to_status", "report_status")
    val note = text("note").nullable()
    val changedAt = timestamp("changed_at")
}

object ReportPriorityHistoryTable : UUIDTable("report_priority_history") {
    val reportId = reference("report_id", ReportsTable)
    val changedBy = reference("changed_by", AdminUsersTable)
    val fromPriority = pgEnum<ReportPriority>("from_priority", "report_priority").nullable()
    val toPriority = pgEnum<ReportPriority>("to_priority", "report_priority")
    val fromDueAt = timestamp("from_due_at").nullable()
    val toDueAt = timestamp("to_due_at").nullable()
    val changedAt = timestamp("changed_at")
}

object ReportAssignmentsTable : UUIDTable("report_assignments") {
    val reportId = reference("report_id", ReportsTable)
    val teamId = reference("team_id", TeamsTable).nullable()
    val responsibleAdminUserId = reference("responsible_admin_user_id", AdminUsersTable).nullable()
    val assignedBy = reference("assigned_by", AdminUsersTable)
    val assignedAt = timestamp("assigned_at")
    val unassignedAt = timestamp("unassigned_at").nullable()
}

object AuditEventsTable : UUIDTable("audit_events") {
    val actorAdminUserId = reference("actor_admin_user_id", AdminUsersTable).nullable()
    val actorUserId = reference("actor_user_id", UsersTable).nullable()
    val action = varchar("action", 120)
    val entityType = varchar("entity_type", 80)
    val entityId = uuid("entity_id").nullable()
    // jsonb support requires kotlinx.serialization configuration or a custom column type. Using text as workaround.
    val metadata = text("metadata").default("{}") 
    val sourceIp = varchar("source_ip", 45).nullable() // inet stored as string
    val occurredAt = timestamp("occurred_at")
}
