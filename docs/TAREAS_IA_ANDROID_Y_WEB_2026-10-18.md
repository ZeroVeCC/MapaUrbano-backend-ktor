# Instrucciones para completar Android y web

**Fecha de corte:** 18 de octubre de 2026  
**Backend de referencia:** `ZeroVeCC/MapaUrbano-backend-ktor` `main` en
`34d219ea915ca9215e99005bcdcb098526bf4fa6`  
**Android revisado:** `Jquincal/Mapaurbano_frontend_kotlin` `main` en
`4cfc8242ef6da96eec7d97302fdf7cc0b36bea43`  
**Web revisada:** `Santy112/MapaUrbano-frontend-web` `main` en
`85832a4183d6d1fde96b49161bee1142445f3cc6`

Este archivo contiene dos instrucciones autocontenidas para entregar a una IA.
Cada IA debe trabajar únicamente en el repositorio que le corresponde. El
contrato HTTP definitivo es el OpenAPI del backend en
`src/main/resources/openapi/documentation.yaml`.

> **Actualización del 19 de octubre:** Android completó las tareas A01–A07 en los
> commits `30d301a` y `e103585`; su workflow está verde. Ese bloque se conserva
> como especificación y trazabilidad, no como lista pendiente. Solo falta su
> validación física. Web no cambió y su bloque continúa plenamente vigente. Ver
> `docs/REESCANEO_REPOSITORIOS_2026-10-19.md`.

## Prompt para la IA responsable de Android

```text
OBJETIVO

Completar la integración de la aplicación Android de Mapa Urbano con el backend
Ktor actual y garantizar que cada ciudadano vea exclusivamente sus propios
reportes. Trabajá únicamente en:
https://github.com/Jquincal/Mapaurbano_frontend_kotlin.git

PUNTO DE PARTIDA AUDITADO

- Rama: main.
- Commit auditado: 4cfc8242ef6da96eec7d97302fdf7cc0b36bea43.
- Backend de referencia: ZeroVeCC/MapaUrbano-backend-ktor, commit
  34d219ea915ca9215e99005bcdcb098526bf4fa6.
- Contrato fuente de verdad: el OpenAPI del backend en
  src/main/resources/openapi/documentation.yaml.
- No modifiques el backend ni el frontend web.
- La autenticación ciudadana usa DNI y contraseña.
- No existe creación anónima, seguimiento por código, listado público, detalle
  público, imagen pública ni WebSocket público.

TRABAJO YA REALIZADO QUE DEBÉS CONSERVAR

- La sesión se restaura antes de mostrar el mapa.
- Una persona sin sesión queda en login/registro.
- El formulario envía submissionMode=ACCOUNT.
- Se retiraron el selector anónimo y la pantalla de seguimiento.
- El mapa permanece montado cuando no hay reportes o cuando un filtro no tiene
  coincidencias.
- Los filtros fueron movidos a un menú desplegable.
- Existe diferenciación básica entre red, 400, 401, 409, 429 y 500.
- El envío repetido mientras hay una solicitud pendiente se bloquea.
- Ante un error de creación se conserva el borrador.

DEFECTOS CONFIRMADOS EN EL COMMIT AUDITADO

1. MapaUrbanoApi todavía llama GET /reports y GET /reports/{id}.
2. El backend actual retiró esas lecturas: GET /reports responde 405 y el detalle
   público ya no está registrado.
3. AppViewModel.refreshPublicContent() todavía carga reportes públicos.
4. refreshMyReports() conserva y mezcla foreignOrPublicReports con los propios.
5. refreshReportDetail() todavía usa getPublicReport().
6. Al cerrar sesión clearLocalSession() no vacía reports ni todo el estado privado.
7. UiState y FakeReportRepository siguen usando DemoContent; los datos de demo no
   deben aparecer en ejecución integrada.
8. Solo se consume la primera página de /users/me/reports.
9. La fotografía continúa siendo una simulación; no se seleccionan ni envían bytes.
10. No se descarga la imagen protegida de un reporte propio.
11. README.md, docs/backend-integration.md y docs/proximas-correcciones.md todavía
    describen rutas públicas o limitaciones del backend que ya fueron corregidas.

CONTRATO CIUDADANO VIGENTE

- GET /api/v1/categories: público; devuelve únicamente categorías activas.
- POST /api/v1/users/register: JSON {dni, displayName, password}.
- POST /api/v1/users/login: JSON {dni, password}.
- GET /api/v1/users/me: Bearer obligatorio.
- POST /api/v1/users/logout: Bearer obligatorio.
- DELETE /api/v1/users/me: Bearer obligatorio.
- POST /api/v1/reports: Bearer y multipart/form-data obligatorios.
- GET /api/v1/users/me/reports?cursor=&limit=: Bearer; devuelve items y nextCursor.
- GET /api/v1/users/me/reports/{id}: Bearer; devuelve 404 para un reporte ajeno.
- GET /api/v1/users/me/reports/{id}/image: Bearer; devuelve bytes y Content-Type,
  o 404 si no existe/no pertenece a la cuenta.

El multipart de creación admite title, description, categorySlug, latitude,
longitude, submissionMode=ACCOUNT opcional e image opcional. La imagen debe ser
JPEG, PNG o WebP, pesar como máximo 5 MB y no superar 12000 px por lado.

TAREAS OBLIGATORIAS

A01. Eliminar completamente el flujo público

- Eliminar MapaUrbanoApi.getPublicReports() y getPublicReport().
- Eliminar ReportRepository.getPublicReports() y getPublicReport().
- Eliminar refreshPublicContent() o convertirlo en una carga de categorías más
  reportes propios, sin llamadas públicas.
- El mapa y la lista deben usar exclusivamente GET /users/me/reports.
- El detalle debe usar exclusivamente GET /users/me/reports/{id}.
- No conservar lógica llamada foreignOrPublicReports.
- Actualizar nombres como ReportPublicSummary si inducen a reutilizar una ruta
  pública; los nombres locales deben expresar que son reportes propios.

A02. Aislar totalmente cada sesión

- Al logout, desactivación o 401, borrar reportes, detalle seleccionado, imágenes,
  filtros privados, mensajes derivados, back stack y cualquier caché del usuario.
- Al iniciar con otra cuenta en el mismo teléfono, no reutilizar ninguna respuesta
  de la cuenta anterior.
- Mantener el borrador únicamente en el caso explícito de sesión vencida durante
  un envío, para permitir login y reintento; no mezclarlo con datos ya enviados.
- Eliminar DemoContent de la inicialización de producción. Puede permanecer en
  previews y fakes de pruebas.

A03. Integrar listado y detalle propios

- Implementar getCurrentUserReport(id) con el DTO exacto del OpenAPI.
- Cargar historial, description, categoryName, hasImage y coordenadas reales desde
  el detalle.
- Usar categorySlug para resolver la categoría; retirar compatibilidades antiguas
  con categoryId cuando el contrato actual no las necesita.
- Mantener el mapa visible con cero elementos y con filtros sin coincidencias.
- No dibujar coordenadas inválidas; no inventar 0.0,0.0.

A04. Implementar paginación por cursor

- Conservar nextCursor como string opaco.
- Solicitar la página siguiente con el cursor recibido y un limit entre 1 y 100.
- No interpretar ni fabricar el cursor.
- Evitar duplicados por id y detener la carga cuando nextCursor sea null.
- Definir estados inicial, cargando más, error recuperable y fin de lista.

A05. Implementar fotografías reales

- Reemplazar el diálogo de “foto de demostración” por Photo Picker y captura de
  cámara siguiendo las APIs actuales de Android.
- Guardar un Uri con permisos temporales seguros; no guardar rutas absolutas.
- Leer y validar Content-Type, tamaño y dimensiones antes de enviar.
- Comprimir solo cuando sea necesario y conservar JPEG/PNG/WebP.
- Adjuntar los bytes como parte multipart llamada exactamente image.
- Mostrar progreso y errores conservando título, descripción, categoría y punto.
- Si hasImage=true, descargar /users/me/reports/{id}/image con Bearer y mostrarla.
- Liberar bitmaps/streams y usar caché limitada por usuario.

A06. Centralizar expiración y errores

- Toda respuesta 401 debe limpiar el token y el estado privado, y volver a login.
- 400 debe mostrar validaciones del servidor.
- 404 del detalle/imagen debe mostrarse como no encontrado sin filtrar información.
- 409, 429 y 5xx deben tener mensajes diferenciados.
- Un IOException debe indicar servidor inaccesible/sin red.
- No reemplazar errores con datos DemoContent ni simular éxito.
- Cuando la API falle, conservar el último estado válido solo si pertenece a la
  misma sesión y mostrar claramente que está desactualizado.

A07. Actualizar documentación

- README y docs deben dejar de afirmar que GET /reports o GET /reports/{id} son
  rutas ciudadanas disponibles.
- Documentar cuenta obligatoria, rutas /users/me/reports, foto real, paginación y
  comportamiento de 401.
- Registrar la URL configurable del backend para emulador y teléfono físico.

PRUEBAS MÍNIMAS

- Sin token: inicia en login y no solicita reportes.
- Sesión restaurada: carga categorías y solo /users/me/reports.
- No existe ninguna llamada a GET /reports ni GET /reports/{id}.
- Cuenta A no conserva datos al salir; cuenta B no ve nada de A.
- 401 en lista, detalle, imagen o creación limpia sesión y estado privado.
- Detalle propio consume la ruta correcta y muestra historial.
- Mapa visible con cero reportes y con filtro vacío.
- Cursor agrega páginas sin duplicados y termina en null.
- Foto válida se envía con el nombre multipart image.
- Foto inválida o mayor de 5 MB conserva el borrador y no crea el reporte.
- Envío doble crea una sola solicitud.
- Errores 400/401/404/409/429/500 y red producen mensajes distintos.

VALIDACIÓN REQUERIDA

Ejecutá ./scripts/validate.sh. Deben pasar testDebugUnitTest, lintDebug,
assembleDebug y jacocoDebugReport. Agregá pruebas con MockEngine/MockWebServer para
verificar método, ruta, Bearer, cursor, multipart y respuestas. Ejecutá además un
recorrido en emulador o teléfono contra el backend real.

CRITERIO FINAL DE ACEPTACIÓN

Con dos cuentas A y B: A crea un reporte con imagen, A lo ve en mapa/lista/detalle;
B inicia sesión en el mismo dispositivo y no ve datos ni caché de A; A no puede
abrir un id de B; cerrar sesión deja únicamente login. Todas las verificaciones y
GitHub Actions deben quedar verdes. Subí los cambios al main del repositorio
Android con commits claros y documentá la evidencia.
```

## Prompt para la IA responsable de la web

```text
OBJETIVO

Completar el panel administrativo Angular de Mapa Urbano contra el backend Ktor
real, eliminar simulaciones y permitir que solo administradores autenticados vean
y gestionen el conjunto completo de reportes. Trabajá únicamente en:
https://github.com/Santy112/MapaUrbano-frontend-web.git

PUNTO DE PARTIDA AUDITADO

- Rama: main.
- Commit auditado: 85832a4183d6d1fde96b49161bee1142445f3cc6.
- Backend de referencia: ZeroVeCC/MapaUrbano-backend-ktor, commit
  34d219ea915ca9215e99005bcdcb098526bf4fa6.
- Contrato fuente de verdad: src/main/resources/openapi/documentation.yaml del
  backend. Swagger se sirve en http://localhost:8080/swagger.
- No modifiques el backend ni Android.
- Las rutas administrativas que antes estaban bloqueadas o devolvían 501 ya están
  implementadas y probadas con PostgreSQL/PostGIS.

AVANCES EXISTENTES QUE DEBÉS CONSERVAR

- Hay base de login, guarda, servicios HTTP y withCredentials.
- Existen servicios iniciales para reportes, categorías, estadísticas, auditoría,
  equipos y WebSocket.
- Se corrigió la construcción insegura de marcadores del mapa.
- El build y 8 pruebas pasaron localmente en la auditoría.

DEFECTOS CONFIRMADOS EN EL COMMIT AUDITADO

1. Persisten bypasses de login y datos simulados.
2. Varios servicios convierten fallos HTTP en éxitos locales.
3. Los modelos no coinciden con OpenAPI: ids number en vez de UUID string, estados
   y prioridades inexistentes, page/size en vez de cursor, displayOrder en vez de
   sortOrder y DTO incorrectos para equipos/estadísticas/auditoría/auth.
4. El detalle reutiliza el resumen de la tabla y no consulta /admin/reports/{id}.
5. No se descarga la imagen administrativa.
6. Estado, prioridad, fecha objetivo, asignación y baja no están conectados de
   extremo a extremo.
7. Dashboard, mapa, categorías, equipos, estadísticas y auditoría todavía usan
   contenido local o incompleto.
8. El servicio WebSocket existe pero no se conecta desde la aplicación.
9. GitHub Actions falla porque usa --browsers=ChromeHeadless con Vitest sin un
   proveedor de navegador configurado.

SEGURIDAD Y SESIÓN OBLIGATORIAS

- POST /api/v1/admin/auth/login es público y recibe {username,password}.
- Login responde {adminUserId,username,csrfToken} y emite la cookie HttpOnly
  mapa_urbano_admin.
- GET /api/v1/admin/auth/me restaura {adminUserId,username,csrfToken}.
- POST /api/v1/admin/auth/logout invalida la sesión.
- Todas las solicitudes HTTP /api/v1/admin/** deben usar withCredentials=true.
- Guardá csrfToken solo en memoria; recuperalo con /auth/me tras recargar.
- Enviá X-CSRF-Token en todos los POST, PUT, PATCH y DELETE administrativos,
  incluido logout.
- Un 401 limpia usuario, reportes, dashboard, auditoría, equipos, imágenes y socket,
  y redirige a login.
- Un 403 por CSRF puede intentar una sola restauración con /auth/me; si se repite,
  debe cerrar la sesión local.
- La cookie es HttpOnly: JavaScript no debe intentar leerla.

MODELOS OBLIGATORIOS

- Todos los UUID se representan como string.
- ReportStatus: PENDING | IN_PROGRESS | RESOLVED.
- ReportPriority: LOW | MEDIUM | HIGH | URGENT.
- SubmissionMode de datos históricos: ACCOUNT | ANONYMOUS.
- Category usa id, slug, name, description, icon, colorHex, isActive, sortOrder,
  createdAt y updatedAt según OpenAPI; no uses displayOrder.
- Las páginas usan {items,nextCursor,totalCount}; nextCursor es opaco.
- Toda mutación de reporte usa version. Un 409 exige recargar el detalle.
- No uses any para ocultar incompatibilidades de contrato.

TAREAS OBLIGATORIAS

W01. Cerrar autenticación real

- Eliminar cualquier administrador hardcodeado, bypass o fallback de login.
- Implementar login, restauración al arrancar, guarda de rutas y logout real.
- Crear interceptores para withCredentials, CSRF y manejo central de 401/403.
- Impedir que un usuario sin sesión entre a cualquier pantalla administrativa.
- Impedir que una sesión válida vuelva al login salvo que cierre sesión.
- Cerrar WebSocket y limpiar stores al hacer logout o recibir 401.

W02. Alinear el cliente con OpenAPI

- Corregir interfaces, enums, parámetros, métodos HTTP y respuestas.
- Preferentemente generar tipos desde OpenAPI o agregar pruebas de contrato.
- Eliminar REJECTED, CRITICAL, ids numéricos, page/size, displayOrder y demás
  valores que no existen en el backend.
- Implementar un ApiError común con error.code, error.message, error.details y
  requestId.

W03. Eliminar simulaciones peligrosas

- En modo integrado ningún error puede devolver mocks ni aplicar cambios locales.
- Si una mutación falla, conservar el estado anterior y mostrar el error.
- No eliminar una tarjeta local si DELETE falló.
- No simular login, creación de categoría, cambio de estado ni asignación.
- Si se conserva un modo demo, debe estar detrás de una bandera de entorno
  explícita, desactivada en producción y visualmente identificada.

W04. Integrar reportes administrativos

- GET /api/v1/admin/reports con status, categorySlug, priority, teamId,
  responsibleAdminUserId, search, cursor y limit.
- Renderizar items, nextCursor y totalCount; no fabricar páginas numéricas.
- GET /api/v1/admin/reports/{id} para el detalle real.
- Mostrar description, categoría, coordenadas, estado, prioridad, dueAt,
  submissionMode, asignación, version, historial de estado e historial de prioridad.
- GET /api/v1/admin/reports/{id}/image cuando hasImage=true, convirtiendo la
  respuesta binaria en una URL revocable y liberándola al cerrar el detalle.
- PATCH /reports/{id}/status con {status,version,note}.
- PATCH /reports/{id}/priority con {priority,dueAt,version}.
- DELETE /reports/{id} con cookie y CSRF, solo después de confirmación.
- Ante 409, no sobrescribir: recargar el detalle y avisar que otro administrador lo
  modificó.

W05. Integrar asignaciones y equipos

- PUT /reports/{id}/assignment con {teamId,responsibleAdminUserId,version}.
- DELETE /reports/{id}/assignment?version=... para desasignar.
- GET y POST /admin/teams.
- GET y PATCH /admin/teams/{id}.
- PUT y DELETE /admin/teams/{teamId}/members/{adminUserId}.
- GET /admin/assignees.
- Crear pantallas reales para equipos, miembros, responsables y activación.
- Validar que responsable/equipo provengan de los catálogos actuales.

W06. Integrar categorías

- GET /admin/categories debe mostrar activas e inactivas.
- POST /admin/categories crea con el DTO exacto.
- PATCH /admin/categories/{id} actualiza únicamente los campos permitidos.
- Tratar slug como inmutable después de crear si OpenAPI no lo admite al actualizar.
- Mostrar 400/404/409 reales y no inventar la categoría localmente.

W07. Integrar dashboard, mapa, estadísticas y auditoría

- Alimentar tabla y mapa con /admin/reports; eliminar marcadores constantes.
- GET /admin/statistics devuelve totalReports, byStatus, byCategory, byPriority y
  byTeam. Calcular porcentajes visuales desde los conteos si fueran necesarios.
- GET /admin/audit usa únicamente cursor y limit, y devuelve items, nextCursor y
  totalCount.
- Representar metadata de auditoría como JSON opcional.
- Los números del dashboard deben coincidir con los datos del servidor.

W08. Activar tiempo real

- Conectar /api/v1/ws/admin solamente después de restaurar la sesión.
- En navegador, la autenticación depende de la cookie; desplegar REST y WSS bajo un
  esquema/origen compatible con SameSite y usar HTTPS/WSS en producción.
- Interpretar los tipos de evento documentados en OpenAPI.
- Limitar reconexiones con backoff y cerrar el socket en logout/401.
- Después de reconectar, volver a consultar REST: el bus no conserva replay.
- Los eventos disparan refrescos o actualizaciones validadas; no deben duplicar
  reportes ni pisar una versión más nueva.

W09. Manejar errores y experiencia operativa

- Diferenciar red, 400, 401, 403, 404, 409, 429 y 5xx.
- Mostrar requestId en el detalle técnico de errores de servidor.
- Agregar loading, vacío y reintento en cada pantalla.
- Mantener foco accesible en modales, cierre con Escape, labels y navegación por
  teclado.
- Verificar responsive en 375 px, tablet y escritorio.
- Evitar suscripciones sin ciclo de vida; usar takeUntilDestroyed o equivalente.

W10. Corregir pruebas, CI y documentación

- Ajustar GitHub Actions al runner real de Angular/Vitest. Usar un comando Vitest
  compatible o configurar explícitamente el proveedor de navegador.
- Agregar pruebas HTTP de cookies/CSRF, DTO, filtros, cursor y errores.
- Agregar pruebas de guarda, restauración, 401, 403, 409 y logout.
- Agregar pruebas de detalle, imagen, estado, prioridad, asignación y eliminación.
- Agregar pruebas del WebSocket y de reconexión con recarga REST.
- Agregar un E2E contra Ktor/PostGIS.
- Reemplazar el README generado por Angular con configuración, variables, proxy,
  ejecución, pruebas y despliegue HTTPS/WSS.

ORDEN DE IMPLEMENTACIÓN

1. Corregir CI.
2. Alinear tipos y contratos.
3. Eliminar mocks/fallbacks de producción.
4. Completar sesión, CSRF, guarda e interceptores.
5. Listado, filtros, cursor, detalle e imagen.
6. Estado, prioridad, fecha, asignación y baja.
7. Categorías, equipos y responsables.
8. Dashboard, mapa, estadísticas y auditoría.
9. WebSocket.
10. Pruebas E2E, accesibilidad, responsive y documentación.

CRITERIO FINAL DE ACEPTACIÓN

Sin cookie, ninguna pantalla privada carga datos. Con un administrador válido, una
recarga restaura la sesión; se listan todos los reportes, incluidos anónimos
históricos; detalle e imagen provienen del backend; estado, prioridad, fecha y
asignación persisten después de recargar; un 409 no pisa cambios; un 500 no simula
éxito; logout invalida la sesión y cierra el socket. Build, pruebas y GitHub Actions
deben quedar verdes. Subí los cambios al main del repositorio web con commits claros
y documentá la evidencia.
```

## Prueba conjunta que deben coordinar ambos equipos

Cuando cada repositorio termine su plan, ejecutar contra el backend en
`34d219e` o posterior:

1. Registrar e iniciar sesión con ciudadano A.
2. Crear reporte A con coordenadas e imagen.
3. Registrar e iniciar sesión con ciudadano B en el mismo dispositivo.
4. Confirmar que B no ve lista, detalle, imagen ni caché de A.
5. Crear reporte B.
6. Iniciar sesión en web como administrador.
7. Confirmar que web ve A y B, además de cualquier anónimo histórico.
8. Cambiar estado y prioridad de A; asignarlo a un equipo.
9. Confirmar en Android A que el historial actualizado se recupera al refrescar.
10. Provocar un conflicto de versión desde dos pestañas y comprobar el 409.
11. Cerrar ambas sesiones y confirmar que los datos privados se limpian.
12. Verificar CI verde en los tres repositorios.

