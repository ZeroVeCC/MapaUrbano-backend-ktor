package com.mapaurbano.reports.api

import com.mapaurbano.reports.domain.SubmissionMode
import com.mapaurbano.reports.dto.TrackingCodeRequest
import com.mapaurbano.reports.application.CreateReportUseCase
import com.mapaurbano.reports.application.GetPublicReportUseCase
import com.mapaurbano.reports.application.GetReportByTrackingCodeUseCase
import com.mapaurbano.reports.application.ListPublicReportsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.publicReportRoutes() {
    val createReportUseCase by inject<CreateReportUseCase>()
    val listPublicReportsUseCase by inject<ListPublicReportsUseCase>()
    val getPublicReportUseCase by inject<GetPublicReportUseCase>()
    val getReportByTrackingCodeUseCase by inject<GetReportByTrackingCodeUseCase>()

    route("/reports") {
        get {
            val minLat = call.request.queryParameters["minLat"]?.toDoubleOrNull() ?: -90.0
            val minLng = call.request.queryParameters["minLng"]?.toDoubleOrNull() ?: -180.0
            val maxLat = call.request.queryParameters["maxLat"]?.toDoubleOrNull() ?: 90.0
            val maxLng = call.request.queryParameters["maxLng"]?.toDoubleOrNull() ?: 180.0
            val status = call.request.queryParameters["status"]
            val category = call.request.queryParameters["category"]
            val cursor = call.request.queryParameters["cursor"]

            val response = listPublicReportsUseCase.execute(
                minLat, minLng, maxLat, maxLng, status, category, cursor
            )
            call.respond(HttpStatusCode.OK, response)
        }

        post {
            var title = ""
            var description = ""
            var categorySlug = ""
            var lat = 0.0
            var lng = 0.0
            var mode = SubmissionMode.ANONYMOUS

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "title" -> title = part.value
                            "description" -> description = part.value
                            "categorySlug" -> categorySlug = part.value
                            "latitude" -> lat = part.value.toDoubleOrNull() ?: 0.0
                            "longitude" -> lng = part.value.toDoubleOrNull() ?: 0.0
                            "submissionMode" -> mode = try {
                                SubmissionMode.valueOf(part.value.uppercase())
                            } catch (_: IllegalArgumentException) {
                                SubmissionMode.ANONYMOUS
                            }
                        }
                    }
                    is PartData.FileItem -> {
                        // TODO: Handle photo upload
                    }
                    else -> {}
                }
                part.dispose()
            }

            val userId = call.principal<UserIdPrincipal>()?.name
            val response = createReportUseCase.execute(
                title, description, categorySlug, lat, lng, mode, userId
            )
            call.respond(HttpStatusCode.Created, response)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("ID is required")
            val response = getPublicReportUseCase.execute(id)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    post("/report-status") {
        val request = call.receive<TrackingCodeRequest>()
        val response = getReportByTrackingCodeUseCase.execute(request.trackingCode)
        call.respond(HttpStatusCode.OK, response)
    }
}
