package com.mapaurbano.infrastructure.database.tables

import com.mapaurbano.shared.domain.PersistenceException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

/**
 * Simple latitude/longitude holder used by the Exposed column type.
 * No dependency on postgis-jdbc; we speak WKT directly to PostgreSQL.
 */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    /** WKT with SRID for insert into geography(Point, 4326) */
    fun toEWKT(): String = "SRID=4326;POINT($longitude $latitude)"
}

/**
 * Exposed column type that maps to PostgreSQL `geography(Point, 4326)`.
 * Sends data as EWKT text wrapped in a PGobject, reads back the hex WKB
 * and decodes lat/lng from it.
 */
class GeographyPointColumnType : ColumnType<GeoPoint>() {
    override fun sqlType(): String = "geography(Point, 4326)"

    override fun valueFromDB(value: Any): GeoPoint = try {
        when (value) {
            is GeoPoint -> value
            is PGobject -> decodeText(value.value ?: error("NULL geography value"))
            is String -> decodeText(value)
            is ByteArray -> decodeWkb(value)
            else -> error("Unsupported geography value type: ${value::class.qualifiedName}")
        }
    } catch (cause: PersistenceException) {
        throw cause
    } catch (cause: Exception) {
        throw PersistenceException(
            message = "No pudimos interpretar la ubicación guardada.",
            errorCode = "LOCATION_DECODING_ERROR",
            cause = cause,
        )
    }

    private fun decodeText(raw: String): GeoPoint {
        val text = raw.trim()
        val wkt = if (text.startsWith("SRID=", ignoreCase = true)) {
            val separator = text.indexOf(';')
            require(separator > 5) { "Invalid EWKT point" }
            require(text.substring(5, separator).toInt() == 4326) { "Unexpected geography SRID" }
            text.substring(separator + 1)
        } else {
            text
        }

        POINT_PATTERN.matchEntire(wkt)?.let { match ->
            return checkedPoint(
                longitude = match.groupValues[1].toDouble(),
                latitude = match.groupValues[2].toDouble(),
            )
        }

        return decodeWkb(decodeHex(text))
    }

    private fun decodeWkb(bytes: ByteArray): GeoPoint {
        require(bytes.size >= WKB_POINT_SIZE) { "WKB point is truncated" }
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(
            when (buffer.get().toInt()) {
                0 -> ByteOrder.BIG_ENDIAN
                1 -> ByteOrder.LITTLE_ENDIAN
                else -> error("Invalid WKB byte order")
            },
        )

        val typeWord = buffer.int.toLong() and UINT_MASK
        val geometryType = (typeWord and TYPE_MASK).toInt()
        require(geometryType % ISO_DIMENSION_OFFSET == WKB_POINT_TYPE) { "WKB geometry is not a point" }

        if (typeWord and EWKB_SRID_FLAG != 0L) {
            require(buffer.remaining() >= Int.SIZE_BYTES + 2 * Double.SIZE_BYTES) { "EWKB point is truncated" }
            require(buffer.int == 4326) { "Unexpected geography SRID" }
        } else {
            require(buffer.remaining() >= 2 * Double.SIZE_BYTES) { "WKB point is truncated" }
        }

        val longitude = buffer.double
        val latitude = buffer.double
        return checkedPoint(latitude = latitude, longitude = longitude)
    }

    private fun decodeHex(raw: String): ByteArray {
        val hex = raw.removePrefix("\\x").removePrefix("0x")
        require(hex.length % 2 == 0 && hex.matches(HEX_PATTERN)) { "Invalid hexadecimal WKB" }
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun checkedPoint(latitude: Double, longitude: Double): GeoPoint {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid point latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid point longitude" }
        return GeoPoint(latitude = latitude, longitude = longitude)
    }

    override fun notNullValueToDB(value: GeoPoint): Any {
        val obj = PGobject()
        obj.type = "geography"
        obj.value = value.toEWKT()
        return obj
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value == null) {
            super.setParameter(stmt, index, null)
            return
        }
        val parameter = when (value) {
            is PGobject -> value
            is GeoPoint -> notNullValueToDB(value)
            else -> error("Unsupported geography parameter: ${value::class.qualifiedName}")
        }
        stmt[index] = parameter
    }

    override fun nonNullValueToString(value: GeoPoint): String {
        return "'${value.toEWKT()}'::geography"
    }

    private companion object {
        private const val WKB_POINT_SIZE = 1 + Int.SIZE_BYTES + 2 * Double.SIZE_BYTES
        private const val WKB_POINT_TYPE = 1
        private const val ISO_DIMENSION_OFFSET = 1000
        private const val UINT_MASK = 0xFFFF_FFFFL
        private const val TYPE_MASK = 0x0FFF_FFFFL
        private const val EWKB_SRID_FLAG = 0x2000_0000L
        private val HEX_PATTERN = Regex("[0-9a-fA-F]+")
        private val POINT_PATTERN = Regex(
            """POINT(?:\s+(?:Z|M|ZM))?\s*\(\s*([-+]?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?)\s+([-+]?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?)(?:\s+[-+]?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?){0,2}\s*\)""",
            RegexOption.IGNORE_CASE,
        )
    }
}

/** Register a geography(Point,4326) column on any Exposed Table */
fun Table.geoPoint(name: String): Column<GeoPoint> =
    registerColumn(name, GeographyPointColumnType())

// ─── PgEnum ──────────────────────────────────────────────────────────────

/**
 * Exposed column type for PostgreSQL custom ENUM types.
 */
class PgEnum<T : Enum<T>>(
    private val pgTypeName: String,
    private val enumClass: Class<T>
) : ColumnType<T>() {

    override fun sqlType(): String = pgTypeName

    override fun valueFromDB(value: Any): T {
        val raw = when (value) {
            is PGobject -> value.value ?: error("NULL enum value")
            is String -> value
            else -> value.toString()
        }
        return java.lang.Enum.valueOf(enumClass, raw.uppercase())
    }

    override fun notNullValueToDB(value: T): Any {
        val obj = PGobject()
        obj.type = pgTypeName
        obj.value = value.name.lowercase()
        return obj
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value == null) {
            super.setParameter(stmt, index, null)
            return
        }
        val parameter = when (value) {
            is PGobject -> value
            else -> {
                require(enumClass.isInstance(value)) {
                    "Unsupported $pgTypeName parameter: ${value::class.qualifiedName}"
                }
                @Suppress("UNCHECKED_CAST")
                notNullValueToDB(value as T)
            }
        }
        stmt[index] = parameter
    }

    override fun nonNullValueToString(value: T): String {
        return "'${value.name.lowercase()}'::$pgTypeName"
    }
}

/** Register a PostgreSQL ENUM column on any Exposed Table */
inline fun <reified T : Enum<T>> Table.pgEnum(name: String, enumTypeName: String): Column<T> =
    registerColumn(name, PgEnum(enumTypeName, T::class.java))
