package com.mapaurbano.reports.api

import com.mapaurbano.reports.domain.SubmissionMode
import com.mapaurbano.reports.application.CreateReportUseCase
import com.mapaurbano.media.application.ProcessImageUseCase
import com.mapaurbano.shared.domain.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import com.mapaurbano.auth.application.UserPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

fun Route.authenticatedReportRoutes() {
    val createReportUseCase by inject<CreateReportUseCase>()
    route("/reports") {
        post {
            var title = ""
            var description = ""
            var categorySlug = ""
            var lat: Double? = null
            var lng: Double? = null
            var mode = SubmissionMode.ACCOUNT
            var imageBytes: ByteArray? = null
            var imageContentType: String? = null
            var imageFileName: String? = null

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "title" -> title = part.value
                            "description" -> description = part.value
                            "categorySlug" -> categorySlug = part.value
                            "latitude" -> lat = part.value.toDoubleOrNull()
                                ?: throw ValidationException("Latitud inválida")
                            "longitude" -> lng = part.value.toDoubleOrNull()
                                ?: throw ValidationException("Longitud inválida")
                            "submissionMode" -> mode = try {
                                SubmissionMode.valueOf(part.value.uppercase())
                            } catch (_: IllegalArgumentException) {
                                throw ValidationException("Modo de envío inválido")
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            imageBytes = part.provider()
                                .readRemaining((ProcessImageUseCase.MAX_IMAGE_BYTES + 1).toLong())
                                .readByteArray()
                            imageContentType = part.contentType?.toString() ?: "image/jpeg"
                            imageFileName = part.originalFileName
                        }
                    }
                    else -> {}
                }
                part.release()
            }

            val userId = call.principal<UserPrincipal>()?.userId
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val validLatitude = lat ?: throw ValidationException("Latitud obligatoria")
            val validLongitude = lng ?: throw ValidationException("Longitud obligatoria")
            val response = createReportUseCase.execute(
                title = title,
                description = description,
                categorySlug = categorySlug,
                latitude = validLatitude,
                longitude = validLongitude,
                submissionMode = mode,
                userId = userId,
                imageData = imageBytes,
                imageContentType = imageContentType,
                imageOriginalFilename = imageFileName,
            )
            call.respond(HttpStatusCode.Created, response)
        }

    }
}







