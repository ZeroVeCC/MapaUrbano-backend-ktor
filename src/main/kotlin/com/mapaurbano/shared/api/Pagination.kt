package com.mapaurbano.shared.api

import kotlinx.serialization.Serializable

/**
 * Respuesta paginada genérica con cursor.
 * El cursor es opaco para el cliente; el backend decide su formato interno.
 */
@Serializable
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)
