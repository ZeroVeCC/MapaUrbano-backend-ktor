package com.mapaurbano.media.application

import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.shared.domain.ValidationException
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

data class ImageMetadata(val width: Int, val height: Int)

class ProcessImageUseCase(
    private val imageRepository: ImageRepository
) {
    suspend fun execute(
        reportId: String,
        data: ByteArray,
        contentType: String,
        originalFilename: String?
    ): ReportImage {
        val metadata = inspect(data, contentType)

        val digest = MessageDigest.getInstance("SHA-256")
        val checksumSha256 = digest.digest(data).joinToString("") { "%02x".format(it) }

        val reportImage = ReportImage(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            data = data,
            contentType = contentType,
            sizeBytes = data.size.toLong(),
            widthPx = metadata.width,
            heightPx = metadata.height,
            originalFilename = originalFilename,
            checksumSha256 = checksumSha256,
            createdAt = Instant.now()
        )

        return imageRepository.create(reportImage)
    }

    fun inspect(data: ByteArray, contentType: String): ImageMetadata {
        if (data.isEmpty() || data.size > MAX_IMAGE_BYTES) {
            throw ValidationException(message = "La imagen debe pesar entre 1 byte y 5 MB")
        }
        val detected = detectContentType(data)
            ?: throw ValidationException(message = "El archivo no contiene una imagen JPEG, PNG o WebP válida")
        if (detected != contentType.lowercase()) {
            throw ValidationException(message = "El contenido de la imagen no coincide con su Content-Type")
        }
        val metadata = when (detected) {
            "image/webp" -> readWebpDimensions(data)
            else -> ImageIO.read(ByteArrayInputStream(data))?.let { ImageMetadata(it.width, it.height) }
        } ?: throw ValidationException(message = "No se pudieron leer las dimensiones de la imagen")
        if (metadata.width !in 1..MAX_IMAGE_DIMENSION || metadata.height !in 1..MAX_IMAGE_DIMENSION) {
            throw ValidationException(message = "La imagen no puede superar ${MAX_IMAGE_DIMENSION}x${MAX_IMAGE_DIMENSION} píxeles")
        }
        return metadata
    }

    private fun detectContentType(data: ByteArray): String? = when {
        data.size >= 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte() -> "image/jpeg"
        data.size >= 8 && data.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE) -> "image/png"
        data.size >= 12 && data.ascii(0, 4) == "RIFF" && data.ascii(8, 12) == "WEBP" -> "image/webp"
        else -> null
    }

    private fun readWebpDimensions(data: ByteArray): ImageMetadata? {
        if (data.size < 30) return null
        return when (data.ascii(12, 16)) {
            "VP8X" -> ImageMetadata(1 + data.uint24Le(24), 1 + data.uint24Le(27))
            "VP8L" -> {
                if (data.size < 25 || data[20] != 0x2F.toByte()) return null
                val bits = data[21].toInt() and 0xFF or
                    ((data[22].toInt() and 0xFF) shl 8) or
                    ((data[23].toInt() and 0xFF) shl 16) or
                    ((data[24].toInt() and 0xFF) shl 24)
                ImageMetadata((bits and 0x3FFF) + 1, ((bits shr 14) and 0x3FFF) + 1)
            }
            "VP8 " -> {
                if (data.size < 30 || data[23] != 0x9D.toByte() || data[24] != 0x01.toByte() || data[25] != 0x2A.toByte()) return null
                ImageMetadata(data.uint16Le(26) and 0x3FFF, data.uint16Le(28) and 0x3FFF)
            }
            else -> null
        }
    }

    private fun ByteArray.ascii(from: Int, to: Int) = copyOfRange(from, to).toString(Charsets.US_ASCII)
    private fun ByteArray.uint16Le(offset: Int) = (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)
    private fun ByteArray.uint24Le(offset: Int) = uint16Le(offset) or ((this[offset + 2].toInt() and 0xFF) shl 16)

    companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        const val MAX_IMAGE_DIMENSION = 12_000
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
