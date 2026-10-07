# Revisión de integración de los tres repositorios

Fecha: 6 de octubre de 2026. Alcance: lectura de los repositorios de los compañeros y cambios exclusivamente en este backend. No se modificaron ni publicaron cambios en Android ni web.

## Conclusión

**Todavía no se pueden unir y afirmar que funcionan de extremo a extremo.** Hay avances importantes de interfaz, pero los clientes publicados usan datos de demostración y operaciones locales. No tienen aún una integración REST/WebSocket con Ktor. Además persisten funciones incompletas del backend.

Esto distingue finalización de pantallas de finalización de integración. No hace falta juntar el código en un monorepo: las aplicaciones pueden seguir en sus tres repositorios y comunicarse mediante contratos HTTP/WebSocket y configuración de entorno.

## Versiones verificadas en GitHub

| Proyecto / rama | Commit consultado | Resultado |
|---|---|---|
| Backend `main` antes de publicar esta revisión | `f85f9638a8ea2ecf1495b8a8bd508956042ad098` | Los cambios locales de DNI todavía no estaban publicados al iniciar esta revisión. |
| Android `main` | `b61792e5982dba366051d819876f172c1fe3fd1d` | Misma base de pantallas/navegación de la revisión inicial. |
| Android `codex/frontend-stitch` | `0a26dd4d149d2b526743ae5a5f9c7b9d457933e4` | Interfaz mucho más completa, todavía con repositorio falso. Se revisó esta rama para no omitir ese avance. |
| Web `main` | `7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c` | Dashboard, reportes, categorías, estadísticas y auditoría implementados como interfaz local. |

Se consultaron las ramas publicadas: Android tiene `main` y `codex/frontend-stitch`; web tiene `main`. Esta revisión no cubre cambios que los compañeros aún no hayan subido.

## Android: qué existe y qué falta conectar

En la rama avanzada hay formulario, detalle, seguimiento, login/registro/perfil, filtros y estados de carga/error. Su propio [README](https://github.com/Jquincal/Mapaurbano_frontend_kotlin/blob/0a26dd4d149d2b526743ae5a5f9c7b9d457933e4/README.md) aclara que no consume backend, cámara, GPS ni SDK de mapas reales.

Evidencia:

- [ReportRepository.kt](https://github.com/Jquincal/Mapaurbano_frontend_kotlin/blob/0a26dd4d149d2b526743ae5a5f9c7b9d457933e4/app/src/main/java/com/mapaurbano/mobile/core/data/ReportRepository.kt): implementación `FakeReportRepository`, lista mutable en memoria.
- [AppViewModel.kt](https://github.com/Jquincal/Mapaurbano_frontend_kotlin/blob/0a26dd4d149d2b526743ae5a5f9c7b9d457933e4/app/src/main/java/com/mapaurbano/mobile/core/data/AppViewModel.kt): login valida correo y longitud mínima de cuatro caracteres; registro crea `demo-user`; alta guarda localmente; seguimiento busca en la lista; código anónimo se genera en el dispositivo.
- [Models.kt](https://github.com/Jquincal/Mapaurbano_frontend_kotlin/blob/0a26dd4d149d2b526743ae5a5f9c7b9d457933e4/app/src/main/java/com/mapaurbano/mobile/core/model/Models.kt): usuario con `email`, fotos representadas por booleano, fechas ya formateadas y ubicación como texto de presentación.
- [DemoContent.kt](https://github.com/Jquincal/Mapaurbano_frontend_kotlin/blob/0a26dd4d149d2b526743ae5a5f9c7b9d457933e4/app/src/main/java/com/mapaurbano/mobile/core/data/DemoContent.kt): categorías `roads`, `lighting`, `waste`, etc. e IDs como `REP-2048`.

Pendientes del equipo Android, informados aquí sin tocar su repositorio: implementar repositorio remoto; cambiar correo por DNI; usar tokens reales y almacenamiento seguro; consumir catálogo; enviar coordenadas/foto reales; usar IDs y códigos devueltos por el servidor; traducir DTO a modelos de presentación; consultar reportes propios por API; reconectar WebSocket y resincronizar REST.

## Web: qué existe y qué falta conectar

El panel ya tiene navegación y pantallas. Las operaciones inspeccionadas no llegan a Ktor:

- [reports.ts](https://github.com/Santy112/MapaUrbano-frontend-web/blob/7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c/src/app/features/reports/reports.ts): cinco reportes de ejemplo; `cambiarEstado` modifica `reporte.estado` en memoria.
- [categories.ts](https://github.com/Santy112/MapaUrbano-frontend-web/blob/7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c/src/app/features/categories/categories.ts): alta con `Date.now()` y eliminación mediante filtro de un array.
- [statistics.ts](https://github.com/Santy112/MapaUrbano-frontend-web/blob/7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c/src/app/features/statistics/statistics.ts): totales y tiempos escritos como constantes.
- [audit.ts](https://github.com/Santy112/MapaUrbano-frontend-web/blob/7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c/src/app/features/audit/audit.ts): entradas de auditoría estáticas.
- [dashboard.ts](https://github.com/Santy112/MapaUrbano-frontend-web/blob/7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c/src/app/features/dashboard/dashboard.ts): mapa MapLibre/OpenFreeMap con marcadores propios de demostración; no consulta reportes del servidor.
- `app.config.ts` no registra `HttpClient`; `app.routes.ts` no contiene login ni guards. La búsqueda en `src` no encontró llamadas HTTP/WebSocket al backend.

Pendientes del equipo web: servicios HTTP tipados, login administrativo, cookies y protección CSRF acordadas, manejo de 401/409/errores, IDs/versiones reales, operaciones persistentes, estadísticas/auditoría remotas y actualización WebSocket. Al sustituir textos fijos por datos de vecinos, no interpolarlos sin escape en el `setHTML` de los popups: usar nodos con texto seguro.

## Diferencias concretas de contrato

| Tema | Clientes publicados | Backend actual | Integración necesaria |
|---|---|---|---|
| Login ciudadano | Android usa correo y sesión simulada | DNI + contraseña y Bearer opaco | Aplicar [contrato DNI](AUTENTICACION_DNI.md). |
| Login municipal | Web no tiene flujo remoto | `username` + contraseña; implementación administrativa todavía incompleta | Completar backend y servicio/guard del panel. No usar token de vecino como administrador. |
| IDs | Web números; Android `REP-*` | UUID en texto | Conservar UUID; cualquier número visible es presentación. |
| Campos | Web `titulo`, `estado`, `imagenUrl`; Android modelos UI | DTO `title`, `status`, `hasImage`, etc. | Adaptadores en los clientes, no enviar modelos UI como JSON arbitrario. |
| Estados | Español / enums `Pending`, `InProgress`, `Resolved` | Respuestas `PENDING`, `IN_PROGRESS`, `RESOLVED` | Traducir explícitamente. No enviar `En proceso` como enum. |
| Categorías | Tres catálogos distintos | API con `id`, `slug`, `name`, `colorHex` | Consumir catálogo común. Decidir con el equipo si se amplía el seed; no mapear categorías diferentes por intuición. |
| Registro de reporte | Operación local con campos de pantalla | Multipart: `title`, `description`, `categorySlug`, `latitude`, `longitude`, `submissionMode`, archivo opcional `image` | Enviar formulario real. `photo` figura en el documento original, pero el handler actual espera `image`. |
| Descripción | Android puede habilitar envío sin descripción | Backend exige descripción no vacía | Alinear validaciones y mostrar errores del servidor. |
| Ubicación | Dirección y punto de mapa ilustrado / marcadores fijos | Coordenadas numéricas WGS84 | Obtener ubicación real. Dirección textual/reverse geocoding no existe en el DTO actual. |
| Foto | Booleano o imagen local de `/public` | Archivo binario y `GET /reports/{id}/image` | Cargar archivo y mostrar endpoint binario, no Base64 ni URL local del frontend. |
| Seguimiento | Código generado/buscado en el dispositivo | Código generado por backend y consultado por POST | El código del servidor es opaco; no generarlo ni imponer la longitud/formato de la demo. |
| Fechas | Texto como «hace 2 h» | ISO 8601 UTC | Formatear en la interfaz. |
| Listados | Arrays locales | `{items, nextCursor, totalCount?}` | Adaptar envoltorio y cursor; corregir paginación pendiente del servidor. |
| Cambio de estado | Asignación local en JS | PATCH con `status`, `version`, `note?` | Usar versión del servidor y manejar 409; la respuesta DTO del backend todavía requiere corrección. |
| Estadísticas | Incluye tiempo promedio fijo | Agregaciones declaradas, implementación aún vacía; no tiempo promedio | Acordar qué métricas implementa el MVP y completar consultas. |
| Categorías eliminadas | Web quita un elemento del array | PATCH `isActive=false` | Acordar desactivación lógica, no asumir DELETE remoto. |
| Tiempo real | Sin cliente WebSocket | `/api/v1/ws/public`, `/api/v1/ws/admin`, bus parcial | Implementar suscripción, reconexión y refresco REST; completar eventos del backend. |

## Trabajo de nuestro backend que sigue bloqueando la integración

Además del DNI ya implementado, quedan los hallazgos del [informe inicial](REVISION_Y_PLAN_2026-10-01.md):

1. Sesiones, login público administrativo y CSRF; provisión segura del primer administrador.
2. Los siete handlers 501 de reportes administrativos, equipos/responsables, auditoría y detalle de reporte propio; estadísticas reales.
3. Mapeo de coordenadas PostGIS, visibilidad tras borrado lógico, cursores y `hasImage`/historial reales.
4. Transacción única para reporte/foto; validación binaria de fotos, dimensiones y límites.
5. Respuesta serializable del cambio de estado, auditoría e integridad concurrente de asignaciones/prioridad.
6. Eventos completos después del commit y contrato OpenAPI del resto de módulos.
7. Ejecutar integración con PostgreSQL/PostGIS y comprobar arranque completo. Docker local sigue bloqueado por virtualización.

No se declara resuelto ninguno de estos puntos por haber compilado un JAR o ejecutado pruebas con repositorios en memoria.

## Cambios de esta revisión, solo en el backend

- Se agrega `http://localhost:4200` a CORS porque ese es el origen documentado para Angular. Se mantiene una lista explícita; no se habilita cualquier origen. Esto permite preflight de API, pero no completa cookies/CSRF administrativos ni habilita por sí mismo solicitudes con credenciales entre orígenes.
- Se agrega prueba de preflight permitido/rechazado.
- Dockerfile da permiso de ejecución a `gradlew`, necesario en un checkout Linux limpio.
- `.dockerignore` excluye Git, cachés, builds y archivos locales sensibles del contexto Docker.
- Se publican también todos los cambios locales de DNI, validación de sesiones, migración, OpenAPI, pruebas y documentación de los turnos anteriores.

## Validación y criterio de integración completa

La revisión de los frontends es de código y contratos. No se compilaron APK ni Angular, ni se conectaron clientes a PostgreSQL en esta máquina. Los árboles de sus copias de referencia quedaron sin cambios de código. No se enviaron mensajes ni commits a sus repositorios.

La suite general del backend tiene cuatro fallos conocidos en pruebas heredadas de rutas/configuración de base; se documentan en `AUTENTICACION_DNI.md`. Las **14 pruebas seleccionadas de los cambios pasaron**, cubriendo autenticación, autoría, eventos y CORS, y `buildFatJar` generó el ejecutable correctamente. Las pruebas de migraciones y la imagen Docker siguen sin validar por el bloqueo de virtualización.

Antes de dar la integración por terminada, deben verificarse juntos estos escenarios sobre una base real:

1. Android registra vecino con DNI, obtiene token y consulta su perfil.
2. Envía reporte en modo cuenta con foto; recibe UUID; aparece solo en sus reportes propios.
3. Crea reporte anónimo y obtiene el código generado por Ktor; puede consultarlo después de reiniciar la app.
4. Web inicia sesión administrativa, lista el mismo reporte y cambia su estado con versión.
5. El estado se refleja en Android y web sin recargar; tras reconectar se recupera el estado vía REST.
6. Una segunda modificación con versión vieja responde 409 y la interfaz permite recargar.
7. Logout invalida la sesión, el borrado oculta reporte/foto y no se exponen DNI ni datos privados en listados/eventos públicos.
8. Reiniciar backend/clientes conserva datos; probar backup/restauración.

### Configuración a acordar

- URL base por entorno y dispositivos de prueba. En teléfono físico, `localhost` es el teléfono, no la PC del backend.
- Uso de HTTPS en entorno compartido; configuración HTTP local solo de desarrollo.
- Origen del panel y estrategia de despliegue: mismo origen mediante proxy/artefacto, o cookies/CORS/CSRF entre orígenes. Repositorios separados no deciden eso automáticamente.
- Catálogo final de categorías y alcance de las métricas.
- Quién integrará en sus repositorios los adaptadores y llamadas descritos arriba.

No prometer «funciona todo junto» hasta superar esos escenarios con las versiones de los tres repositorios identificadas.
