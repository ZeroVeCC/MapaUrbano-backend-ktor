package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.assignments.dto.TeamSummary
import com.mapaurbano.assignments.dto.UpdateTeamRequest
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.ValidationException
import java.time.Instant

class UpdateTeamUseCase(
    private val teamRepository: TeamRepository
) {
    suspend fun execute(teamId: String, request: UpdateTeamRequest): TeamSummary {
        val team = teamRepository.findById(teamId)
            ?: throw NotFoundException("Equipo no encontrado")
        val requestedName = request.name?.trim()
        if (requestedName != null && requestedName.isBlank()) {
            throw ValidationException("El nombre del equipo no puede quedar vacío")
        }
        if (requestedName != null && requestedName.length > 120) {
            throw ValidationException("El nombre del equipo no puede superar 120 caracteres")
        }
        if (requestedName != null) {
            val existing = teamRepository.findByName(requestedName)
            if (existing != null && existing.id != teamId) {
                throw ConflictException("Ya existe un equipo con ese nombre")
            }
        }

        val updatedTeam = team.copy(
            name = requestedName ?: team.name,
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
