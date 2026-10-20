package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.shared.domain.NotFoundException

class ManageTeamMembersUseCase(
    private val teamRepository: TeamRepository,
    private val adminUserRepository: AdminUserRepository,
) {
    suspend fun addMember(teamId: String, adminUserId: String): Boolean {
        val team = teamRepository.findById(teamId)
            ?: throw NotFoundException("Equipo no encontrado")
        val admin = adminUserRepository.findById(adminUserId)
            ?.takeIf { it.isActive }
            ?: throw NotFoundException("Administrador no encontrado o inactivo")

        return teamRepository.addMember(team.id, admin.id)
    }

    suspend fun removeMember(teamId: String, adminUserId: String): Boolean {
        val team = teamRepository.findById(teamId)
            ?: throw NotFoundException("Equipo no encontrado")

        return teamRepository.removeMember(team.id, adminUserId)
    }
}
