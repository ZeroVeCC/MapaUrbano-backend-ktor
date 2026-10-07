# Revisión del proyecto y plan para completar el backend

Fecha: 1 de octubre de 2026. Revisión de código, contratos y entorno; no constituye una validación funcional completa contra PostgreSQL.

## Resultado general

La separación del backend está materializada: tiene build propio, wrapper, migraciones internas, Dockerfile y CI. Existe una implementación considerable, pero aún no está listo para integrar todos los flujos del MVP ni para producción. La documentación local sobrestima algunas funciones y describe cambios posteriormente revertidos.

No se cambió la lógica del backend durante esta revisión. Se prepararon herramientas y se ejecutaron verificaciones para preservar un diagnóstico del estado recibido.

## Repositorios revisados

| Repositorio | Revisión consultada | Situación |
|---|---|---|
| [Original](https://github.com/Jquincal/Mapa-Urbano) | `13150ab4142ccf15434d7fc1160f941b17fde8c7` | Documentación principal, alcance, arquitectura y contratos iniciales. |
| [Backend](https://github.com/ZeroVeCC/MapaUrbano-backend-ktor) | `f85f9638a8ea2ecf1495b8a8bd508956042ad098` | HEAD local coincide con HEAD remoto consultado. Árbol limpio al comenzar. |
| [Android](https://github.com/Jquincal/Mapaurbano_frontend_kotlin) | `b61792e5982dba366051d819876f172c1fe3fd1d` | Compose, navegación y pantallas base; `AppContainer` utiliza `FakeReportRepository`. |
| [Web](https://github.com/Santy112/MapaUrbano-frontend-web) | `cc8b987fdb13f40fbb3ce25d1101e9b201e71df0` | Angular 22, componentes iniciales; rutas vacías y sin integración HTTP en la configuración inspeccionada. |

Se descargaron copias temporales del original y de ambos frontends para leerlos; no se modificaron sus repositorios remotos. No se compilaron los frontends ni se revisaron ramas distintas de sus HEAD predeterminados.

## Qué debe hacer el producto

El vecino usa Android para ver problemas urbanos, crear un reporte con ubicación y foto opcional, y consultar su evolución. Puede registrarse o reportar anónimamente con código de seguimiento. El municipio usa Angular para gestionar reportes, estados, prioridades, equipos, asignaciones, estadísticas y auditoría. Ambos consumen Ktor mediante REST y WebSocket; los datos y fotos se guardan en PostgreSQL/PostGIS.

El alcance original incluye asignación manual y prioridades. Excluye iOS, push, chat, recuperación de contraseña, login social, asignación automática e inteligencia artificial. Esas funciones excluidas no deben contarse como pendientes del MVP salvo nueva exigencia docente.

Separar repositorios no implica necesariamente separar despliegues: RF-14 todavía pide servir Angular desde Ktor. Como el profesor solicitó cambios, hace falta dejar escrita la decisión vigente antes de dar por obligatorio ese diseño anterior.

## Base técnica y trabajo existente

- Kotlin 2.4.10, Ktor 3.5.1/Netty, JDK 21 y Gradle Wrapper 9.5.0.
- Koin para dependencias; Exposed 0.54.0, HikariCP y PostgreSQL; Flyway 13.2.0.
- Separación por módulos: `auth`, `users`, `reports`, `categories`, `media`, `assignments`, `statistics`, `notifications`, `audit`, más infraestructura y configuración.
- Rutas y casos de uso para registro/login, alta y consulta de reportes, seguimiento anónimo, categorías, equipos, prioridad, estado y eliminación lógica.
- BCrypt para contraseñas y generación de tokens opacos con hash para vecinos. Esto existe en los casos de uso, pero la autenticación HTTP no los verifica todavía.
- Migraciones V1/V2 con 12 tablas, claves foráneas, restricciones de autoría, una foto por reporte, una asignación activa y un índice espacial GIST. Seed de seis categorías.
- Fotos en `report_images.data BYTEA`, checksum SHA-256 y validaciones básicas de tamaño/MIME declarado.
- Cambio de estado con actualización por versión e historial en una transacción; publicación de un evento posterior desde el caso de uso.
- Bus de eventos y canales WebSocket público/administrativo.
- Errores JSON, CORS local, rate limiting, health endpoints, Docker Compose y GitHub Actions.
- Ocho archivos de pruebas Kotlin, incluyendo pruebas de migraciones, restricciones y backup/restore con Testcontainers. Su presencia no significa que estén pasando.

## Verificación realizada

| Comando / comprobación | Resultado |
|---|---|
| `java -version` | JDK 21.0.10 disponible. |
| `gradlew.bat test --no-daemon --console=plain` | FALLÓ en `compileTestKotlin`; no se ejecutó la suite. |
| `gradlew.bat compileKotlin buildFatJar --rerun-tasks --no-daemon --console=plain` | CORRECTO: recompilación y empaquetado ejecutados, sin depender de resultados previos. |
| JAR | `build/libs/mapa-urbano-backend.jar`. Generarlo no valida el arranque ni los endpoints. |
| PostgreSQL/PostGIS, HTTP y WebSocket reales | Pendientes de entorno operativo y correcciones. |

La falla de pruebas está en `ChangeReportStatusUseCaseTest.kt` y `CreateReportUseCaseTest.kt`: los repositorios de prueba implementan una firma antigua de `updateStatus`; también quedó el constructor antiguo con `AuditRepository` y una expectativa booleana donde ahora se devuelve `Report`.

Además, varias pruebas de rutas solo verifican que la respuesta no sea 404, lo cual permitiría aceptar un 500. `PublicReportRoutesTest` arranca el módulo completo y depende de una base accesible aunque no lleva la etiqueta `database`. Hay que corregir su preparación y sus aserciones; actualizar las firmas no garantiza que después todo pase.

## Pendientes prioritarios con evidencia

### 1. Autenticación y acceso: bloqueo principal

- `application/Security.kt` acepta el texto de cualquier Bearer como `UserIdPrincipal`, sin buscar hash, vencimiento, revocación ni usuario activo. Las rutas privadas interpretan ese texto como un ID de usuario. Debe resolverse la sesión real y separar `userId` de `sessionId`.
- `UserRoutes.kt` pasa ese texto al logout; `LogoutUserUseCase` espera el UUID de sesión. Hoy un token opaco normal no produce la revocación pretendida.
- No se instala/configura `Sessions`; la validación administrativa devuelve la sesión sin comprobar el repositorio y el challenge está vacío.
- `Routing.kt` coloca el login administrativo dentro de `authenticate("admin-session")`, exigiendo sesión para intentar obtenerla.
- No hay protección CSRF instalada ni configuración de cookies segura; `SESSION_SECRET` está declarado pero no se utiliza para firmarlas.
- El alta pública de reportes no tiene autenticación opcional: consultar `principal` allí no activa el procesamiento Bearer, por lo que el modo cuenta no puede obtener el usuario correctamente.
- Falta un procedimiento reproducible para crear el primer administrador, sin credenciales fijas.

**Criterio de cierre:** pruebas reales de registro/login, expiración, logout, cuenta desactivada, acceso cruzado, login administrativo sin sesión previa y mutaciones protegidas.

### 2. Persistencia, coordenadas y borrado lógico

- `CustomTypes.kt` solo interpreta texto que empieza por `POINT`; para el formato hexadecimal de PostGIS devuelve `(0,0)`. `ReportRepositoryImpl` lee directamente esa columna y no aplica la extracción `ST_X/ST_Y` mencionada en el comentario. Debe validarse un recorrido guardar/leer con coordenadas reales.
- `findById` y `findByTrackingCodeHash` no filtran `deleted_at`, y el mapeo de `Report` omite ese atributo. Un reporte eliminado puede seguir accesible por detalle/seguimiento.
- El endpoint público de imagen solo busca por ID del reporte y no comprueba su visibilidad/borrado.
- `Tables.kt` mapea auditoría `metadata` como texto y `source_ip` como varchar, mientras SQL define JSONB e INET. Es una incompatibilidad potencial que requiere probar inserciones con PostgreSQL real y corregir el binding.
- La lectura geográfica y los repositorios no quedan cubiertos por probar únicamente el DDL con JDBC.

### 3. Integridad de reportes, imágenes y cambios administrativos

- Se confirma el reporte antes de validar/guardar su foto, en transacciones separadas. Una foto inválida puede dejar un reporte persistido pese a que la solicitud termine en error.
- La imagen se carga completa con `readBytes()` antes de comprobar el límite propio de 5 MB; no se verifica firma real y las dimensiones se inventan como 800 × 600.
- Faltan procesamiento/normalización de imágenes, dimensiones reales, límites durante lectura y política EXIF. Faltan ETag/nosniff y visibilidad en la entrega.
- Coordenadas ausentes o texto inválido se convierten en `0.0`; modo inválido se convierte en anónimo. Hay que rechazar entradas incorrectas en lugar de cambiarlas silenciosamente.
- Completar límites de título/descripción, coordenadas finitas, área operativa acordada, categoría activa, UUID, fechas y errores 400 en vez de errores genéricos 500.
- El cambio de estado registra `report_status_history`, pero no un registro general en `audit_events`; además retorna directamente `Report`, que no es un DTO serializable configurado. Revisar respuesta HTTP y excluir campos internos.
- Prioridad y borrado registran auditoría en transacciones separadas. No se completa `report_priority_history` con sus valores anteriores/nuevos.
- Asignar/desasignar ignora el parámetro `version`, no actualiza la versión y realiza varios pasos separados. Faltan validación de equipo/responsable activo y pertenencia al equipo.
- Acordar y validar transiciones de estado permitidas: actualmente se acepta cualquier enum válido.

### 4. Funciones incompletas visibles para los clientes

Los siguientes handlers contienen una respuesta explícita 501, aunque la autenticación puede impedir llegar a ellos:

| Método | Ruta bajo `/api/v1` |
|---|---|
| GET | `/admin/reports` |
| GET | `/admin/reports/{id}` |
| GET | `/admin/teams` |
| GET | `/admin/teams/{id}` |
| GET | `/admin/assignees` |
| GET | `/admin/audit` |
| GET | `/users/me/reports/{id}` |

- Estadísticas: devuelve cero y colecciones vacías, no consultas reales.
- Detalle/seguimiento: historial vacío; `hasImage=false` incluso si existe una imagen.
- Paginación pública: se devuelve un cursor pero no se aplica a la consulta.
- Mis reportes: se devuelve un UUID como cursor, pero el repositorio intenta interpretarlo como fecha; `categorySlug` se rellena con el UUID de categoría.
- Faltan filtros y paginación administrativa coherentes, incluidos equipo, responsable, prioridad y fecha objetivo.
- Completar eventos de creación, borrado, prioridad, asignación y estadísticas. El publisher existente no está integrado en esos flujos; definir un contrato uniforme y payload público permitido.
- `/health/ready` siempre responde UP; debe comprobar dependencias y retornar 503 cuando corresponda.

### 5. Contrato compartido y documentación

`src/main/resources/openapi/documentation.yaml` contiene `paths: {}`. Swagger está montado, pero todavía no documenta los endpoints.

| Tema | Documentación original | Código actual | Acción |
|---|---|---|---|
| Archivo multipart | `photo` | `image` | Elegir un nombre y acordarlo con ambos clientes. |
| Filtro de mapa | `bbox` | `minLat/minLng/maxLat/maxLng` | Documentar formato, límites y ejemplos. |
| Estados y modo | Minúsculas | Respuestas con `.name` en mayúsculas | Unificar serialización y contratos. |
| Web en desarrollo | Angular | CORS solo permite 3000/5173 | Configurar origen/puerto acordado, cookies y CSRF o proxy de desarrollo. |
| Backend en README Android | Referencia final a Spring Boot | Ktor | Corregir inconsistencia documental. |
| Panel en producción | Servido por Ktor | Sin assets/rutas estáticas del panel | Confirmar cambio solicitado por el profesor. |

`docs/ESTADO_ACTUAL_Y_DECISIONES.md` todavía habla de `StorageService`, `/uploads`, `imageUrl` y una migración V3 inexistente. El código actual volvió a BYTEA; solo hay V1 y V2. También quedan recomendaciones de eventos desactualizadas respecto del último cambio de estado.

La entrega debe incluir OpenAPI con ejemplos, autenticación, errores, fotos, cursores y versiones; contrato WebSocket; README ejecutable y decisiones de separación/despliegue vigentes.

### 6. Docker, CI y operación

- `gradlew` está versionado con modo `100644`, pero Dockerfile ejecuta `./gradlew` sin darle permiso. CI sí hace `chmod`; el Dockerfile necesita resolverlo para un checkout Linux limpio.
- No hay `.dockerignore`; evitar enviar `.git`, cachés, compilaciones previas y archivos locales al contexto.
- El builder declara Gradle 8.7 pero ejecuta el wrapper 9.5.0: no es necesariamente un fallo, pero es una incoherencia a simplificar.
- `check/build` depende de `integrationTest` y necesita Docker, algo que el README no explica claramente.
- El workflow valida builds; no existe despliegue automático implementado pese al nombre CI/CD.
- Completar imagen reproducible, arranque verificado, secretos por entorno, HTTPS/WSS, logs/observabilidad y procedimiento de creación del administrador.
- Existe una prueba de backup/restore en Testcontainers, pero todavía hay que ejecutarla y definir backups/restauración del entorno real.
- Las sesiones administrativas y el bus están en memoria: documentar su pérdida al reiniciar y el límite de una instancia; no exigir escalado distribuido si no es parte del MVP.

## Orden sugerido para terminar

1. Recuperar compilación y ejecución de pruebas; hacer operativo Docker/PostGIS y verificar arranque, migraciones y repositorios.
2. Completar autenticación de vecinos/administradores, logout, permisos y CSRF.
3. Corregir coordenadas, borrado lógico, fotos y transacciones; demostrar alta anónima y alta asociada a cuenta.
4. Publicar el contrato OpenAPI y WebSocket acordado; conectar un flujo mínimo desde Android y web.
5. Completar los siete handlers, estadísticas, historiales, filtros, equipos y asignaciones con control de concurrencia.
6. Completar eventos y reconexión coordinada con los clientes; probar escenarios completos y errores.
7. Preparar demo/staging, documentación, HTTPS, backups y criterio de entrega académica.

No se asigna un porcentaje de avance: hay mucho código, pero fallan prerrequisitos transversales y aún no hay flujos completos verificados.

## Comandos de trabajo

Preparación efectuada: Java 21 y las extensiones Java/Gradle ya estaban instalados. Se instaló `JetBrains.kotlin-server` 0.0.12 en Antigravity IDE y Docker Desktop 4.93.0 mediante WinGet; se verificaron Docker CLI 29.8.1 y Compose 5.5.1. También se instaló WSL 2.7.13 y se habilitó `VirtualMachinePlatform` mediante DISM con `/NoRestart`.

**Bloqueo de entorno pendiente:** DISM devolvió 3010 (reinicio requerido) y `Win32_Processor.VirtualizationFirmwareEnabled` devolvió `False`. WSL informó que no puede iniciar WSL2 por falta de virtualización. Reiniciar cuando sea conveniente, revisar/habilitar virtualización en BIOS/UEFI y volver a comprobar `wsl --status` y `docker info`. No se reinició el equipo ni se cambió el firmware. Docker está instalado, pero el motor Linux no quedó validado ni PostgreSQL/PostGIS iniciado.

La extensión proviene de [Kotlin by JetBrains](https://marketplace.visualstudio.com/items?itemName=JetBrains.kotlin-server). Para Docker se siguió el mecanismo de instalación de Windows y su [backend WSL](https://docs.docker.com/desktop/features/wsl/). No hace falta instalar Gradle globalmente ni Node/Android SDK para desarrollar este backend.

Desde PowerShell en la raíz del backend:

```powershell
.\gradlew.bat compileKotlin buildFatJar
.\gradlew.bat test
docker compose up -d database
.\gradlew.bat integrationTest
.\gradlew.bat run
```

`test` falla actualmente por lo descrito. Los comandos de base/ejecución son el flujo previsto y no una afirmación de que ya funcionan. Ejecutar `build` cuando estén reparadas las pruebas y Docker esté operativo. Para el contenedor completo, corregir/verificar primero el permiso del wrapper en Dockerfile.

## Decisiones que el equipo debe dejar por escrito

- Qué cambió exactamente el profesor además de separar los repositorios y qué pide para la entrega.
- Si web se despliega aparte o se incorpora como artefacto compilado a Ktor.
- Nombre multipart, formato de bbox, casing de enums, cookies y esquema de eventos.
- Ciudad/área de prueba, transiciones permitidas, equipos/administrador inicial y política de datos.
- Criterios de aceptación y responsables actuales; la distribución del documento original puede haber cambiado.

La primera demostración verificable debería cubrir: reporte anónimo con foto y seguimiento; registro/login y reporte propio; administrador que cambia estado; actualización del cliente; reporte borrado ya no visible; sesión revocada rechazada.
