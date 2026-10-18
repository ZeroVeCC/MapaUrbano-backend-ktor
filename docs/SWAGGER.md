# Swagger y contrato OpenAPI

La documentación interactiva se publica en `http://localhost:8080/swagger` y su
fuente versionada es `src/main/resources/openapi/documentation.yaml` (OpenAPI
3.0.3). El contrato enumera solamente rutas registradas e implementadas.

## Vecinos: DNI, Bearer y reportes propios

1. Ejecutar `POST /api/v1/users/register` o `POST /api/v1/users/login`.
2. Copiar `token`, pulsar **Authorize** y pegar solamente ese valor. Swagger agrega
   `Bearer`.
3. Crear con `POST /api/v1/reports` y consultar con
   `GET /api/v1/users/me/reports` y `/users/me/reports/{id}`.

Los reportes requieren una cuenta. Ya no existen listado global ciudadano,
detalle público, imagen pública, alta anónima ni seguimiento por código. El
backend comprueba pertenencia tanto para el detalle como para la imagen.

`POST /api/v1/reports` usa `multipart/form-data`:

| Parte | Tipo | Obligatoria | Regla |
|---|---|---:|---|
| `title` | texto | sí | no vacío, máximo de base 150 |
| `description` | texto | sí | no vacío |
| `categorySlug` | texto | sí | categoría existente |
| `latitude` | número | sí | -90 a 90 |
| `longitude` | número | sí | -180 a 180 |
| `submissionMode` | texto | no | si se envía, solo `ACCOUNT` |
| `image` | archivo | no | JPEG, PNG o WebP; máximo 5 MB y 12000 px por lado |

La imagen se inspecciona por firma binaria y dimensiones antes de crear el
reporte; no se confía únicamente en el `Content-Type`.

## Administradores: cookie y CSRF

El primer administrador puede crearse al arrancar con
`ADMIN_BOOTSTRAP_USERNAME` y `ADMIN_BOOTSTRAP_PASSWORD`. La contraseña inicial
debe ocupar entre 12 y 72 bytes UTF-8.

1. Ejecutar `POST /api/v1/admin/auth/login`.
2. El servidor emite la cookie HttpOnly `mapa_urbano_admin`; el navegador la
   conserva si la llamada usa credenciales.
3. Guardar `csrfToken` de la respuesta. También puede recuperarse con
   `GET /api/v1/admin/auth/me`.
4. Enviar `X-CSRF-Token` en POST, PUT, PATCH y DELETE administrativos.

El panel dispone de listado, búsqueda y detalle global de reportes; evidencia;
estado, prioridad, fecha objetivo, asignación y baja; equipos y responsables;
categorías; estadísticas; auditoría y `/api/v1/ws/admin`.

Para desarrollo local la cookie puede viajar por HTTP. En producción debe usarse
`SESSION_SECURE_COOKIE=true`, HTTPS/WSS, un `SESSION_SECRET` aleatorio de al menos
32 caracteres y `CORS_ALLOWED_HOSTS` con los hosts exactos del panel.

## Paginación y concurrencia

Los listados usan `items` y `nextCursor`; el cliente debe reenviar el cursor sin
interpretarlo. Las mutaciones de reporte reciben `version`. Un `409` indica que
otro administrador modificó el reporte y el panel debe recargar el detalle antes
de reintentar.

Swagger UI documenta el handshake del WebSocket, pero no abre el canal. El cliente
debe cargar primero el estado por REST y usar WebSocket para novedades posteriores.
El bus de eventos es local al proceso y no tiene replay.

Toda modificación de ruta, DTO, enum o parte multipart debe actualizar OpenAPI y
`OpenApiContractTest` en el mismo commit.
