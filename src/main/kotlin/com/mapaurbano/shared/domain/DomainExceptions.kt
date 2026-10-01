package com.mapaurbano.shared.domain

/**
 * Jerarquía sellada de errores de negocio.
 * StatusPages los convierte en respuestas HTTP con ApiErrorEnvelope.
 */
sealed class DomainException(
    val errorCode: String,
    override val message: String,
    val details: List<FieldError> = emptyList(),
) : RuntimeException(message)

data class FieldError(
    val field: String? = null,
    val reason: String,
)

/** Recurso no encontrado → 404 */
class NotFoundException(
    message: String = "Recurso no encontrado.",
    errorCode: String = "NOT_FOUND",
) : DomainException(errorCode, message)

/** Datos inválidos → 400 */
class ValidationException(
    message: String = "Revisa los datos enviados.",
    details: List<FieldError> = emptyList(),
) : DomainException("VALIDATION_ERROR", message, details)

/** Conflicto de versión o regla de unicidad → 409 */
class ConflictException(
    message: String = "La operación entra en conflicto con el estado actual.",
    errorCode: String = "CONFLICT",
) : DomainException(errorCode, message)

/** Credenciales inválidas o sesión expirada → 401 */
class AuthenticationException(
    message: String = "Credenciales inválidas o sesión expirada.",
) : DomainException("AUTHENTICATION_REQUIRED", message)

/** Permisos insuficientes → 403 */
class AuthorizationException(
    message: String = "No tenés permiso para realizar esta acción.",
) : DomainException("FORBIDDEN", message)
