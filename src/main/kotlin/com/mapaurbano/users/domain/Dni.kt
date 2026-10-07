package com.mapaurbano.users.domain

/** Formato del MVP: 7 u 8 dígitos ASCII, opcionalmente agrupados con puntos. */
object Dni {
    fun normalize(value: String): String? {
        val trimmed = value.trim()
        if (!Regex("(?:[0-9]{7,8}|[0-9]{1,2}\\.[0-9]{3}\\.[0-9]{3})").matches(trimmed)) return null
        return trimmed.replace(".", "").takeUnless { it.all { digit -> digit == '0' } }
    }
}
