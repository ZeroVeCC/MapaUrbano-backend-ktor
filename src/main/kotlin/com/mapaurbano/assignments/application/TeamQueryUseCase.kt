package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.assignments.dto.*
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.shared.domain.NotFoundException

class TeamQueryUseCase(
    private val teamRepository: TeamRepository,
    private val adminUserRepository: AdminUserRepository,
) {
    suspend fun list(): List<TeamSummary> = teamRepository.findAll().map { team ->
        TeamSummary(
            id = team.id,
            name = team.name,
            description = team.description,
            isActive = team.isActive,
            memberCount = teamRepository.getMembers(team.id).size,
            createdAt = team.createdAt.toString(),
        )
    }

    suspend fun detail(id: String): TeamDetail {
        val team = teamRepository.findById(id) ?: throw NotFoundException("Equipo no encontrado")
        val admins = adminUserRepository.findAllActive().associateBy { it.id }
        return TeamDetail(
            id = team.id,
            name = team.name,
            description = team.description,
            isActive = team.isActive,
            members = teamRepository.getMembers(id).map { member ->
                TeamMemberInfo(
                    adminUserId = member.adminUserId,
                    username = admins[member.adminUserId]?.username ?: "usuario-inactivo",
                    joinedAt = member.joinedAt.toString(),
                )
            },
            createdAt = team.createdAt.toString(),
            updatedAt = team.updatedAt.toString(),
        )
    }

    suspend fun assignees(): List<AssigneeResponse> {
        val teams = teamRepository.findAll()
        val memberships = teams.flatMap { team ->
            teamRepository.getMembers(team.id).map { it.adminUserId to team.id }
        }.groupBy({ it.first }, { it.second })
        return adminUserRepository.findAllActive().map { admin ->
            AssigneeResponse(admin.id, admin.username, memberships[admin.id].orEmpty())
        }
    }
}
