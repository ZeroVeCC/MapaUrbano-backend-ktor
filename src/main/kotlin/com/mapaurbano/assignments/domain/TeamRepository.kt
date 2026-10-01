package com.mapaurbano.assignments.domain

interface TeamRepository {
    suspend fun findAll(): List<Team>
    suspend fun findById(id: String): Team?
    suspend fun create(team: Team): Team
    suspend fun update(team: Team): Boolean
    
    suspend fun addMember(teamId: String, adminUserId: String): Boolean
    suspend fun removeMember(teamId: String, adminUserId: String): Boolean
    suspend fun getMembers(teamId: String): List<TeamMember>
}
