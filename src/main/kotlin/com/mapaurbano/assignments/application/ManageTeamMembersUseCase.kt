package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.shared.domain.NotFoundException

class ManageTeamMembersUseCase(
    private val teamRepository: TeamRepository
) {
    suspend fun addMember(teamId: String, adminUserId: String): Boolean {
        val team = teamRepository.findById(teamId)
            ?: throw NotFoundException("Equipo no encontrado")

        return teamRepository.addMember(team.id, adminUserId)
    }

    suspend fun removeMember(teamId: String, adminUserId: String): Boolean {
        val team = teamRepository.findById(teamId)
            ?: throw NotFoundException("Equipo no encontrado")

        return teamRepository.removeMember(team.id, adminUserId)
    }
}
