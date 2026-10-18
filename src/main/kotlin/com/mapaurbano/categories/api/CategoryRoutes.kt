package com.mapaurbano.categories.api

import com.mapaurbano.categories.application.ListCategoriesUseCase
import com.mapaurbano.categories.application.ManageCategoryUseCase
import com.mapaurbano.categories.dto.CreateCategoryRequest
import com.mapaurbano.categories.dto.UpdateCategoryRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import com.mapaurbano.application.AdminPrincipal
import com.mapaurbano.application.hasValidAdminCsrf
import com.mapaurbano.shared.api.respondApiError
import io.ktor.server.auth.principal

fun Route.publicCategoryRoutes() {
    val listCategoriesUseCase by inject<ListCategoriesUseCase>()

    get("/categories") {
        val response = listCategoriesUseCase.execute()
        call.respond(HttpStatusCode.OK, response)
    }
}

fun Route.adminCategoryRoutes() {
    val listCategoriesUseCase by inject<ListCategoriesUseCase>()
    val manageCategoryUseCase by inject<ManageCategoryUseCase>()

    route("/categories") {
        get {
            val response = listCategoriesUseCase.execute(includeInactive = true)
            call.respond(HttpStatusCode.OK, response)
        }

        post {
            if (!call.requireCategoryCsrf()) return@post
            val request = call.receive<CreateCategoryRequest>()
            val response = manageCategoryUseCase.create(request)
            call.respond(HttpStatusCode.Created, response)
        }

        patch("/{id}") {
            if (!call.requireCategoryCsrf()) return@patch
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID required")
            val request = call.receive<UpdateCategoryRequest>()
            val response = manageCategoryUseCase.update(id, request)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.requireCategoryCsrf(): Boolean {
    val principal = principal<AdminPrincipal>() ?: run {
        respond(HttpStatusCode.Unauthorized)
        return false
    }
    if (!hasValidAdminCsrf(principal)) {
        respondApiError(HttpStatusCode.Forbidden, "CSRF_INVALID", "El token CSRF es inválido.")
        return false
    }
    return true
}
