# Cierre del backend y plan de adecuación web — 18 de octubre de 2026

## Alcance y referencias

Este documento registra el cierre de la etapa de backend solicitada después del
reescaneo de privacidad. Solo se modificó `ZeroVeCC/MapaUrbano-backend-ktor`.
Android, web y el monorepo se mantuvieron como referencias de lectura.

La última revisión web documentada corresponde a
`Santy112/MapaUrbano-frontend-web` `main` en
`85832a4183d6d1fde96b49161bee1142445f3cc6`. El análisis detallado previo está en
`docs/REESCANEO_WEB_2026-10-18.md`.

## Contrato de seguridad definitivo

### Vecinos

- Registro e inicio de sesión por DNI y contraseña.
- `POST /api/v1/reports` exige Bearer y solo acepta `ACCOUNT`.
- Se retiraron el listado global ciudadano, el detalle y la imagen públicos, el
  seguimiento anónimo y el WebSocket público.
- Las consultas se realizan por `/api/v1/users/me/reports`, su detalle y su imagen.
- Detalle e imagen devuelven 404 si el reporte no pertenece a la cuenta, para no
  revelar su existencia.

### Administradores

- `POST /api/v1/admin/auth/login` es público y crea una sesión persistida en
  PostgreSQL.
- La sesión viaja en la cookie HttpOnly `mapa_urbano_admin`, con `SameSite=Lax` y
  opción `Secure` por entorno.
- Login y `/admin/auth/me` entregan `csrfToken`.
- Toda mutación administrativa exige `X-CSRF-Token`.
- CORS permite credenciales y se parametriza con `CORS_ALLOWED_HOSTS`.
- Registro y login ciudadano, y login administrativo, tienen límite específico de
  diez solicitudes por minuto, además del límite global.

## Funcionalidad backend terminada en esta etapa

1. Persistencia de sesiones administrativas mediante la migración
   `V4__admin_sessions.sql`, revocación al cerrar sesión y validación de vencimiento.
2. Bootstrap opcional del primer administrador mediante variables de entorno.
3. Listado administrativo con estado, categoría, prioridad, equipo, responsable,
   texto, cursor y límite; devuelve total real.
4. Detalle administrativo con coordenadas, imagen, asignación e historiales de
   estado y prioridad.
5. Descarga administrativa de imágenes.
6. Cambio de estado y prioridad con respuesta estable, historial, auditoría,
   control optimista y eventos WebSocket.
7. Asignación y desasignación atómicas: la misma transacción incrementa la versión,
   cierra la asignación previa y crea la nueva. Se validan equipo y responsable.
8. Listado y detalle de equipos, miembros y responsables posibles.
9. Estadísticas calculadas desde reportes activos y asignaciones reales.
10. Auditoría paginada. Se corrigió el mapeo Exposed de `jsonb`, descubierto por la
    prueba HTTP real.
11. Readiness que ejecuta `SELECT 1` y responde 503 cuando PostgreSQL no está listo.
12. Validación de evidencia por firma JPEG/PNG/WebP, tamaño máximo y dimensiones
    reales antes de crear el reporte.
13. Swagger alineado con las rutas reales y sin presentar endpoints 501 como
    disponibles.
14. Retiro de los casos de uso residuales de listado público, detalle público y
    seguimiento anónimo. La persistencia conserva compatibilidad de lectura con
    filas anónimas históricas para que sigan visibles únicamente en administración.

## Evidencia ejecutada

- `./gradlew.bat test --rerun-tasks`: correcto.
- `./gradlew.bat integrationTest --rerun-tasks`: correcto con Docker,
  PostgreSQL/PostGIS y Testcontainers.
- La integración cubre migraciones, restricciones, escritura y lectura de
  coordenadas tras reconstruir repositorios, listado y detalle ciudadano y
  administrativo, estadísticas, sesiones administrativas, cookie, CSRF, cambio de
  estado, auditoría y logout.
- El contrato OpenAPI se parsea como YAML, resuelve todas sus referencias y coincide
  exactamente con el conjunto de rutas registradas.

## Plan que debe ejecutar el equipo web

### Prioridad 1 — sesión administrativa real

1. Eliminar el bypass de login y cualquier usuario simulado.
2. Configurar el cliente HTTP con `withCredentials: true` para todas las llamadas
   `/api/v1/admin/**` y para el handshake WebSocket.
3. Iniciar sesión con `POST /api/v1/admin/auth/login` y guardar `csrfToken` solo en
   memoria de la aplicación.
4. Restaurar sesión con `GET /api/v1/admin/auth/me` al recargar.
5. Enviar `X-CSRF-Token` en logout y en cada POST, PUT, PATCH o DELETE admin.
6. Ante 401, limpiar el estado y volver al login; ante 403 CSRF, intentar restaurar
   `/auth/me` una vez y no simular éxito.

### Prioridad 2 — modelos y paginación

1. Usar UUID como `string` en todos los modelos.
2. Estados válidos: `PENDING`, `IN_PROGRESS`, `RESOLVED`.
3. Prioridades válidas: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
4. Categorías usan `sortOrder`, no `displayOrder`.
5. Reemplazar paginación por páginas por `{items,nextCursor,totalCount}` y reenviar
   `nextCursor` como valor opaco.
6. Mantener `version` del reporte y enviarlo en estado, prioridad y asignación.
7. Al recibir 409, recargar el detalle y avisar que otro administrador lo modificó.

### Prioridad 3 — reportes reales

1. Cargar la tabla con `GET /api/v1/admin/reports` y sus filtros reales:
   `status`, `categorySlug`, `priority`, `teamId`,
   `responsibleAdminUserId`, `search`, `cursor`, `limit`.
2. Abrir cada caso mediante `GET /api/v1/admin/reports/{id}`; no reutilizar el
   resumen de la tabla como detalle.
3. Mostrar evidencia desde `/api/v1/admin/reports/{id}/image` solo cuando
   `hasImage=true`, usando la cookie de sesión.
4. Conectar `PATCH .../status`, `PATCH .../priority`, `PUT/DELETE .../assignment` y
   `DELETE .../reports/{id}`. No actualizar la interfaz si el servidor falla.

### Prioridad 4 — catálogos, equipos, tablero y auditoría

1. Reemplazar arreglos locales por `/admin/categories`, `/admin/teams` y
   `/admin/assignees`.
2. Usar `/admin/statistics` para el tablero; retirar números inventados.
3. Implementar la vista de `/admin/audit` con cursor.
4. Conectar `/api/v1/ws/admin` después de cargar el estado por REST. Al reconectar,
   volver a consultar REST porque el bus no conserva replay.

### Prioridad 5 — errores, pruebas y CI

1. Retirar fallbacks que devuelven datos mock o simulan una mutación exitosa.
2. Distinguir 400, 401, 403, 404, 409, 429 y 500 usando `error.code`,
   `error.message` y `requestId`.
3. Corregir el workflow: el proyecto usa Vitest sin proveedor de navegador, por lo
   que `ng test --watch=false --browsers=ChromeHeadless` falla en GitHub Actions.
   Usar el comando Vitest compatible o configurar explícitamente el proveedor.
4. Agregar pruebas de login/cookie/CSRF, restauración, 401, 403, 409, filtros,
   detalle, imagen, mutaciones y WebSocket.
5. Añadir un E2E contra Ktor/PostGIS que recorra login, listado, detalle, cambio de
   estado, asignación y logout.

## Criterio de aceptación conjunto

Con dos ciudadanos y un administrador:

- cada ciudadano ve exclusivamente sus reportes y evidencias;
- el administrador ve ambos desde el panel;
- ninguna mutación funciona sin cookie y CSRF;
- un conflicto de versión produce 409 y recarga;
- el panel no contiene mocks ni éxito local cuando la API falla;
- build, pruebas y GitHub Actions quedan verdes en backend y web.
