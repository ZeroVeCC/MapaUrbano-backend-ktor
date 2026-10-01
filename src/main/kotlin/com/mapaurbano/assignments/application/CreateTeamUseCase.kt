package com.mapaurbano.assignments.application

import com.mapaurbano.assignments.domain.Team
import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.assignments.dto.CreateTeamRequest
import com.mapaurbano.assignments.dto.TeamSummary
import com.mapaurbano.shared.domain.ValidationException
import java.time.Instant
import java.util.UUID

class CreateTeamUseCase(
    private val teamRepository: TeamRepository
) {
    suspend fun execute(request: CreateTeamRequest): TeamSummary {
        val name = request.name.trim()
        if (name.isBlank()) {
            throw ValidationException(message = "El nombre del equipo es obligatorio")
        }

        val now = Instant.now()
        val team = Team(
            id = UUID.randomUUID().toString(),
            name = name,
            description = request.description?.trim(),
            createdAt = now,
            updatedAt = now
        )

        val createdTeam = teamRepository.create(team)

        return TeamSummary(
            id = createdTeam.id,
            name = createdTeam.name,
            description = createdTeam.description,
            isActive = createdTeam.isActive,
            memberCount = 0,
            createdAt = createdTeam.createdAt.toString()
        )
    }
}
