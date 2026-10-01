package com.mapaurbano.infrastructure.database.repositories

import com.mapaurbano.assignments.domain.Team
import com.mapaurbano.assignments.domain.TeamMember
import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.infrastructure.database.tables.TeamMembersTable
import com.mapaurbano.infrastructure.database.tables.TeamsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class TeamRepositoryImpl : TeamRepository {
    override suspend fun findAll(): List<Team> = newSuspendedTransaction(Dispatchers.IO) {
        TeamsTable.selectAll().map { it.toTeam() }
    }

    override suspend fun findById(id: String): Team? = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { return@newSuspendedTransaction null }
        TeamsTable.selectAll().where { TeamsTable.id eq uuid }.singleOrNull()?.toTeam()
    }

    override suspend fun create(team: Team): Team = newSuspendedTransaction(Dispatchers.IO) {
        TeamsTable.insert {
            it[id] = UUID.fromString(team.id)
            it[name] = team.name
            it[description] = team.description
            it[isActive] = team.isActive
            it[createdAt] = team.createdAt
            it[updatedAt] = team.updatedAt
        }
        team
    }

    override suspend fun update(team: Team): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val uuid = try { UUID.fromString(team.id) } catch (_: Exception) { return@newSuspendedTransaction false }
        val updatedRows = TeamsTable.update({ TeamsTable.id eq uuid }) {
            it[name] = team.name
            it[description] = team.description
            it[isActive] = team.isActive
            it[updatedAt] = Instant.now()
        }
        updatedRows > 0
    }

    override suspend fun addMember(teamId: String, adminUserId: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val teamUuid = try { UUID.fromString(teamId) } catch (_: Exception) { return@newSuspendedTransaction false }
        val userUuid = try { UUID.fromString(adminUserId) } catch (_: Exception) { return@newSuspendedTransaction false }

        try {
            TeamMembersTable.insert {
                it[TeamMembersTable.teamId] = teamUuid
                it[TeamMembersTable.adminUserId] = userUuid
                it[joinedAt] = Instant.now()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun removeMember(teamId: String, adminUserId: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val teamUuid = try { UUID.fromString(teamId) } catch (_: Exception) { return@newSuspendedTransaction false }
        val userUuid = try { UUID.fromString(adminUserId) } catch (_: Exception) { return@newSuspendedTransaction false }

        val deletedRows = TeamMembersTable.deleteWhere {
            (TeamMembersTable.teamId eq teamUuid) and (TeamMembersTable.adminUserId eq userUuid)
        }
        deletedRows > 0
    }

    override suspend fun getMembers(teamId: String): List<TeamMember> = newSuspendedTransaction(Dispatchers.IO) {
        val teamUuid = try { UUID.fromString(teamId) } catch (_: Exception) { return@newSuspendedTransaction emptyList() }
        TeamMembersTable.selectAll().where { TeamMembersTable.teamId eq teamUuid }.map {
            TeamMember(
                teamId = it[TeamMembersTable.teamId].value.toString(),
                adminUserId = it[TeamMembersTable.adminUserId].value.toString(),
                joinedAt = it[TeamMembersTable.joinedAt]
            )
        }
    }

    private fun ResultRow.toTeam(): Team = Team(
        id = this[TeamsTable.id].value.toString(),
        name = this[TeamsTable.name],
        description = this[TeamsTable.description],
        isActive = this[TeamsTable.isActive],
        createdAt = this[TeamsTable.createdAt],
        updatedAt = this[TeamsTable.updatedAt]
    )
}
