package com.mapaurbano.media.application

import com.mapaurbano.media.domain.ImageRepository
import com.mapaurbano.media.domain.ReportImage
import com.mapaurbano.shared.domain.ValidationException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessImageUseCaseTest {
    private class Images : ImageRepository {
        var saved: ReportImage? = null
        override suspend fun findByReportId(reportId: String) = saved?.takeIf { it.reportId == reportId }
        override suspend fun create(image: ReportImage) = image.also { saved = it }
        override suspend fun deleteByReportId(reportId: String) = (saved?.reportId == reportId).also { if (it) saved = null }
    }

    @Test
    fun `detecta firma y conserva dimensiones reales de PNG`(): Unit = runBlocking {
        val repository = Images()
        val useCase = ProcessImageUseCase(repository)
        val bytes = image("png", 37, 19)

        val result = useCase.execute("report-1", bytes, "image/png", "evidencia.png")

        assertEquals(37, result.widthPx)
        assertEquals(19, result.heightPx)
        assertEquals(bytes.size.toLong(), result.sizeBytes)
        assertEquals(result, repository.saved)
    }

    @Test
    fun `rechaza contenido falso tipo declarado incorrecto y exceso de tamaño`() {
        val useCase = ProcessImageUseCase(Images())
        assertThrows<ValidationException> { useCase.inspect("no es imagen".toByteArray(), "image/png") }
        assertThrows<ValidationException> { useCase.inspect(image("png", 2, 2), "image/jpeg") }
        assertThrows<ValidationException> {
            useCase.inspect(ByteArray(ProcessImageUseCase.MAX_IMAGE_BYTES + 1), "image/png")
        }
    }

    private fun image(format: String, width: Int, height: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output)
            output.toByteArray()
        }
}
