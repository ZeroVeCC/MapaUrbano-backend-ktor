package com.mapaurbano.application

import com.mapaurbano.assignments.application.AssignReportUseCase
import com.mapaurbano.assignments.application.CreateTeamUseCase
import com.mapaurbano.assignments.application.ManageTeamMembersUseCase
import com.mapaurbano.assignments.application.UnassignReportUseCase
import com.mapaurbano.assignments.application.UpdateTeamUseCase
import com.mapaurbano.assignments.application.TeamQueryUseCase
import com.mapaurbano.assignments.domain.AssignmentRepository
import com.mapaurbano.assignments.domain.TeamRepository
import com.mapaurbano.audit.domain.AuditRepository
import com.mapaurbano.audit.application.ListAuditEventsUseCase
import com.mapaurbano.audit.application.RecordAuditEventUseCase
import com.mapaurbano.auth.application.LoginAdminUseCase
import com.mapaurbano.auth.application.AuthenticateUserUseCase
import com.mapaurbano.auth.application.LoginUserUseCase
import com.mapaurbano.auth.application.LogoutAdminUseCase
import com.mapaurbano.auth.application.LogoutUserUseCase
import com.mapaurbano.auth.domain.AdminSessionRepository
import com.mapaurbano.auth.domain.AdminUserRepository
import com.mapaurbano.categories.application.ListCategoriesUseCase
import com.mapaurbano.categories.application.ManageCategoryUseCase
import com.mapaurbano.categories.domain.CategoryRepository
import com.mapaurbano.infrastructure.database.repositories.AdminSessionRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AdminReportQueryRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AdminUserRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AssignmentRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.AuditRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.CategoryRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ImageRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ReportRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.ReportSubmissionRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.SessionRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.TeamRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.UserRepositoryImpl
import com.mapaurbano.infrastructure.database.repositories.StatisticsRepositoryImpl
import com.mapaurbano.media.application.GetImageUseCase
import com.mapaurbano.media.application.GetOwnImageUseCase
import com.mapaurbano.media.application.ProcessImageUseCase
import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.reports.application.ChangeReportPriorityUseCase
import com.mapaurbano.reports.application.ChangeReportStatusUseCase
import com.mapaurbano.reports.application.CreateReportUseCase
import com.mapaurbano.reports.application.DeleteReportUseCase
import com.mapaurbano.reports.application.AdminReportQueryUseCase
import com.mapaurbano.reports.domain.AdminReportQueryRepository
import com.mapaurbano.reports.domain.ReportRepository
import com.mapaurbano.reports.domain.ReportSubmissionRepository
import com.mapaurbano.statistics.application.GetStatisticsUseCase
import com.mapaurbano.statistics.domain.StatisticsRepository
import com.mapaurbano.users.application.DeactivateUserUseCase
import com.mapaurbano.users.application.GetCurrentUserUseCase
import com.mapaurbano.users.application.ListUserReportsUseCase
import com.mapaurbano.users.application.GetOwnReportUseCase
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
    single<ReportSubmissionRepository> { ReportSubmissionRepositoryImpl() }
    single<AdminReportQueryRepository> { AdminReportQueryRepositoryImpl() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<TeamRepository> { TeamRepositoryImpl() }
    single<AssignmentRepository> { AssignmentRepositoryImpl() }
    single<AuditRepository> { AuditRepositoryImpl() }
    single<ImageRepository> { ImageRepositoryImpl() }
    single<StatisticsRepository> { StatisticsRepositoryImpl() }
}

val applicationModule = module {
    single { EventBus() }

    // Auth
    singleOf(::LoginUserUseCase)
    singleOf(::AuthenticateUserUseCase)
    singleOf(::LoginAdminUseCase)
    singleOf(::LogoutUserUseCase)
    singleOf(::LogoutAdminUseCase)

    // Users
    singleOf(::RegisterUserUseCase)
    singleOf(::GetCurrentUserUseCase)
    singleOf(::DeactivateUserUseCase)
    singleOf(::ListUserReportsUseCase)
    singleOf(::GetOwnReportUseCase)

    // Reports
    singleOf(::CreateReportUseCase)
    singleOf(::ChangeReportStatusUseCase)
    singleOf(::ChangeReportPriorityUseCase)
    singleOf(::DeleteReportUseCase)
    singleOf(::AdminReportQueryUseCase)

    // Assignments
    singleOf(::CreateTeamUseCase)
    singleOf(::UpdateTeamUseCase)
    singleOf(::ManageTeamMembersUseCase)
    singleOf(::AssignReportUseCase)
    singleOf(::UnassignReportUseCase)
    singleOf(::TeamQueryUseCase)

    // Categories
    singleOf(::ListCategoriesUseCase)
    singleOf(::ManageCategoryUseCase)

    // Media
    singleOf(::GetImageUseCase)
    singleOf(::GetOwnImageUseCase)
    singleOf(::ProcessImageUseCase)

    // Statistics
    singleOf(::GetStatisticsUseCase)
    singleOf(::ListAuditEventsUseCase)
    singleOf(::RecordAuditEventUseCase)
}



