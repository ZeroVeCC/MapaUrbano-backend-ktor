# Constancia de auditoría integral de Mapa Urbano

**Fecha de corte original:** 13 de octubre de 2026

**Última actualización:** 15 de octubre de 2026, después del reescaneo de repositorios y la revisión de privacidad

**Responsable de esta constancia:** revisión técnica realizada sobre los repositorios publicados de Mapa Urbano  
**Repositorio donde se conserva:** `ZeroVeCC/MapaUrbano-backend-ktor`  
**Propósito:** dejar una referencia verificable de qué existe, qué fue probado, qué está integrado y qué falta para que los tres componentes funcionen juntos.

> Esta constancia describe el estado observado en las revisiones exactas indicadas abajo. Una rama distinta o un cambio posterior puede modificar las conclusiones. Los repositorios de Android, web y el monorepo original se analizaron en copias temporales de solo referencia; esta auditoría únicamente modifica el repositorio del backend.

## 1. Resultado general

El proyecto tiene una base técnica amplia. Desde el 14 de octubre Android y backend ya forman un flujo ciudadano conectado, aunque todavía no existe una validación automatizada de extremo a extremo y el panel web continúa separado.

- El **backend Ktor** es el componente más avanzado. Compila, sus 47 pruebas pasan, levanta PostgreSQL/PostGIS con Testcontainers y ya resolvió la escritura y lectura de coordenadas. Sin embargo, el reescaneo confirmó que listado, detalle e imagen de reportes siguen siendo públicos; esto contradice la nueva política solicitada de que cada ciudadano vea únicamente sus reportes.
- El **frontend Android publicado en `main`** ya incorporó Stitch, cliente REST Ktor/OkHttp, autenticación por DNI, token Bearer en DataStore, MapLibre/OpenFreeMap, GPS y los flujos ciudadanos principales contra el backend. Continúa exactamente en `8c866de`: carga el listado público al iniciar, permite anónimos y mezcla reportes globales con propios. El cambio de cuenta obligatoria mencionado por el equipo todavía no está publicado.
- El **frontend web** compila para producción y contiene un panel visual completo, pero todos sus datos son locales. No tiene integración HTTP, WebSocket, login real, guardas ni manejo de sesión. Tres de sus ocho pruebas fallan.
- El **monorepo original** sigue siendo la fuente del alcance, arquitectura, requisitos y decisiones iniciales. Sus ramas posteriores no están consolidadas en `main`; por eso no debe tomarse `main` de ese repositorio como el estado actual del producto.
- El contrato ciudadano entre Android y backend coincide en los DTO y rutas revisados. Las barreras principales pasan a ser la autenticación/API administrativa, el panel web sin conexión, fotografías reales, paginación/historial incompletos y una prueba E2E reproducible.

## 2. Alcance y método de revisión

Se revisaron estos repositorios y referencias remotas:

| Componente | Repositorio | Referencia revisada | Revisión |
|---|---|---|---|
| Backend Ktor actual | `ZeroVeCC/MapaUrbano-backend-ktor` | `main` | `a33c1fdce1ac0bfc53cf02ae61242c86290bfb2e` |
| Backend, constancia inicial | mismo repositorio | `main` antes de esta actualización | `56907e566ac834fadef9bbb4982a522fc71f7c77` |
| Android standalone integrado | `Jquincal/Mapaurbano_frontend_kotlin` | `main` | `8c866deb2a7bdff1a112baf34f319b17be099822` |
| Android, base Stitch incorporada | mismo repositorio | commit antecesor en `main` | `0a26dd4` |
| Android, integración REST/mapa | mismo repositorio | commit antecesor en `main` | `9a8e2170cbacf6bdaf8331240959e94256436aa4` |
| Frontend web | `Santy112/MapaUrbano-frontend-web` | `main` | `7ba06ad` |
| Monorepo original | `Jquincal/Mapa-Urbano` | `main` | `13150ab` |
| Documentación original vigente | mismo repositorio | `develop` | `9b28a7e` |
| Backend anterior dentro del monorepo | mismo repositorio | `feat/MU-004-backend-ktor-exposed` | `a04651d` |
| Android anterior dentro del monorepo | mismo repositorio | `codex/android-frontend-stitch` | `e96cbf7` |

La revisión incluyó:

1. inventario de archivos versionados y conteo por tecnología;
2. lectura del código fuente, configuración, migraciones, documentación y pruebas;
3. comparación de ramas y cronología de commits;
4. inspección de pull requests y ejecuciones de GitHub Actions visibles hasta el 14 de octubre de 2026;
5. compilación y pruebas locales donde el entorno lo permitió;
6. comparación de contratos, modelos, categorías, estados, identificadores y autenticación entre componentes.

Los binarios se verificaron por ubicación, tipo y tamaño, sin pretender interpretar su contenido byte por byte. Esto se aplica principalmente a `gradle-wrapper.jar`, fuentes TTF e imágenes. Se excluyeron del análisis funcional directorios generados como `.git`, `build`, `node_modules` y cachés.

## 3. Tamaño observado

| Repositorio/referencia | Archivos relevantes | Código principal observado |
|---|---:|---|
| Backend `main` | 127 | 106 archivos Kotlin, 5.658 líneas; 3 migraciones SQL; OpenAPI; Docker; CI |
| Android `main` actualizado | 79 | 35 archivos Kotlin, 6.020 líneas; REST, MapLibre, GPS, DataStore y 4 fuentes Inter |
| Web `main` | 46 | 17 TypeScript, 8 HTML y 8 CSS; 2.048 líneas aproximadas entre esos grupos |
| Monorepo `develop` | documentación y base histórica | 20 Kotlin, 24 Markdown y 3 SQL; 27 commits visibles en el conjunto de ramas |

Estas cifras sirven para dimensionar el trabajo. No representan porcentaje de producto terminado.

## 4. Ruta histórica de tareas realizadas

### 4.1. Agosto de 2026: definición del producto

El monorepo original estableció el alcance funcional, la arquitectura y las decisiones base:

- aplicación Android nativa con Kotlin y Jetpack Compose;
- panel administrativo Angular;
- API Ktor con REST y WebSocket;
- PostgreSQL con PostGIS;
- fotografías guardadas como binario y metadatos en PostgreSQL;
- reporte anónimo con código de seguimiento;
- cuenta ciudadana opcional;
- administración de estados, prioridades, asignaciones y auditoría;
- Docker, HTTPS, copias de seguridad y una demostración reproducible.

También quedaron definidos 21 requisitos funcionales, 14 no funcionales, modelos de datos, endpoints propuestos, convenciones Git, estrategia de pruebas, despliegue y seguridad.

### 4.2. Finales de agosto y comienzos de septiembre: cimientos técnicos

Se creó la estructura Ktor, los módulos de dominio y las primeras decisiones de autenticación. Se preparó el esquema PostgreSQL/PostGIS con categorías, usuarios, administradores, reportes, imágenes, historial, equipos, asignaciones y auditoría.

La documentación ya contemplaba que una persona pudiera reportar sin cuenta o asociar el reporte a su cuenta. Esta decisión sigue reflejada en el backend actual mediante `submissionMode` y la obtención del autor desde la sesión.

### 4.3. 8 a 16 de septiembre: migraciones, trazabilidad y diseño Stitch

Se consolidaron las migraciones V1/V2 y el catálogo inicial de categorías. La documentación de `develop` agregó trazabilidad, matriz de requisitos, criterios de aceptación, riesgos y una secuencia P-01 a P-06 para llegar a la demostración integrada.

En paralelo apareció la variante visual Android basada en el diseño Stitch, con más pantallas, navegación y estados de interfaz. Esta variante evolucionó en ramas y no reemplazó el `main` del repositorio Android.

### 4.4. 23 de septiembre: backend Exposed dentro del monorepo

La rama `feat/MU-004-backend-ktor-exposed` incorporó la implementación Ktor/Exposed al repositorio original. En el monorepo continúa como trabajo no consolidado. El backend standalone posterior es la evolución que debe usarse como fuente actual.

### 4.5. 30 de septiembre: separación de repositorios

El backend se extrajo a un proyecto independiente. Se trasladaron migraciones, wrapper de Gradle, configuración y pruebas; luego se agregaron Docker Compose, Dockerfile y CI.

Durante esa separación se configuraron CORS, Swagger, persistencia de imágenes y los módulos de rutas. Algunos cambios fueron corregidos poco después, como el intento de almacenamiento estático de imágenes, que se revirtió para respetar `report_images` en PostgreSQL.

El repositorio Android recibió un `main` standalone básico con Compose y datos falsos. Ese `main` no contiene la interfaz Stitch completa.

### 4.6. 1 de octubre: backend y panel web

En el backend se corrigieron inyección de dependencias y registro de rutas, y se agregó una operación atómica para cambio de estado con historial y evento.

En la web se crearon sus únicos dos commits visibles:

- estructura Angular y dependencias;
- mapa visual 3D, fichas de reportes y módulo de auditoría.

El panel quedó visualmente desarrollado, pero con arreglos de datos locales y sin conexión a la API.

### 4.7. 6 y 7 de octubre: Android Stitch, DNI y persistencia geográfica

La rama Android `codex/frontend-stitch` llegó a su estado visual actual. El backend cambió la autenticación ciudadana a **DNI + contraseña** y añadió la migración V3, validación, sesiones opacas, documentación y pruebas.

Luego se corrigieron dos fallos importantes observados en pruebas con el dispositivo:

1. `PgEnum.setParameter` y `GeographyPointColumnType.setParameter` comenzaron a reutilizar valores `PGobject`, evitando insertar `NULL` en `status`, `priority` y `location`.
2. `GeographyPointColumnType.valueFromDB` comenzó a interpretar WKT/EWKT y WKB/EWKB, eliminando el fallback silencioso a `0.0, 0.0`. También se evitó generar el sobre geográfico antipodal para el límite mundial por defecto.

Se actualizó CI para que las pruebas unitarias no dependieran de una base local y se ajustaron pruebas de DNI y respuestas API.

### 4.8. 13 de octubre: auditoría integral

Se comprobaron las ramas remotas, se compiló el panel web, se ejecutaron sus pruebas y se ejecutó el conjunto completo de pruebas del backend con Docker/PostGIS. Esta constancia reúne esa evidencia y redefine la ruta pendiente según el código realmente publicado.

### 4.9. 14 de octubre: fusión de Android integrado

El repositorio Android fusionó el PR #1, **“Integrar frontend Android, API REST y mapa interactivo”**, mediante `8c866de`. La fusión incorpora el diseño Stitch `0a26dd4` y el commit `9a8e217`, que conecta la aplicación con el backend real.

El nuevo `main` reemplaza la maqueta de septiembre y agrega 6.917 líneas mientras elimina 1.033 de la estructura anterior. La rama remota `codex/frontend-stitch` fue eliminada tras la fusión. GitHub Actions `38567264700` completó correctamente pruebas unitarias, Lint, APK debug y JaCoCo mediante `scripts/validate.sh`.

### 4.10. 14 de octubre: contrato OpenAPI completo

El Swagger del backend dejó de describir solamente el registro y login ciudadano. La especificación OpenAPI 3.0.3 ahora registra todas las rutas HTTP y los dos handshakes WebSocket existentes, junto con parámetros, cuerpos JSON, multipart, respuestas, errores comunes, esquemas reutilizables y ejemplos.

Cada operación declara además su estado real mediante `x-implementation-status`: implementada, parcial, stub 501, bloqueada por autenticación administrativa o placeholder. De esta forma el contrato puede compartirse con Android y web sin presentar como terminadas las rutas que aún no funcionan. Se agregó `docs/SWAGGER.md` como guía de uso y una prueba automatizada que analiza el YAML, comprueba la cobertura de rutas, la unicidad de `operationId`, los estados y todas las referencias locales.

### 4.11. 15 de octubre: reescaneo y cambio de política de privacidad

Se descargaron nuevamente las referencias remotas de los cuatro repositorios. Android, web, `main` y `develop` del monorepo no presentan commits posteriores a los ya auditados. El backend agregó únicamente la auditoría y el contrato OpenAPI desde su última revisión funcional.

La prueba en dispositivos motivó una política nueva: cuenta obligatoria, cada ciudadano ve solo sus reportes y únicamente los administradores ven el conjunto completo. El código actual implementa el alcance anterior, que permitía mapa público y reportes anónimos. Frente al requisito nuevo, la API expone indebidamente listado, detalle e imagen sin autorización, y Android consume y mezcla esos datos. El diagnóstico, la matriz de acceso, las tareas por repositorio y dos mensajes listos para los equipos quedaron en `docs/REESCANEO_Y_PLAN_PRIVACIDAD_2026-10-15.md`.

## 5. Monorepo original: qué conserva y cómo usarlo

Repositorio: `Jquincal/Mapa-Urbano`.

### 5.1. Contenido valioso

La rama `develop` contiene la documentación más completa del producto:

- estado y alcance;
- arquitectura general y modular;
- módulos backend;
- especificación Android;
- especificación del panel;
- modelo de datos y migraciones;
- API REST y WebSocket;
- roadmap y criterios de aceptación;
- pruebas, Docker, despliegue, seguridad y privacidad;
- guía Git y proceso de trabajo;
- auditorías y trazabilidad.

Esta documentación sigue siendo la referencia para evaluar el objetivo final. Sin embargo, varias frases que indican que la API todavía no existe quedaron obsoletas después de la evolución del backend standalone.

### 5.2. Estado de integración en GitHub

- `main` está por detrás de `develop` y de las ramas de implementación.
- Permanecen abiertos PR #9 (documentación Stitch), PR #11 (backend Exposed) y PR #12 (Android Stitch).
- El contador de tres elementos abiertos corresponde a esos pull requests; no se observaron issues funcionales abiertos separados.
- El PR Android tuvo una ejecución exitosa. La ejecución antigua del PR de backend falló, pero ya no representa el backend standalone actual, cuyo CI está verde.

### 5.3. Regla de interpretación

Para futuras decisiones:

- usar el monorepo para **alcance y requisitos**;
- usar `ZeroVeCC/MapaUrbano-backend-ktor/main` para el **backend actual**;
- usar `Jquincal/Mapaurbano_frontend_kotlin/main` como Android vigente desde la fusión del PR #1;
- usar el repositorio web standalone para el **panel actual**;
- no portar automáticamente código viejo del monorepo sin compararlo con el standalone.

## 6. Backend Ktor: inventario técnico

### 6.1. Plataforma

- Kotlin 2.4.10 y Ktor 3.5.1.
- Java 21.
- JetBrains Exposed 0.54.
- PostgreSQL con PostGIS.
- Flyway 13.2, HikariCP y Koin.
- BCrypt para contraseñas.
- autenticación Bearer para vecinos y una intención de sesión para administradores;
- rate limiting, CORS, WebSocket y Swagger;
- JUnit, MockK y Testcontainers.

### 6.2. Organización del código

| Área | Responsabilidad observada |
|---|---|
| `application` | arranque, base de datos, DI, plugins, seguridad, límites y composición de rutas |
| `auth` | login/logout ciudadano y administrativo, DTO y repositorios de administrador |
| `users` | DNI, registro, sesión, perfil, baja y reportes propios |
| `reports` | alta, consulta pública, seguimiento, estado, prioridad y eliminación |
| `categories` | listado público y administración del catálogo |
| `media` | validación básica, persistencia y recuperación de imágenes |
| `assignments` | equipos, miembros, asignación y desasignación |
| `audit` | eventos administrativos persistidos |
| `statistics` | contrato de estadísticas, todavía con valores vacíos |
| `notifications` | bus de eventos en memoria y rutas WebSocket |
| `infrastructure/database` | tablas Exposed, tipos PostgreSQL/PostGIS y repositorios |
| `health` | liveness y readiness |
| `shared` | errores API, paginación y respuestas de endpoints no implementados |

### 6.3. Base de datos

La migración V1 crea:

1. `categories`;
2. `admin_users`;
3. `users`;
4. `user_sessions`;
5. `teams`;
6. `team_members`;
7. `reports`;
8. `report_images`;
9. `report_status_history`;
10. `report_priority_history`;
11. `report_assignments`;
12. `audit_events`.

También define enums de estado/prioridad, claves, restricciones, índices espaciales y triggers de actualización. V2 carga seis categorías: `bache`, `luminaria`, `basura`, `vandalismo`, `inundacion` y `otro`. V3 agrega DNI, permite compatibilidad con cuentas históricas por correo y exige una identidad válida.

No existe una tabla persistente de sesiones administrativas ni una tabla outbox para eventos.

### 6.4. Rutas verificadas en el código

| Método y ruta | Estado | Observación |
|---|---|---|
| `GET /health/live` | implementada | comprueba proceso vivo |
| `GET /health/ready` | parcial | responde 200; todavía no verifica la base |
| `GET /swagger` | implementada | expone el contrato OpenAPI 3.0.3 completo y el estado real de cada operación |
| `GET /api/v1/categories` | implementada | catálogo desde PostgreSQL |
| `GET /api/v1/reports` | implementada/parcial y en conflicto de privacidad | lista todos los reportes sin exigir sesión; debe cerrarse bajo la política nueva |
| `POST /api/v1/reports` | implementada/parcial y en conflicto de privacidad | permite cuenta o anónimo; debe exigir Bearer si se confirma cuenta obligatoria |
| `GET /api/v1/reports/{id}` | implementada/parcial y en conflicto de privacidad | detalle sin sesión ni pertenencia |
| `GET /api/v1/reports/{id}/image` | implementada y en conflicto de privacidad | descarga sin sesión ni pertenencia |
| `POST /api/v1/report-status` | implementada | seguimiento anónimo por código |
| `POST /api/v1/users/register` | implementada | DNI + contraseña |
| `POST /api/v1/users/login` | implementada | devuelve token Bearer opaco |
| `POST /api/v1/users/logout` | implementada | requiere Bearer |
| `GET /api/v1/users/me` | implementada | requiere Bearer |
| `DELETE /api/v1/users/me` | implementada | baja/revocación de sesiones |
| `GET /api/v1/users/me/reports` | implementada/parcial | problemas de cursor y categoría |
| `GET /api/v1/users/me/reports/{id}` | pendiente | responde 501 |
| `WS /api/v1/ws/public` | implementada/parcial | bus local, sin replay persistente |
| `WS /api/v1/ws/admin` | implementada/parcial | depende de seguridad admin incompleta |
| `/api/v1/admin/auth/*` | bloqueada funcionalmente | el login quedó dentro del bloque autenticado |
| listados/detalle admin de reportes | pendientes | stubs 501 |
| cambio de estado admin | implementado en dominio | la entrada HTTP depende de auth admin |
| prioridad/eliminación/asignación | parciales | repositorios/use cases existen; faltan garantías y flujo real |
| equipos/auditoría | parciales | varias consultas son stubs 501 |
| estadísticas | placeholder | devuelve totales vacíos/cero |

### 6.5. Autenticación ciudadana por DNI

El flujo actual correcto es DNI + contraseña:

- acepta DNI de 7 u 8 dígitos y normaliza puntos;
- rechaza valores no válidos y todos ceros;
- aplica BCrypt con costo 12;
- genera tokens opacos aleatorios de 32 bytes en Base64URL;
- guarda SHA-256 del token, nunca el token plano;
- aplica vencimiento de 30 días;
- permite logout y revoca sesiones cuando la cuenta se desactiva;
- evita que el cliente elija el `userId` autor del reporte.

El documento específico es `docs/AUTENTICACION_DNI.md`.

### 6.6. Correcciones de persistencia ya realizadas

Estas correcciones están en `main` y no deben volver a diagnosticarse como pendientes:

- los enums y `GeoPoint` se escriben correctamente aunque Exposed entregue un `PGobject` ya convertido;
- `status=pending`, `priority=medium` y `location` dejan de llegar como `NULL`;
- las coordenadas se leen desde WKT/EWKT o WKB/EWKB;
- un formato geográfico desconocido produce un error en vez de inventar `0.0, 0.0`;
- se respeta `latitude = Y` y `longitude = X`;
- el bbox mundial por defecto no crea un polígono antipodal inválido;
- un error de persistencia se traduce a una respuesta API controlada.

### 6.7. Pruebas y CI

Verificación local actualizada el 14 de octubre de 2026:

| Grupo | Resultado |
|---|---:|
| unitarias | 32/32 correctas |
| integración PostgreSQL/PostGIS | 15/15 correctas |
| total | **47/47 correctas, 0 fallos, 0 omitidas** |

Docker Desktop 29.8.1 se inició correctamente y Testcontainers ejecutó la integración real. GitHub Actions `38482124016`, correspondiente a la publicación inicial de esta constancia, también finalizó correctamente.

La suite cubre CORS, errores de persistencia, rutas base, DNI, login/logout/baja, migraciones, restricciones, autoría anónima y autenticada, tipos geográficos, lectura de coordenadas, metadatos de imagen, asignaciones, borrado lógico, eventos, consulta por código, cambio atómico de estado y creación de reportes. También valida el YAML OpenAPI, su cobertura de rutas, referencias internas, identificadores de operación, estados de implementación y la disponibilidad de Swagger UI.

Redocly CLI validó además `documentation.yaml` como una descripción OpenAPI válida, sin errores. Conserva advertencias no bloqueantes por las respuestas `101` de WebSocket y por endpoints públicos que no tienen una respuesta 4xx específica.

No cubre todavía el flujo administrativo HTTP, sesiones admin reales, procesamiento real de imágenes ni un recorrido completo desde Android/web.

### 6.8. Deuda y riesgos del backend

#### Prioridad crítica

1. **Autorización ciudadana incompatible con la política nueva:** listado, detalle e imagen son públicos. La corrección debe hacerse en backend; un filtro Android no constituye un control de acceso.
2. **Autenticación administrativa inutilizable:** todo el grupo `/admin`, incluido el login, está dentro de `authenticate("admin-session")`; una persona no autenticada no puede iniciar sesión.
3. **Falta instalar/configurar `Sessions`:** el código usa `call.sessions`, pero no hay transporte de cookie, firma ni configuración efectiva.
4. **Validador admin incompleto:** confía en la sesión deserializada y no la contrasta con una sesión persistida.
5. **Sesión admin en memoria:** `AdminSessionRepositoryImpl` usa `ConcurrentHashMap`; un reinicio la elimina.
6. **No hay administrador inicial:** no existe seed ni mecanismo documentado de bootstrap.
7. **No hay E2E automatizado cliente-servidor:** Android ya consume el backend y fue recorrido en dispositivo según su documentación, pero falta una prueba reproducible dentro de CI.

#### Prioridad alta

1. Los listados y detalles administrativos de reportes, equipos y auditoría contienen respuestas 501.
2. El detalle de reporte propio también responde 501.
3. `GET /reports` acepta cursor pero no lo aplica.
4. Reportes del usuario generan el siguiente cursor con ID, mientras el repositorio intenta interpretarlo como fecha.
5. Reportes del usuario colocan `categoryId` donde el DTO espera el slug y siempre indican `hasImage=false`.
6. Los historiales de detalle público no están reconstruidos y `hasImage` público queda falso.
7. La prioridad, asignación, eliminación y auditoría no siempre comparten una única transacción.
8. Algunos use cases reciben `version` pero no la emplean correctamente, debilitando el control de concurrencia.
9. Los eventos viven solo en memoria; sin outbox pueden perderse tras confirmar la transacción.
10. El contrato OpenAPI ya está completo; debe mantenerse sincronizado al implementar o modificar las rutas pendientes.

#### Prioridad media

1. Latitud/longitud ausentes o no numéricas pueden convertirse en `0.0`; deben rechazarse.
2. Falta validar en API todos los rangos y orden del bbox.
3. Filtros públicos de estado/categoría inválidos se ignoran silenciosamente.
4. Longitudes de título y descripción no están alineadas completamente con la base.
5. La imagen se carga completa en memoria antes de verificar 5 MB.
6. Solo se confía en MIME declarado; faltan firma, dimensiones reales y tratamiento de EXIF.
7. Reporte e imagen se insertan en transacciones separadas; una imagen fallida puede dejar el reporte creado.
8. Las dimensiones se guardan como 800×600 fijas.
9. Readiness no consulta PostgreSQL.
10. CORS está fijado a orígenes locales y no tiene configuración de producción.
11. `SESSION_SECRET` aparece en configuración, pero no protege todavía una cookie real.
12. Ktor marca APIs deprecadas (`Principal`, `streamProvider`, `dispose`) y hay dos `!!` evitables.
13. Algunos mensajes/comentarios muestran mojibake, con riesgo de respuestas mal codificadas.
14. Ktor no sirve todavía el build de Angular, requisito original RF-14.

## 7. Android: estado completo actualizado al 15 de octubre

### 7.1. Integración en `main`

El `main` vigente es `8c866de`, merge del PR #1. Contiene como antecesores la interfaz Stitch `0a26dd4` y la integración REST/mapa `9a8e217`. Ya no existe una diferencia funcional entre `main` y la antigua rama Stitch; esa rama remota fue eliminada.

El salto respecto de `b61792e` comprende 72 archivos modificados, 6.917 líneas agregadas y 1.033 eliminadas. La estructura anterior fue reemplazada por una aplicación Compose organizada como `UI → ViewModel → Repository`.

### 7.2. Funciones reales incorporadas

- Ktor Client 3.5.1 con motor OkHttp y serialización Kotlinx JSON.
- URL configurable por `MAPA_URBANO_API_BASE_URL`; emulador por defecto en `10.0.2.2`.
- `INTERNET`, ubicación aproximada y ubicación precisa en el manifiesto.
- tráfico HTTP local habilitado solo en la variante debug; release exige HTTPS.
- registro, login, restauración de sesión, perfil, logout y baja por DNI.
- token Bearer persistido en Preferences DataStore y eliminado ante logout, baja o 401 en llamadas protegidas.
- categorías, lista pública, detalle, creación multipart, seguimiento por código y Mis reportes conectados.
- elección real entre reporte anónimo y reporte asociado a la cuenta.
- MapLibre Native 13.6.1 con estilo Liberty de OpenFreeMap.
- marcadores desde coordenadas del backend, selección manual y centrado por GPS mediante Fused Location Provider.
- filtrado de `0.0, 0.0`, valores no finitos y coordenadas fuera de rango.
- mensajes diferenciados para red, 400, 401, 403, 404, 409, 429 y 5xx.
- preservación del borrador ante fallos y sesión vencida.

### 7.3. Inventario de responsabilidad

| Grupo | Función actual |
|---|---|
| `MainActivity.kt`, `MapaUrbanoApp.kt` | inicialización, tema, navegación, diálogos y conexión de pantallas |
| `core/data/AppContainer.kt` | composición manual de API y repositorios remotos |
| `core/data/AppViewModel.kt` | estado, carga remota, sesión, altas, tracking y navegación |
| `core/data/AuthRepository.kt` | adaptación de autenticación remota al modelo de UI |
| `core/data/RemoteReportRepository.kt` | conversión de DTO backend a modelos Android |
| `core/data/SessionStore.kt` | almacenamiento local del Bearer token |
| `core/data/UserFacingErrors.kt` | traducción de red/HTTP a mensajes específicos |
| `core/network/ApiDtos.kt` | espejo del contrato ciudadano del backend |
| `core/network/MapaUrbanoApi.kt` | llamadas REST, multipart, Bearer y decodificación de errores |
| `core/location/LocationRequest.kt` | permisos en contexto y una lectura de ubicación actual |
| `feature/map/InteractiveMap.kt` | ciclo de vida MapLibre, estilo, cámara y marcadores |
| `feature/map/MapScreen.kt` | mapa/lista, filtros, estados vacíos y acción de alta |
| `feature/report/*` | formulario, selector geográfico y confirmación |
| `feature/auth/*` | login/registro DNI y perfil |
| `feature/myreports/*` | listado y detalle ciudadano |
| `feature/tracking/*` | consulta real por código |
| `DemoContent` y `FakeReportRepository` | fakes reservados para pruebas y previsualización |

### 7.4. Compatibilidad comprobada con el backend

La comparación campo por campo mostró coincidencia en:

- `LoginRequest/Response`, `RegisterRequest/Response` y `UserProfileResponse`;
- `CategoryResponse`;
- `CursorPage`;
- resumen y detalle público de reporte;
- historial de estados;
- creación multipart: `title`, `description`, `categorySlug`, `latitude`, `longitude`, `submissionMode`;
- consulta `trackingCode`;
- envoltorio `ApiErrorEnvelope` y sus detalles.

La aplicación ya compensa dos defectos conocidos del backend: acepta que Mis reportes coloque un ID dentro de `categorySlug` y evita paginar mientras el cursor no esté corregido.

### 7.5. Correcciones Android confirmadas

- El mapa permanece visible con cero reportes y con filtros sin coincidencias.
- El estado vacío distingue entre “Todavía no hay reportes” y “No encontramos coincidencias”.
- La acción de crear reporte continúa visible.
- La autenticación visual y el modelo cambiaron de correo a DNI.
- Los mensajes 500 ya no se presentan como falla de conexión.
- Las coordenadas reales se muestran después de la corrección PostGIS del backend.

### 7.6. Pendientes y riesgos Android

#### Prioridad alta

1. La fotografía sigue siendo una simulación visual: no se conservan bytes ni se adjunta la parte `image` al multipart.
2. No hay prueba automatizada cliente-servidor; los tests de ViewModel usan fakes por diseño.
3. Mis reportes consume solo la primera página y el detalle privado usa temporalmente la ruta pública.
4. Historial e indicador de imagen dependen de campos que el backend todavía devuelve vacíos/falsos.
5. El token se almacena en Preferences DataStore sin cifrado de aplicación. Los backups se excluyen, pero debe decidirse la protección local requerida.

#### Prioridad media

1. El mapa usa marcadores predeterminados iguales; no diferencia visualmente estado ni selección como proponía el plan.
2. `InteractiveMap` avisa que comenzó una interacción en `ACTION_DOWN`, pero no emite explícitamente `false` en `ACTION_UP/CANCEL`. Debe probarse que el scroll vertical se reactive siempre después de usar el mapa.
3. Permiso denegado y GPS apagado muestran explicación, pero no ofrecen todavía acceso directo a ajustes.
4. `HttpClient` vive durante toda la aplicación y no expone cierre; es aceptable como singleton de proceso, pero debe mantenerse así para no crear clientes repetidos.
5. Las funciones “recuperar contraseña”, notificaciones, editar perfil y cambiar contraseña siguen marcadas como futuras.
6. El borrador vive en el ViewModel; no hay persistencia duradera ante muerte del proceso.
7. La integración se probó manualmente en un Redmi según la documentación, pero la validación final de gestos/GPS todavía figura pendiente en los documentos.

### 7.7. Pruebas y CI Android

El código contiene 11 pruebas unitarias y 2 pruebas instrumentadas Compose. Las unitarias cubren flujo anónimo, login conservando borrador, seguimiento, sesión vencida, doble envío, error 500, mensajes HTTP/red y coordenadas. Las instrumentadas comprueban mapa con reportes y mapa vacío visible.

El workflow `android.yml` ejecuta `scripts/validate.sh`, que comprueba secretos y formato, y luego corre pruebas unitarias, Android Lint, construcción del APK debug y JaCoCo. No ejecuta las dos pruebas instrumentadas porque no configura dispositivo/emulador.

GitHub Actions `38567264700`, sobre `8c866de`, terminó correctamente el 14 de octubre de 2026. La auditoría local no pudo repetir Gradle porque esta computadora no tiene Android SDK configurado; el intento alcanzó la configuración de Gradle y se detuvo con `SDK location not found`. El resultado remoto verde es la evidencia de compilación disponible.

### 7.8. Reescaneo de privacidad

El `main` Android no cambió desde la auditoría anterior. La aplicación sigue abriendo en el mapa, consulta el listado global sin Bearer, conserva alta anónima y seguimiento por código, y combina reportes propios con los públicos. La versión con cuenta obligatoria mencionada durante las pruebas todavía no está en ninguna rama remota visible. Bajo la política nueva, Android debe esperar la restauración de sesión, consumir exclusivamente rutas propias y borrar los datos de la cuenta anterior al cerrar o cambiar sesión.

## 8. Frontend web: estado completo

### 8.1. Plataforma y estructura

El panel usa Angular 22, TypeScript 6, MapLibre y Leaflet. Contiene rutas y componentes para:

- dashboard;
- reportes;
- categorías;
- estadísticas;
- auditoría;
- sidebar y estructura general.

### 8.2. Funcionalidad visual existente

- dashboard con mapa MapLibre 3D, cuatro marcadores y tarjetas de métricas;
- listado/fichas de cinco reportes;
- cambio de estado local;
- imágenes locales de demostración;
- catálogo local de cinco categorías con alta y eliminación en memoria;
- estadísticas fijas, por ejemplo un total de 42;
- siete eventos de auditoría simulados y filtro visual;
- navegación lateral y diseño adaptable.

### 8.3. Integración ausente

- No está configurado `HttpClient` ni existen servicios API.
- No hay login administrativo.
- No hay guardas de rutas.
- No hay cookies, CSRF ni renovación/verificación de sesión.
- No hay cliente WebSocket.
- Los cambios desaparecen al recargar.
- No se consumen categorías, reportes, imágenes, estadísticas ni auditoría del backend.
- No hay workflow de GitHub Actions.

El dashboard crea popups mediante `setHTML` e interpolación. Con constantes locales no produce un incidente actual; al conectar datos de API sería una vía XSS si no se escapa o reemplaza por nodos seguros.

### 8.4. Compilación y pruebas

La compilación de producción se repitió correctamente el 15 de octubre:

- bundle inicial: 409,17 kB sin comprimir;
- transferencia estimada: 92,24 kB;
- advertencia: `reports.css` pesa 4,27 kB y excede en 272 bytes el presupuesto de 4 kB.

Resultado de pruebas:

- 8 pruebas totales;
- 5 correctas;
- 3 fallidas.

Los fallos son dos pruebas de `App` y una de `Sidebar`, inicialmente por no proveer `ActivatedRoute`. Además, una aserción de título espera `Hello, mapa-urbano`, texto que ya no existe en `app.html`. Son pruebas desactualizadas respecto de la navegación actual.

No hay commits, ramas remotas nuevas, pull requests ni workflow desde la revisión anterior. El panel continúa sin conexión y debe consumir únicamente `/api/v1/admin/*` cuando la autenticación administrativa esté reparada.

## 9. Compatibilidad entre los tres componentes

| Tema | Backend | Android `main` | Web | Consecuencia |
|---|---|---|---|---|
| identidad ciudadana | DNI + contraseña | DNI + contraseña real | no aplica al vecino | contrato Android/backend alineado |
| administrador | sesión pretendida, incompleta | no aplica | sin login | panel no puede autenticarse |
| fuente de datos | PostgreSQL/PostGIS | API real; fakes solo en pruebas | constantes locales | Android consume hoy datos globales; administración no está conectada |
| privacidad de reportes | listado/detalle/imagen públicos | carga y mezcla reportes globales | todavía sin API | incompatible con la nueva regla de reportes propios |
| categorías | 6 slugs en español singular | catálogo remoto por ID y slug | 5 categorías locales | Android alineado; web pendiente |
| ID de reporte | UUID | UUID recibido del backend | números locales | Android alineado; web pendiente |
| tracking | código opaco del servidor | endpoint real | no implementado | flujo ciudadano integrado |
| estados | `pending`, `in_progress`, `resolved` | conversión tolerante a mayúsculas | etiquetas españolas locales | Android alineado; web pendiente |
| prioridad | enum backend | no modelada de forma operativa | visual/parcial | panel no puede administrarla aún |
| coordenadas | PostGIS, latitud/longitud reales | MapLibre + GPS + marcadores remotos | marcadores fijos | flujo móvil conectado; web local |
| fotografías | BYTEA + endpoint específico | selección simulada, sin bytes | archivos públicos locales | falta carga/descarga real en clientes |
| errores HTTP | envoltorio controlado | distingue red y códigos principales | sin cliente | Android alineado; web pendiente |
| tiempo real | WebSocket en memoria | sin cliente | sin cliente | actualizaciones no llegan a UI |
| paginación | incompleta | primera página, sin cursor | no consume | corregir backend antes de habilitarla |

### Contratos que deben congelarse antes de integrar

1. estabilizar definitivamente el uso de `slug`/`id` en Mis reportes;
2. corregir y probar el cursor de `{ "items": [...], "nextCursor": ... }`;
3. publicar la serialización exacta de estado y prioridad en OpenAPI;
4. UUID como identificador público y código opaco solo para seguimiento anónimo;
5. multipart de creación, nombres de partes y errores;
6. contrato de imagen y `hasImage`;
7. formato de cursor;
8. seguridad ciudadana Bearer y seguridad administrativa por cookie;
9. estructura común de error para 400, 401, 404, 409, 429 y 500;
10. eventos WebSocket y política de reconexión.

## 10. Matriz de requisitos funcionales

Estados usados: **cumplido**, **parcial**, **pendiente** o **bloqueado**.

| Req. | Objetivo resumido | Estado observado | Evidencia/deuda |
|---|---|---|---|
| RF-01 | mapa Android con reportes | parcial/incompatible | MapLibre funciona, pero muestra reportes globales en vez de solo los propios |
| RF-02 | iniciar desde punto del mapa | cumplido | selector táctil y GPS producen coordenadas reales |
| RF-03 | formulario completo y foto | parcial alto | formulario/multipart reales; foto todavía simulada |
| RF-04 | detalle y estado ciudadano | parcial/incompatible | usa detalle público sin pertenencia; detalle privado sigue 501 |
| RF-05 | filtros por categoría/estado | parcial alto | Android filtra datos reales; web continúa local |
| RF-06 | admin cambia estado | parcial/bloqueado | operación atómica existe; auth admin impide flujo real |
| RF-07 | tabla paginada y mapa admin | parcial | UI demo; API/listado admin pendientes |
| RF-08 | totales/distribuciones | parcial | UI fija; backend devuelve vacío |
| RF-09 | foto binaria y metadatos | parcial | esquema/repositorio existen; validación/atomicidad incompletas |
| RF-10 | eliminar con auditoría | parcial | use case existe; flujo y transacción completa pendientes |
| RF-11 | código para anónimo | implementado bajo alcance anterior | debe retirarse si se confirma cuenta obligatoria |
| RF-12 | consultar por código | implementado bajo alcance anterior | debe desactivarse o limitarse a una transición de históricos |
| RF-13 | evento WS tras commit | parcial | estado publica evento; bus no persistente y clientes ausentes |
| RF-14 | Ktor sirve Angular | pendiente | no hay static content del build web |
| RF-15 | asignar/reasignar | parcial | dominio/repositorio; UI/API real incompletas |
| RF-16 | prioridad y fecha objetivo | parcial | dominio/esquema; flujo admin no operativo |
| RF-17 | historial de asignación/prioridad | parcial | tablas existen; garantías transaccionales incompletas |
| RF-18 | registro/login/sesión/logout vecino | cumplido | DNI y Bearer integrados en Android/backend |
| RF-19 | listar y consultar reportes propios | parcial crítico | lista propia existe, pero Android también conserva globales; cursor y detalle privado pendientes |
| RF-20 | elegir cuenta o anónimo | en conflicto con política nueva | está implementado, pero el requisito nuevo pide cuenta obligatoria |
| RF-21 | autor desde sesión, no cliente | cumplido en backend | cubierto por implementación/pruebas |

## 11. Matriz de requisitos no funcionales

| Req. | Estado | Observación |
|---|---|---|
| RNF-01, mapa ≤3 s/200 reportes | pendiente | mapa/API integrados, pero sin prueba de carga |
| RNF-02, responsive 360 dp/375 px | parcial | diseños existen; falta validación sistemática |
| RNF-03, admin y cookie segura | bloqueado | sesión Ktor no configurada |
| RNF-04, CRUD p95 <500 ms | pendiente | no hay prueba de carga |
| RNF-05, imágenes 5 MB y firma | parcial | límite/MIME básico; firma real ausente |
| RNF-06, persistencia tras reinicio | parcial alto | PostgreSQL y coordenadas probadas; sesiones/eventos admin no |
| RNF-07, Node solo build | pendiente de despliegue | arquitectura objetivo clara; Ktor no sirve Angular |
| RNF-08, índices/área/límite | parcial | índice y bbox; validación/paginación incompletas |
| RNF-09, HTTPS/WSS | pendiente | no hay despliegue productivo documentado/validado |
| RNF-10, accesibilidad | parcial | no auditada completamente; web sin prueba teclado integral |
| RNF-11, concurrencia optimista | parcial | estado mejor cubierto; otras operaciones no |
| RNF-12, binario fuera de listados | cumplido en diseño/backend | imagen usa endpoint específico |
| RNF-13, secretos no expuestos | parcial alto | BCrypt/token hash correctos; Android guarda Bearer en DataStore sin cifrado propio; defaults/admin pendientes |
| RNF-14, privacidad pública/WS | crítico bajo política nueva | listado, detalle e imagen globales siguen accesibles sin pertenencia |

## 12. Ruta recomendada desde este estado

Esta secuencia evita que cada equipo construya contra supuestos diferentes.

### Fase 0 — fijar referencias y contrato

- [x] Fusionar `codex/frontend-stitch` al `main` Android standalone mediante PR #1.
- [x] Publicar un OpenAPI completo desde el backend.
- [x] Definir ejemplos reales de request/response y errores.
- [x] Alinear Android con categoría, estado, UUID y tracking del contrato ciudadano actual.
- [x] Completar prioridad, cursor, imagen e historiales en el contrato publicado, indicando sus limitaciones actuales.
- [x] Añadir una prueba automatizada del contrato OpenAPI.

**Salida:** los dos frontends pueden generar modelos contra un contrato estable.

### Fase 0.5 — aplicar la nueva política de privacidad

- [ ] Confirmar formalmente cuenta obligatoria y retiro del modo anónimo.
- [ ] Definir que los reportes anónimos históricos sean visibles solo para administradores.
- [ ] Exigir Bearer para crear, listar, abrir y descargar imágenes ciudadanas.
- [ ] Implementar pertenencia en detalle e imagen, devolviendo 404 genérico para recursos ajenos.
- [ ] Desactivar listado/detalle/imagen públicos y revisar `/ws/public`.
- [ ] Hacer que Android espere sesión y consuma únicamente `users/me/reports`.
- [ ] Reservar la lectura global para `/api/v1/admin/*`.
- [ ] Probar dos cuentas y un administrador contra la misma base.

**Salida:** ningún ciudadano ni cliente sin sesión puede conocer reportes de otra cuenta.

### Fase 1 — cerrar el flujo ciudadano del backend

- [ ] Rechazar coordenadas ausentes/no numéricas en lugar de convertirlas a cero.
- [ ] Corregir cursores y mapeo de categoría/imagen en reportes propios.
- [ ] Implementar detalle propio.
- [ ] Completar `hasImage` e historial visible del reporte propio.
- [ ] Hacer atómica la creación de reporte + imagen.
- [ ] Validar firma, tamaño y dimensiones de archivos sin cargar innecesariamente todo en memoria.

**Salida:** registro, login, alta asociada, lista, detalle e imagen propios estables.

### Fase 2 — corregir seguridad administrativa

- [ ] Sacar el login del bloque que exige autenticación.
- [ ] Instalar `Sessions` con cookie HttpOnly, Secure en producción, SameSite y firma/cifrado.
- [ ] Persistir y validar sesiones admin.
- [ ] Crear mecanismo seguro de administrador inicial.
- [ ] Incorporar CSRF donde corresponda.
- [ ] Agregar pruebas integradas del flujo admin completo.

**Salida:** el panel puede iniciar/cerrar sesión y las rutas protegidas rechazan accesos inválidos.

### Fase 3 — completar API administrativa

- [ ] Listado y detalle paginados de reportes.
- [ ] Estado, prioridad, fecha objetivo y eliminación con auditoría atómica.
- [ ] Equipos, miembros y asignaciones.
- [ ] Auditoría consultable.
- [ ] Estadísticas calculadas desde PostgreSQL.
- [ ] Control de versión consistente en todas las mutaciones.

**Salida:** se puede operar un reporte entero por API sin stubs.

### Fase 4 — integrar Android

- [x] Fusionar la rama visual elegida.
- [x] Cambiar correo por DNI.
- [x] Agregar cliente HTTP, permiso de Internet y persistencia local del token.
- [x] Consumir categorías, mapa/lista, alta multipart sin imagen, detalle, Mis reportes y tracking.
- [x] Mantener visible el mapa cuando no hay resultados.
- [x] Diferenciar red, 400, 401, 409, 429 y 500.
- [x] Incorporar MapLibre, selección manual y ubicación GPS.
- [ ] Publicar en GitHub el cambio de cuenta obligatoria mencionado por el equipo.
- [ ] Esperar la restauración de sesión antes de mostrar/cargar el mapa.
- [ ] Retirar el listado/detalle global, modo anónimo y seguimiento por código.
- [ ] Limpiar todos los datos privados al cerrar o cambiar de cuenta.
- [ ] Adjuntar fotografías reales y mostrar la imagen recuperada.
- [ ] Corregir paginación y usar el detalle privado cuando el backend lo implemente.
- [ ] Automatizar una prueba de contrato/E2E contra Docker.
- [ ] Completar la validación manual de gestos, GPS y scroll en el Redmi.

**Salida actual:** recorrido ciudadano principal conectado; faltan foto, paginación y validación E2E reproducible.

### Fase 5 — integrar web

- [ ] Reparar las tres pruebas y agregar CI.
- [ ] Crear servicios HTTP y modelos desde OpenAPI.
- [ ] Implementar login, cookie, guarda e interceptor/CSRF.
- [ ] Reemplazar todos los arreglos simulados por API.
- [ ] Sanitizar popups de MapLibre.
- [ ] Conectar WebSocket con reconexión.
- [ ] Resolver el presupuesto CSS y revisar accesibilidad/375 px.

**Salida:** recorrido administrativo real contra la misma instancia Ktor.

### Fase 6 — integración, despliegue y demostración

- [ ] Servir el build Angular desde Ktor o documentar formalmente el cambio de arquitectura.
- [ ] Parametrizar orígenes CORS y secretos por entorno.
- [ ] Hacer readiness real contra PostgreSQL.
- [ ] Añadir HTTPS/WSS, backup/restore y observabilidad.
- [ ] Ejecutar pruebas E2E de ambos clientes, concurrencia y carga.
- [ ] Preparar datos sintéticos y un guion reproducible de demo.

**Salida:** otra persona puede levantar el sistema y completar ambos recorridos sin ayuda del equipo.

## 13. Orden de urgencia práctico

Si el objetivo inmediato es mostrar una demostración funcional, el orden mínimo aconsejado es:

1. cerrar listado, detalle e imagen ciudadanos por sesión y pertenencia;
2. mover Android a cuenta obligatoria y rutas propias;
3. autenticación admin;
4. endpoints admin sin 501;
5. web real;
6. cerrar foto/paginación/detalle propio y ejecutar E2E con dos usuarios y un administrador;
7. rendimiento y despliegue.

Android ya superó la etapa de pantallas simuladas, pero la privacidad debe corregirse antes de continuar la integración funcional. Después, el foco puede trasladarse al panel administrativo y a las partes concretas que continúan incompletas.

## 14. Evidencia reproducible

### Backend

```powershell
./gradlew.bat test --rerun-tasks
./gradlew.bat integrationTest --rerun-tasks
./gradlew.bat build --rerun-tasks
```

Las pruebas de integración requieren Docker activo porque usan Testcontainers con PostgreSQL/PostGIS.

### Android

El repositorio define esta validación:

```bash
./scripts/validate.sh
```

Ejecuta `testDebugUnitTest`, `lintDebug`, `assembleDebug` y `jacocoDebugReport`. GitHub Actions `38567264700` la completó correctamente sobre `8c866de`. Las 2 pruebas instrumentadas requieren dispositivo/emulador y no forman parte del workflow actual.

En esta computadora se descargó correctamente Gradle 9.1, pero no se pudo repetir la compilación porque no hay un Android SDK instalado/configurado. El error fue `SDK location not found`; no fue un fallo del código.

### Web

En la copia temporal de auditoría se utilizó pnpm por no estar `npm` disponible en PATH:

```powershell
pnpm install
pnpm exec ng build --configuration production
pnpm exec ng test --watch=false
```

La compilación pasó y las pruebas quedaron 5/8, con los fallos documentados en la sección 8.4.

### GitHub

Se verificaron ramas, commits, pull requests y ejecuciones mediante metadatos públicos de GitHub. Para Android se confirmó el PR #1 fusionado y el workflow exitoso `38567264700`. El estado corresponde al momento de la auditoría y puede cambiar.

## 15. Reglas de continuidad para próximas tareas

1. Modificar y subir únicamente `ZeroVeCC/MapaUrbano-backend-ktor`, salvo instrucción expresa posterior del usuario.
2. Tratar Android, web y monorepo como referencias de lectura.
3. Desde el 14 de octubre, tomar Android `main`/`8c866de` o posterior como referencia; la antigua rama Stitch fue fusionada y eliminada.
4. La autenticación ciudadana vigente es por DNI, no correo.
5. Las correcciones de escritura y lectura PostGIS ya están en `main` y tienen pruebas.
6. Ejecutar pruebas unitarias e integración antes de subir cambios que afecten persistencia o rutas.
7. Subir los cambios autorizados al `main` del backend y comprobar GitHub Actions.
8. Actualizar esta constancia cuando cambie un hito estructural, una referencia revisada o el estado de integración.
9. La política solicitada el 15 de octubre exige cuenta ciudadana y visibilidad por pertenencia; hasta implementarla, las rutas públicas de reportes deben considerarse un riesgo crítico.
10. Usar `docs/REESCANEO_Y_PLAN_PRIVACIDAD_2026-10-15.md` como plan coordinado para backend, Android y web.

## 16. Conclusión técnica

El backend ya funciona como centro del recorrido ciudadano y Android dejó de ser una maqueta, pero ambos conservan el modelo anterior de mapa público y alta anónima. La prioridad inmediata es imponer pertenencia en el servidor y mover Android a un flujo de cuenta obligatoria. El panel web sigue siendo demostrativo y la administración del backend continúa bloqueada por sesiones/rutas incompletas. El proyecto se considerará integrado de extremo a extremo cuando la privacidad se haga cumplir en API, Android consuma solo recursos propios, el panel use rutas administrativas reales y una prueba con dos ciudadanos y un administrador confirme el aislamiento.

Esta constancia debe leerse junto con:

- `docs/AUTENTICACION_DNI.md` para el contrato ciudadano actual;
- `docs/INTEGRACION_REPOSITORIOS_2026-10-06.md` como revisión anterior;
- `docs/ESTADO_ACTUAL_Y_DECISIONES.md` para las decisiones del backend;
- `docs/REESCANEO_Y_PLAN_PRIVACIDAD_2026-10-15.md` para el diagnóstico de visibilidad y las tareas separadas por equipo;
- la rama `develop` del monorepo original para requisitos y alcance.
