# Auditoría web y cierre de pendientes backend — 19 de octubre de 2026

## Alcance y regla de continuidad

Esta revisión usa únicamente los tres repositorios vigentes de Mapa Urbano. El
monorepo original queda excluido salvo pedido expreso. En este corte se volvió a
inspeccionar en profundidad el panel web y se modificó solamente el backend Ktor.
Android no tuvo una auditoría nueva porque no forma parte del pedido puntual.

Referencias verificadas:

| Componente | Rama | Revisión |
|---|---|---|
| Backend Ktor | `main` | `49e3a52fbb76ffe4ba443557d1d03baee8f87691` |
| Android | `main` | `e1035852acfe98c46a629440eccc06d927fd7967` (referencia previa vigente) |
| Web Angular | `main` | `4101b24f0c1754fe94f3c3bea6f3373b3eb39699` |

Los cambios backend descriptos aquí se prepararon sobre la rama
`codex/auditoria-web-ajustes-backend`, basada en `main`, para revisión antes de
fusionarlos. No se modificó el repositorio web.

## Cambios observados en web

Web avanzó desde `fced3eaaa20edde4296a376ec2ec98384cb60b29` hasta
`4101b24f0c1754fe94f3c3bea6f3373b3eb39699` mediante:

1. `f525573`: cambios de distribución y tamaño del dashboard;
2. `409e528`: tres líneas en README que declaran integración completa;
3. `4101b24`: fusión del PR de implementación.

El código de contrato no cambió en este delta. Los ajustes nuevos son visuales y
no resuelven los desacoples funcionales ya informados.

Validación independiente sobre una copia temporal de solo lectura:

- Node 24.19;
- 7 suites y 8 pruebas correctas;
- build de producción correcto;
- bundle inicial de 363,84 kB, estimado transferido 88,14 kB;
- última ejecución pública de GitHub Actions `39334403684`: falló en pruebas y
  omitió el build.

La causa operativa del CI sigue siendo el workflow con Node 20. El lock actual
resuelve Angular 22.2.1 y herramientas que requieren Node
`^22.22.3 || ^24.15.0 || >=26`. El proyecto funciona localmente con Node 24, por
lo que el fallo remoto no demuestra un defecto del código de aplicación, pero sí
deja el repositorio sin validación automática confiable.

## Trabajo que todavía corresponde al equipo web

### W01 — reparar CI

- Cambiar `.github/workflows/ci.yml` a Node 24 LTS o, como mínimo, 22.22.3.
- Mantener `npm ci`, `npm test -- --watch=false` y build de producción.
- Actualizar acciones que GitHub marque por runtimes obsoletos.
- No dar la tarea por terminada hasta obtener una ejecución verde en `main`.

### W02 — separar los contratos de reportes

Definir tipos distintos para `ReportAdminSummary`, `ReportAdminDetail` y
`ReportMutationResponse`. Asignar y desasignar responden HTTP 204 sin cuerpo.
El listado no contiene descripción, IDs completos de asignación ni modo de envío:
la interfaz debe pedir el detalle al abrir el modal. Una mutación parcial no debe
reemplazar el objeto completo del listado.

### W03 — conectar detalle, imagen y operaciones administrativas

- Usar `GET /api/v1/admin/reports/{id}` al abrir un reporte.
- Descargar `/api/v1/admin/reports/{id}/image` como Blob solo si
  `hasImage=true`; crear una URL local y revocarla al cerrar o cambiar de imagen.
- Retirar `/bache.jpg`, porque puede mostrar evidencia ajena al reporte.
- Enviar realmente asignación, desasignación, prioridad y `dueAt`.
- Cargar responsables desde `/api/v1/admin/assignees`.
- Tras un 204 o una respuesta parcial, recargar detalle/listado o fusionar sólo
  los campos devueltos.
- Ante 409, informar el conflicto y consultar la versión vigente.

### W04 — corregir equipos, estadísticas y auditoría

- Equipo backend: `description`, `memberCount`, `createdAt`; web aún usa
  `specialty` y `membersCount`.
- Estadísticas: `byCategory` y `byTeam` son arreglos; `byStatus` y `byPriority`
  usan claves enum en mayúsculas.
- Auditoría: usar `actorAdminUserId`, `actorUserId`, `action`, `entityType`,
  `entityId`, `metadata`, `sourceIp` y `occurredAt`.
- Eliminar equipos ficticios y fallbacks que convierten un error HTTP en éxito.

### W05 — corregir categorías y errores API

- `CreateCategoryRequest` no admite `isActive`; no enviarlo al crear.
- Leer errores con `{ "error": { "code", "message", "details", "requestId" } }`.
- Diferenciar 400, 401, 403, 404, 409, 429 y 5xx.
- En 401 limpiar sesión y datos privados.
- En 403 por CSRF restaurar sesión/token una vez y reintentar sin bucles.
- No convertir fallos de logout o restauración en éxitos silenciosos.

### W06 — completar sesión, rutas y tiempo real

- Impedir que un administrador autenticado vuelva a `/login`.
- Conectar el WebSocket una vez por sesión y cerrarlo en logout.
- Al reconectar, recargar por REST porque el bus no conserva eventos.
- Manejar creación, actualización, prioridad, asignación y eliminación; usar
  retroceso acotado entre reintentos.
- Limpiar caches y marcadores del mapa al cerrar sesión.

### W07 — completar paginación, filtros y mapa

- Consumir `nextCursor` y anexar páginas sin duplicar registros.
- Agregar filtros de categoría, prioridad, equipo y responsable.
- No sobrescribir el listado al pedir la siguiente página.
- No validar coordenadas con `if (!latitude || !longitude)`: cero es válido;
  validar finitud y rangos.
- Actualizar y retirar marcadores cuando cambie el conjunto de reportes.
- No limitar el dashboard permanentemente a los primeros 50 registros.

### W08 — pruebas y documentación

- Agregar pruebas HTTP de URLs, requests, responses, 204, paginación y 409.
- Probar interceptor, cookie, CSRF, guard y limpieza de sesión.
- Agregar un E2E de login, listado, detalle, imagen y mutaciones.
- Documentar instalación, versión de Node, backend y validación real.
- Retirar afirmaciones de integración, TLS, cifrado o certificación que no estén
  respaldadas por evidencia del entorno desplegado.

## Correcciones realizadas en backend

### Seguridad administrativa y WebSocket

- Cookie administrativa con `Path=/api/v1` para REST y `/api/v1/ws/admin`.
- Evento `connected` inmediato después del handshake.
- Comprobación en cada solicitud de que el administrador siga activo.
- Integración con login, cookies, WebSocket HTTP 101 y cuenta desactivada.

### Persistencia atómica de reporte e imagen

- La imagen se prepara y valida antes de escribir.
- Reporte e imagen se insertan en una sola transacción.
- `report.created` se publica únicamente después del commit.
- Si la imagen falla, se responde `REPORT_PERSISTENCE_ERROR` y se revierte también
  el reporte. Una integración reproduce y comprueba esa reversión.

### Cursores estables

- Listado propio, administrativo y auditoría ordenan por fecha y UUID descendente.
- El cursor nuevo es opaco, URL-safe y contiene ambos valores.
- Se aceptan temporalmente cursores ISO-8601 antiguos durante el despliegue.
- Una integración con fechas idénticas comprueba que no desaparezcan filas.

### Validaciones administrativas

- UUID inválido en filtros y cursor inválido responden 400.
- Equipos validan nombre, máximo 120 y duplicados con 409.
- Sólo administradores existentes y activos pueden agregarse a un equipo.
- La inserción idempotente de miembros ya no oculta errores de base.
- Categorías validan slug, nombre y color con los límites documentados.
- OpenAPI declara los 400 y límites corregidos.

## Evidencia de aceptación backend

Entorno: JDK 21.0.10, Gradle Wrapper 9.5.0, Docker Desktop y Testcontainers con
`postgis/postgis:16-3.5`.

- `gradlew test --no-daemon`: correcto, 41 pruebas.
- `gradlew integrationTest --rerun-tasks --no-daemon`: correcto, 19 pruebas.
- HTTP integrado: login, cookie, sesión, CSRF, WebSocket 101, consulta, mutación,
  asignación, conflicto 409, logout y desactivación.
- Datos integrados: PostGIS, reporte/imagen atómicos y cursores con empates.

## Estado de integración

Después de estas correcciones no queda un bloqueo funcional conocido en backend
para que web implemente el contrato publicado. Quedan como trabajo conjunto el
alojamiento, HTTPS/WSS, secretos, observabilidad, backup y el E2E final.

Web todavía no puede considerarse terminada aunque sus pruebas locales y build
pasen: varias pantallas compilan contra modelos TypeScript que no representan las
respuestas reales. El orden recomendado es CI, DTO, detalle/imagen, mutaciones,
equipos/estadísticas/auditoría, WebSocket y E2E.
