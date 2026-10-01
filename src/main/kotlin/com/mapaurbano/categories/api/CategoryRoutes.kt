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
            val response = listCategoriesUseCase.execute()
            call.respond(HttpStatusCode.OK, response)
        }

        post {
            val request = call.receive<CreateCategoryRequest>()
            val response = manageCategoryUseCase.create(request)
            call.respond(HttpStatusCode.Created, response)
        }

        patch("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID required")
            val request = call.receive<UpdateCategoryRequest>()
            val response = manageCategoryUseCase.update(id, request)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
