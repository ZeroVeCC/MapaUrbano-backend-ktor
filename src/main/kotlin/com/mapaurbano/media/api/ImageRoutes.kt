package com.mapaurbano.media.api

import com.mapaurbano.media.application.GetImageUseCase
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.publicImageRoutes() {
    val getImageUseCase by inject<GetImageUseCase>()

    get("/reports/{id}/image") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
        val image = getImageUseCase.execute(id)

        val contentType = try {
            ContentType.parse(image.contentType)
        } catch (_: Exception) {
            ContentType.Image.JPEG
        }

        call.respondBytes(
            bytes = image.data,
            contentType = contentType,
            status = HttpStatusCode.OK
        )
    }
}

fun Route.adminImageRoutes() {
    // Additional admin image management endpoints if needed
}
