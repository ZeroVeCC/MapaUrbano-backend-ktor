package com.mapaurbano.application

import com.mapaurbano.assignments.application.AssignReportUseCase
import com.mapaurbano.assignments.application.CreateTeamUseCase
import com.mapaurbano.assignments.application.ManageTeamMembersUseCase
import com.mapaurbano.assignments.application.UnassignReportUseCase
import com.mapaurbano.assignments.application.UpdateTeamUseCase
import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.auth.application.LoginAdminUseCase
import com.mapaurbano.auth.application.LoginUserUseCase
import com.mapaurbano.auth.application.LogoutAdminUseCase
import com.mapaurbano.auth.application.LogoutUserUseCase
import com.mapaurbano.auth.domain.AdminSessionRepository
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.categories.application.ListCategoriesUseCase
import com.mapaurbano.categories.application.ManageCategoryUseCase
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.infrastructure.database.repositories.AdminSessionRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AdminUserRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AssignmentRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AuditRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.CategoryRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ImageRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ReportRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.SessionRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.TeamRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.UserRepositoryImpl
import com.mapaurbano.media.application.GetImageUseCase
import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.reports.application.ChangeReportPriorityUseCase
import com.mapaurbano.reports.application.ChangeReportStatusUseCase
import com.mapaurbano.reports.application.CreateReportUseCase
import com.mapaurbano.reports.application.DeleteReportUseCase
import com.mapaurbano.reports.application.GetPublicReportUseCase
import com.mapaurbano.reports.application.GetReportByTrackingCodeUseCase
import com.mapaurbano.reports.application.ListPublicReportsUseCase
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.statistics.application.GetStatisticsUseCase
import com.mapaurbano.users.application.DeactivateUserUseCase
import com.mapaurbano.users.application.GetCurrentUserUseCase
import com.mapaurbano.users.application.ListUserReportsUseCase
import com.mapaurbano.users.application.RegisterUserUseCase
import com.mapaurbano.users.domain.SessionRepository
import com.mapaurbano.users.domain.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<SessionRepository> { SessionRepositoryImpl() }
    single<AdminUserRepository> { AdminUserRepositoryImpl() }
    single<AdminSessionRepository> { AdminSessionRepositoryImpl() }
    single<ReportRepository> { ReportRepositoryImpl() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<TeamRepository> { TeamRepositoryImpl() }
    single<AssignmentRepository> { AssignmentRepositoryImpl() }
    single<AuditRepository> { AuditRepositoryImpl() }
    single<ImageRepository> { ImageRepositoryImpl() }
}

val applicationModule = module {
    single { EventBus() }

    // Auth
    singleOf(::LoginUserUseCase)
    singleOf(::LoginAdminUseCase)
    singleOf(::LogoutUserUseCase)
    singleOf(::LogoutAdminUseCase)

    // Users
    singleOf(::RegisterUserUseCase)
    singleOf(::GetCurrentUserUseCase)
    singleOf(::DeactivateUserUseCase)
    singleOf(::ListUserReportsUseCase)

    // Reports
    singleOf(::CreateReportUseCase)
    singleOf(::ListPublicReportsUseCase)
    singleOf(::GetPublicReportUseCase)
    singleOf(::GetReportByTrackingCodeUseCase)
    singleOf(::ChangeReportStatusUseCase)
    singleOf(::ChangeReportPriorityUseCase)
    singleOf(::DeleteReportUseCase)

    // Assignments
    singleOf(::CreateTeamUseCase)
    singleOf(::UpdateTeamUseCase)
    singleOf(::ManageTeamMembersUseCase)
    singleOf(::AssignReportUseCase)
    singleOf(::UnassignReportUseCase)

    // Categories
    singleOf(::ListCategoriesUseCase)
    singleOf(::ManageCategoryUseCase)

    // Media
    singleOf(::GetImageUseCase)

    // Statistics
    singleOf(::GetStatisticsUseCase)
}
