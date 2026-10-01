package com.mapaurbano.media.application

import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.shared.domain.ValidationException
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class ProcessImageUseCase(
    private val imageRepository: ImageRepository
) {
    suspend fun execute(
        reportId: String,
        data: ByteArray,
        contentType: String,
        originalFilename: String?
    ): ReportImage {
        // Validación básica
        if (contentType !in listOf("image/jpeg", "image/png", "image/webp")) {
            throw ValidationException(message = "Formato de imagen no soportado")
        }

        if (data.size > 5 * 1024 * 1024) { // 5 MB max
            throw ValidationException(message = "La imagen no puede pesar más de 5MB")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val checksumSha256 = digest.digest(data).joinToString("") { "%02x".format(it) }

        // TODO: Validate real mime type from bytes, resize if necessary, check max dimensions
        // For now, assume it's valid and get dimensions as dummy
        val width = 800
        val height = 600

        val reportImage = ReportImage(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            data = data,
            contentType = contentType,
            sizeBytes = data.size.toLong(),
            widthPx = width,
            heightPx = height,
            originalFilename = originalFilename,
            checksumSha256 = checksumSha256,
            createdAt = Instant.now()
        )

        return imageRepository.create(reportImage)
    }
}
