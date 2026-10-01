package com.mapaurbano.media.application

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import java.io.File
import java.util.UUID

class StorageService {
    private val uploadDir = File("uploads")

    init {
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }
    }

    suspend fun saveImage(fileItem: PartData.FileItem): String {
        val originalFileName = fileItem.originalFileName ?: "unknown.jpg"
        val extension = if (originalFileName.contains(".")) originalFileName.substringAfterLast(".") else "jpg"
        val uniqueName = "${UUID.randomUUID()}."
        val file = File(uploadDir, uniqueName)

        fileItem.streamProvider().use { input ->
            file.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }
        
        return "/uploads/$uniqueName"
    }
}
