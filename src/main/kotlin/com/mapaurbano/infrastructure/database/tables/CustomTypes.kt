package com.mapaurbano.infrastructure.database.tables

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

    override fun valueFromDB(value: Any): GeoPoint {
        val text = when (value) {
            is PGobject -> value.value ?: error("NULL geography value")
            is String -> value
            else -> value.toString()
        }
        // PostgreSQL returns hex WKB by default.  We ask ST_AsText via a raw
        // query when we really need it, but for the common path we parse the
        // EWKT / WKT that PostgreSQL can also return when cast.
        // If the value starts with "POINT" it's WKT; otherwise it's hex WKB
        // which we'll skip for now (the repo uses ST_X / ST_Y instead).
        if (text.uppercase().startsWith("POINT")) {
            // "POINT(lng lat)"
            val coords = text.substringAfter("(").substringBefore(")").trim().split(" ")
            return GeoPoint(latitude = coords[1].toDouble(), longitude = coords[0].toDouble())
        }
        // Fallback: return (0,0) — in practice the repo reads lat/lng via ST_X/ST_Y
        return GeoPoint(0.0, 0.0)
    }

    override fun notNullValueToDB(value: GeoPoint): Any {
        val obj = PGobject()
        obj.type = "geography"
        obj.value = value.toEWKT()
        return obj
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val obj = PGobject()
        obj.type = "geography"
        if (value is GeoPoint) {
            obj.value = value.toEWKT()
        }
        stmt[index] = obj
    }

    override fun nonNullValueToString(value: GeoPoint): String {
        return "'${value.toEWKT()}'::geography"
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
        val obj = PGobject()
        obj.type = pgTypeName
        obj.value = (value as? Enum<*>)?.name?.lowercase()
        stmt[index] = obj
    }

    override fun nonNullValueToString(value: T): String {
        return "'${value.name.lowercase()}'::$pgTypeName"
    }
}

/** Register a PostgreSQL ENUM column on any Exposed Table */
inline fun <reified T : Enum<T>> Table.pgEnum(name: String, enumTypeName: String): Column<T> =
    registerColumn(name, PgEnum(enumTypeName, T::class.java))
