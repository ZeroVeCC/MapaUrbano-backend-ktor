package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.assignments.dto.TeamSummary
import com.mapaurbano.assignments.dto.UpdateTeamRequest
import com.mapaurbano.shared.domain.NotFoundException
import java.time.Instant

class UpdateTeamUseCase(
    private val teamRepository: TeamRepository
) {
    suspend fun execute(teamId: String, request: UpdateTeamRequest): TeamSummary {
        val team = teamRepository.findById(teamId)
            ?: throw NotFoundException("Equipo no encontrado")

        val updatedTeam = team.copy(
            name = request.name?.trim() ?: team.name,
            description = request.description?.trim() ?: team.description,
            isActive = request.isActive ?: team.isActive,
            updatedAt = Instant.now()
        )

        teamRepository.update(updatedTeam)
        
        val membersCount = teamRepository.getMembers(teamId).size

        return TeamSummary(
            id = updatedTeam.id,
            name = updatedTeam.name,
            description = updatedTeam.description,
            isActive = updatedTeam.isActive,
            memberCount = membersCount,
            createdAt = updatedTeam.createdAt.toString()
        )
    }
}
