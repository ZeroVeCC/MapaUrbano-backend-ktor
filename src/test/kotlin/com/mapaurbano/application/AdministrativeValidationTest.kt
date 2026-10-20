package com.mapaurbano.application

import com.mapaurbano.assignments.application.CreateTeamUseCase
import com.mapaurbano.assignments.application.ManageTeamMembersUseCase
import com.mapaurbano.assignments.application.UpdateTeamUseCase
import com.mapaurbano.assignments.domain.Team
import com.mapaurbano.assignments.domain.TeamMember
import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.assignments.dto.CreateTeamRequest
import com.mapaurbano.assignments.dto.UpdateTeamRequest
import com.mapaurbano.audit.application.ListAuditEventsUseCase
import com.mapaurbano.audit.domain.AuditEvent
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.auth.domain.AdminUser
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.categories.application.ManageCategoryUseCase
import com.mapaurbano.categories.domain.Category
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.categories.dto.CreateCategoryRequest
import com.mapaurbano.reports.application.AdminReportQueryUseCase
import com.mapaurbano.reports.domain.AdminReportFilter
import com.mapaurbano.reports.domain.AdminReportQueryRepository
import com.mapaurbano.reports.dto.ReportAdminDetail
import com.mapaurbano.reports.dto.ReportAdminSummary
import com.mapaurbano.shared.api.CursorPage
import com.mapaurbano.shared.domain.ConflictException
import com.mapaurbano.shared.domain.NotFoundException
import com.mapaurbano.shared.domain.ValidationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class AdministrativeValidationTest {
    @Test
    fun `admin report filters reject malformed UUIDs before querying persistence`(): Unit = runBlocking {
        val repository = RecordingAdminReportRepository()
        val useCase = AdminReportQueryUseCase(repository)

        assertThrows<ValidationException> {
            useCase.list(null, null, null, "not-a-uuid", null, null, null, 20)
        }
        assertThrows<ValidationException> {
            useCase.list(null, null, null, null, "not-a-uuid", null, null, 20)
        }
        kotlin.test.assertEquals(0, repository.listCalls)
    }

    @Test
    fun `audit rejects malformed cursor instead of silently returning the first page`(): Unit = runBlocking {
        val useCase = ListAuditEventsUseCase(EmptyAuditRepository())

        assertThrows<ValidationException> { useCase.execute("invalid-cursor", 20) }
    }

    @Test
    fun `team creation and update reject duplicate or blank names`(): Unit = runBlocking {
        val existing = team("Equipo vial")
        val repository = FakeTeamRepository(mutableListOf(existing))

        assertThrows<ConflictException> {
            CreateTeamUseCase(repository).execute(CreateTeamRequest("Equipo vial"))
        }
        assertThrows<ValidationException> {
            UpdateTeamUseCase(repository).execute(existing.id, UpdateTeamRequest(name = "   "))
        }
    }

    @Test
    fun `adding an inactive or unknown administrator to a team returns not found`(): Unit = runBlocking {
        val existingTeam = team("Equipo vial")
        val teamRepository = FakeTeamRepository(mutableListOf(existingTeam))
        val inactiveAdmin = admin("admin-inactive", isActive = false)
        val adminRepository = FakeAdminUserRepository(mapOf(inactiveAdmin.id to inactiveAdmin))
        val useCase = ManageTeamMembersUseCase(teamRepository, adminRepository)

        assertThrows<NotFoundException> { useCase.addMember(existingTeam.id, inactiveAdmin.id) }
        assertThrows<NotFoundException> { useCase.addMember(existingTeam.id, "00000000-0000-0000-0000-000000000000") }
        kotlin.test.assertEquals(0, teamRepository.addMemberCalls)
    }

    @Test
    fun `invalid category fields are rejected before reaching PostgreSQL`(): Unit = runBlocking {
        val repository = FakeCategoryRepository()
        val useCase = ManageCategoryUseCase(repository)

        assertThrows<ValidationException> {
            useCase.create(CreateCategoryRequest("Slug Inválido", "Nombre", "#123456"))
        }
        assertThrows<ValidationException> {
            useCase.create(CreateCategoryRequest("valido", "Nombre", "azul"))
        }
        kotlin.test.assertEquals(0, repository.createCalls)
    }

    private class RecordingAdminReportRepository : AdminReportQueryRepository {
        var listCalls = 0

        override suspend fun list(filter: AdminReportFilter): CursorPage<ReportAdminSummary> {
            listCalls++
            return CursorPage(emptyList())
        }

        override suspend fun findDetail(id: String): ReportAdminDetail? = null
    }

    private class EmptyAuditRepository : AuditRepository {
        override suspend fun create(event: AuditEvent) = event
        override suspend fun findAll(cursor: String?, limit: Int) = emptyList<AuditEvent>()
        override suspend fun count() = 0L
    }

    private class FakeTeamRepository(private val teams: MutableList<Team>) : TeamRepository {
        var addMemberCalls = 0
        private val members = mutableListOf<TeamMember>()

        override suspend fun findAll() = teams.toList()
        override suspend fun findById(id: String) = teams.find { it.id == id }
        override suspend fun findByName(name: String) = teams.find { it.name == name }
        override suspend fun create(team: Team) = team.also(teams::add)
        override suspend fun update(team: Team): Boolean {
            val index = teams.indexOfFirst { it.id == team.id }
            if (index < 0) return false
            teams[index] = team
            return true
        }

        override suspend fun addMember(teamId: String, adminUserId: String): Boolean {
            addMemberCalls++
            members += TeamMember(teamId, adminUserId, Instant.now())
            return true
        }

        override suspend fun removeMember(teamId: String, adminUserId: String) =
            members.removeIf { it.teamId == teamId && it.adminUserId == adminUserId }

        override suspend fun getMembers(teamId: String) = members.filter { it.teamId == teamId }
    }

    private class FakeAdminUserRepository(private val users: Map<String, AdminUser>) : AdminUserRepository {
        override suspend fun findById(id: String) = users[id]
        override suspend fun findByUsername(username: String) = users.values.find { it.username == username }
        override suspend fun findAllActive() = users.values.filter { it.isActive }
        override suspend fun create(user: AdminUser) = user
        override suspend fun updateLastLogin(id: String, lastLoginAt: Instant) = Unit
    }

    private class FakeCategoryRepository : CategoryRepository {
        var createCalls = 0
        override suspend fun findAll() = emptyList<Category>()
        override suspend fun findById(id: String): Category? = null
        override suspend fun findBySlug(slug: String): Category? = null
        override suspend fun create(category: Category): Category {
            createCalls++
            return category
        }

        override suspend fun update(category: Category) = true
    }

    private fun team(name: String) = Team(
        id = "11111111-1111-1111-1111-111111111111",
        name = name,
        createdAt = Instant.parse("2026-10-19T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-19T00:00:00Z"),
    )

    private fun admin(username: String, isActive: Boolean) = AdminUser(
        id = "22222222-2222-2222-2222-222222222222",
        username = username,
        passwordHash = "hash",
        isActive = isActive,
        createdAt = Instant.parse("2026-10-19T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-19T00:00:00Z"),
    )
}
