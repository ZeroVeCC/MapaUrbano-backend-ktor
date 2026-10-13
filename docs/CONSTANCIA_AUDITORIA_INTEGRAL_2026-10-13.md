# Constancia de auditoría integral de Mapa Urbano

**Fecha de corte:** 13 de octubre de 2026  
**Responsable de esta constancia:** revisión técnica realizada sobre los repositorios publicados de Mapa Urbano  
**Repositorio donde se conserva:** `ZeroVeCC/MapaUrbano-backend-ktor`  
**Propósito:** dejar una referencia verificable de qué existe, qué fue probado, qué está integrado y qué falta para que los tres componentes funcionen juntos.

> Esta constancia describe el estado observado en las revisiones exactas indicadas abajo. Una rama distinta o un cambio posterior puede modificar las conclusiones. Los repositorios de Android, web y el monorepo original se analizaron en copias temporales de solo referencia; esta auditoría únicamente modifica el repositorio del backend.

## 1. Resultado general

El proyecto tiene una base técnica amplia, pero los tres repositorios todavía no forman una aplicación integrada de extremo a extremo.

- El **backend Ktor** es el componente más avanzado. Compila, sus 43 pruebas pasan, levanta PostgreSQL/PostGIS con Testcontainers y ya resolvió la escritura y lectura de coordenadas. El flujo ciudadano por DNI y gran parte del dominio están implementados.
- El **frontend Android publicado en `main`** es una maqueta inicial. La interfaz más completa está en la rama `codex/frontend-stitch`, todavía sin fusionar a `main`, y continúa usando datos simulados. No contiene cliente HTTP, permiso de Internet, mapa real, cámara ni ubicación real. Su autenticación visual todavía usa correo.
- El **frontend web** compila para producción y contiene un panel visual completo, pero todos sus datos son locales. No tiene integración HTTP, WebSocket, login real, guardas ni manejo de sesión. Tres de sus ocho pruebas fallan.
- El **monorepo original** sigue siendo la fuente del alcance, arquitectura, requisitos y decisiones iniciales. Sus ramas posteriores no están consolidadas en `main`; por eso no debe tomarse `main` de ese repositorio como el estado actual del producto.
- La principal barrera de integración ya no es PostGIS. Es la falta de un contrato compartido y consumido por Android y web, junto con la autenticación administrativa incompleta del backend.

## 2. Alcance y método de revisión

Se revisaron estos repositorios y referencias remotas:

| Componente | Repositorio | Referencia revisada | Revisión |
|---|---|---|---|
| Backend Ktor | `ZeroVeCC/MapaUrbano-backend-ktor` | `main` | `b2dbaa026f82377008e23e13d448dced61a0abce` |
| Android standalone | `Jquincal/Mapaurbano_frontend_kotlin` | `main` | `b61792e` |
| Android, interfaz completa | mismo repositorio | `codex/frontend-stitch` | `0a26dd4` |
| Frontend web | `Santy112/MapaUrbano-frontend-web` | `main` | `7ba06ad` |
| Monorepo original | `Jquincal/Mapa-Urbano` | `main` | `13150ab` |
| Documentación original vigente | mismo repositorio | `develop` | `9b28a7e` |
| Backend anterior dentro del monorepo | mismo repositorio | `feat/MU-004-backend-ktor-exposed` | `a04651d` |
| Android anterior dentro del monorepo | mismo repositorio | `codex/android-frontend-stitch` | `e96cbf7` |

La revisión incluyó:

1. inventario de archivos versionados y conteo por tecnología;
2. lectura del código fuente, configuración, migraciones, documentación y pruebas;
3. comparación de ramas y cronología de commits;
4. inspección de pull requests y ejecuciones de GitHub Actions visibles el 13 de octubre de 2026;
5. compilación y pruebas locales donde el entorno lo permitió;
6. comparación de contratos, modelos, categorías, estados, identificadores y autenticación entre componentes.

Los binarios se verificaron por ubicación, tipo y tamaño, sin pretender interpretar su contenido byte por byte. Esto se aplica principalmente a `gradle-wrapper.jar`, fuentes TTF e imágenes. Se excluyeron del análisis funcional directorios generados como `.git`, `build`, `node_modules` y cachés.

## 3. Tamaño observado

| Repositorio/referencia | Archivos relevantes | Código principal observado |
|---|---:|---|
| Backend `main` | 127 | 106 archivos Kotlin, 5.658 líneas; 3 migraciones SQL; OpenAPI; Docker; CI |
| Android `main` | 50 | 18 archivos Kotlin, 978 líneas |
| Android `codex/frontend-stitch` | 60 | 23 archivos Kotlin, 4.933 líneas; 4 fuentes Inter |
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
- revisar siempre la rama Android indicada, porque su `main` y Stitch difieren mucho;
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
| `GET /swagger` | implementada | expone documentación, actualmente incompleta |
| `GET /api/v1/categories` | implementada | catálogo desde PostgreSQL |
| `GET /api/v1/reports` | implementada/parcial | lista pública y bbox; cursor sin aplicar |
| `POST /api/v1/reports` | implementada/parcial | multipart, cuenta o anónimo, imagen con limitaciones |
| `GET /api/v1/reports/{id}` | implementada/parcial | detalle público, historial incompleto |
| `GET /api/v1/reports/{id}/image` | implementada | binario desde base de datos |
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

Verificación local del 13 de octubre de 2026:

| Grupo | Resultado |
|---|---:|
| unitarias | 28/28 correctas |
| integración PostgreSQL/PostGIS | 15/15 correctas |
| total | **43/43 correctas, 0 fallos, 0 omitidas** |

Docker Desktop 29.8.1 se inició correctamente y Testcontainers ejecutó la integración real. La última ejecución observada de GitHub Actions en `main`, identificador `37711945400`, también finalizó correctamente.

La suite cubre CORS, errores de persistencia, rutas base, DNI, login/logout/baja, migraciones, restricciones, autoría anónima y autenticada, tipos geográficos, lectura de coordenadas, metadatos de imagen, asignaciones, borrado lógico, eventos, consulta por código, cambio atómico de estado y creación de reportes.

No cubre todavía el flujo administrativo HTTP, sesiones admin reales, procesamiento real de imágenes ni un recorrido completo desde Android/web.

### 6.8. Deuda y riesgos del backend

#### Prioridad crítica

1. **Autenticación administrativa inutilizable:** todo el grupo `/admin`, incluido el login, está dentro de `authenticate("admin-session")`; una persona no autenticada no puede iniciar sesión.
2. **Falta instalar/configurar `Sessions`:** el código usa `call.sessions`, pero no hay transporte de cookie, firma ni configuración efectiva.
3. **Validador admin incompleto:** confía en la sesión deserializada y no la contrasta con una sesión persistida.
4. **Sesión admin en memoria:** `AdminSessionRepositoryImpl` usa `ConcurrentHashMap`; un reinicio la elimina.
5. **No hay administrador inicial:** no existe seed ni mecanismo documentado de bootstrap.
6. **No hay integración cliente-servidor comprobada:** ningún frontend publicado consume este backend.

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
10. Swagger/OpenAPI documenta casi solo autenticación ciudadana.

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

## 7. Android: estado completo

### 7.1. `main`

El `main` standalone es una primera versión de Compose:

- actividad principal y navegación básica;
- mapa representado como placeholder;
- formulario, Mis reportes y seguimiento;
- dos reportes de muestra mediante `FakeReportRepository`;
- sin REST, autenticación, GPS, cámara ni SDK cartográfico.

Su README reconoce estas funciones como pendientes. Esta rama no representa el trabajo visual más avanzado.

### 7.2. Rama `codex/frontend-stitch`

Esta rama agrega:

- navegación entre mapa/lista, detalle, formulario y confirmación;
- filtros combinados por texto, estado y categoría;
- elección explícita entre reporte con cuenta y anónimo;
- simulación de fotografía, permisos y ubicación;
- código de seguimiento e historial visual;
- login, registro, perfil y Mis reportes;
- estados de carga, vacío y error;
- persistencia del borrador durante ciertos cambios de navegación;
- sistema visual, componentes comunes y fuentes Inter.

Archivos principales observados:

| Grupo | Función |
|---|---|
| `MainActivity.kt`, `MapaUrbanoApp.kt` | entrada, tema, estado y navegación de alto nivel |
| `core/data/AppViewModel.kt` | estado global, filtros, sesión y simulación de altas |
| `core/data/DemoContent.kt` | usuarios, categorías y reportes falsos |
| `core/data/ReportRepository.kt` | repositorio simulado |
| `core/model/Models.kt` | modelos de UI y borradores |
| `core/navigation/*` | destinos y barras de navegación |
| `core/design/*` | tema, colores y tipografía |
| `feature/common/StitchUi.kt` | piezas compartidas de interfaz |
| `feature/map/*` | mapa ilustrado, lista, filtros y estados |
| `feature/report/*` | formulario y confirmación |
| `feature/tracking/*` | ingreso de código y resultado |
| `feature/auth/*` | login y registro visuales |
| `feature/myreports/*` | perfil, lista propia y detalle |

### 7.3. Lo que todavía es simulado

- `FakeReportRepository` y `DemoContent` son la fuente de datos.
- El alta espera unos milisegundos y genera un ID `REP-...` en memoria.
- El seguimiento busca códigos en la colección local.
- Las coordenadas del mapa son puntos dibujados en una ilustración.
- Ubicación, foto, permisos y errores son estados simulados.
- No hay Retrofit, cliente Ktor, OkHttp ni otro cliente HTTP.
- El manifiesto no declara `INTERNET`.
- No hay mapa real, cámara ni proveedor de ubicación.

### 7.4. Diferencias con el contrato actual

- La cuenta usa `email` en el modelo y en las pantallas; el backend requiere DNI.
- La interfaz incluso muestra el DNI como campo “no disponible”.
- Las categorías locales son `roads`, `lighting`, `waste`, `public-space`, `obstruction`, `signage`, `water` y `other`; el backend publica `bache`, `luminaria`, `basura`, `vandalismo`, `inundacion` y `otro`.
- Los IDs locales `REP-...` no coinciden con UUID del backend.
- Los estados son enums Kotlin de interfaz y todavía no existe serialización común.
- No hay tratamiento de HTTP 400, 401, 409, 429 y 500.
- La rama no está fusionada en el `main` standalone.

### 7.5. Defecto visual confirmado

`MapScreen` retorna antes de construir `StitchMiniMap` cuando la colección está vacía. Eso oculta el mapa con cero reportes y también cuando un filtro no encuentra coincidencias. La solución pertenece a Android: el mapa debe permanecer montado, sin marcadores, y el estado vacío debe mostrarse junto con la acción para reportar.

### 7.6. Pruebas Android

La rama Stitch contiene cinco pruebas unitarias y una instrumentada. Cubren confirmación anónima, preservación del borrador al iniciar sesión, normalización de código, sesión vencida y prevención de doble envío. La versión equivalente del PR #12 en el monorepo tuvo CI correcto.

No se observó una ejecución remota de CI para la rama Stitch del repositorio standalone: su workflow solo reacciona a `main` y `develop`. Tampoco hay pruebas de red, contrato REST, cámara, ubicación, mapa real o backend integrado.

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

La compilación de producción del 13 de octubre fue correcta:

- bundle inicial: 408,88 kB sin comprimir;
- transferencia estimada: 92,11 kB;
- advertencia: `reports.css` pesa 4,27 kB y excede en 272 bytes el presupuesto de 4 kB.

Resultado de pruebas:

- 8 pruebas totales;
- 5 correctas;
- 3 fallidas.

Los fallos son dos pruebas de `App` y una de `Sidebar`, inicialmente por no proveer `ActivatedRoute`. Además, una aserción de título espera `Hello, mapa-urbano`, texto que ya no existe en `app.html`. Son pruebas desactualizadas respecto de la navegación actual.

## 9. Compatibilidad entre los tres componentes

| Tema | Backend | Android Stitch | Web | Consecuencia |
|---|---|---|---|---|
| identidad ciudadana | DNI + contraseña | correo + contraseña, simulado | no aplica al vecino | Android debe migrar a DNI |
| administrador | sesión pretendida, incompleta | no aplica | sin login | panel no puede autenticarse |
| fuente de datos | PostgreSQL/PostGIS | memoria/demo | constantes locales | no hay flujo integrado |
| categorías | 6 slugs en español singular | 8 IDs ingleses | 5 categorías locales | filtros/altas no coinciden |
| ID de reporte | UUID | `REP-...` | números | modelos deben alinearse |
| tracking | código opaco del servidor | código local | no implementado | Android debe usar endpoint real |
| estados | `pending`, `in_progress`, `resolved` en DB; DTO debe fijarse | enum `Pending/InProgress/Resolved` | etiquetas españolas locales | falta contrato de serialización probado |
| prioridad | enum backend | no modelada de forma operativa | visual/parcial | panel no puede administrarla aún |
| coordenadas | PostGIS, latitud/longitud reales | mapa ilustrado | marcadores fijos | ningún cliente consume ubicación real |
| fotografías | BYTEA + endpoint específico | simuladas | archivos públicos locales | falta carga/descarga real |
| errores HTTP | respuestas API parciales | mensaje genérico/simulado | sin cliente | falta manejo por código |
| tiempo real | WebSocket en memoria | sin cliente | sin cliente | actualizaciones no llegan a UI |
| paginación | incompleta | no consume | no consume | definir antes de conectar listados |

### Contratos que deben congelarse antes de integrar

1. DTO exacto de categoría y uso estable de `slug`/`id`.
2. JSON de lista: `{ "items": [...], "nextCursor": ... }`.
3. serialización exacta de estado y prioridad;
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
| RF-01 | mapa Android con reportes | parcial | mapa ilustrado y datos demo; sin SDK/API |
| RF-02 | iniciar desde punto del mapa | parcial | selección simulada |
| RF-03 | formulario completo y foto | parcial | UI presente; envío/foto reales pendientes |
| RF-04 | detalle y estado ciudadano | parcial | UI y API separadas; detalle propio 501 |
| RF-05 | filtros por categoría/estado | parcial | UI local; backend parcial; sin integración |
| RF-06 | admin cambia estado | parcial/bloqueado | operación atómica existe; auth admin impide flujo real |
| RF-07 | tabla paginada y mapa admin | parcial | UI demo; API/listado admin pendientes |
| RF-08 | totales/distribuciones | parcial | UI fija; backend devuelve vacío |
| RF-09 | foto binaria y metadatos | parcial | esquema/repositorio existen; validación/atomicidad incompletas |
| RF-10 | eliminar con auditoría | parcial | use case existe; flujo y transacción completa pendientes |
| RF-11 | código para anónimo | cumplido en backend | probado; cliente real pendiente |
| RF-12 | consultar por código | cumplido en backend/parcial producto | endpoint probado; Android simula |
| RF-13 | evento WS tras commit | parcial | estado publica evento; bus no persistente y clientes ausentes |
| RF-14 | Ktor sirve Angular | pendiente | no hay static content del build web |
| RF-15 | asignar/reasignar | parcial | dominio/repositorio; UI/API real incompletas |
| RF-16 | prioridad y fecha objetivo | parcial | dominio/esquema; flujo admin no operativo |
| RF-17 | historial de asignación/prioridad | parcial | tablas existen; garantías transaccionales incompletas |
| RF-18 | registro/login/sesión/logout vecino | cumplido en backend | DNI probado; Android no integrado |
| RF-19 | listar y consultar reportes propios | parcial | listado existe con defectos; detalle 501 |
| RF-20 | elegir cuenta o anónimo | cumplido en backend/parcial producto | UI simula; backend lo prueba |
| RF-21 | autor desde sesión, no cliente | cumplido en backend | cubierto por implementación/pruebas |

## 11. Matriz de requisitos no funcionales

| Req. | Estado | Observación |
|---|---|---|
| RNF-01, mapa ≤3 s/200 reportes | pendiente | sin mapa/API integrados ni prueba de carga |
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
| RNF-13, secretos no expuestos | parcial alto | BCrypt/token hash correctos; defaults de desarrollo y admin pendientes |
| RNF-14, privacidad pública/WS | parcial | DTO públicos no deben filtrar identidad; falta prueba integral WS |

## 12. Ruta recomendada desde este estado

Esta secuencia evita que cada equipo construya contra supuestos diferentes.

### Fase 0 — fijar referencias y contrato

- [ ] Decidir si `codex/frontend-stitch` se fusiona al `main` Android standalone.
- [ ] Publicar un OpenAPI completo desde el backend.
- [ ] Definir ejemplos reales de request/response y errores.
- [ ] Alinear categoría, estado, prioridad, UUID, tracking, cursor e imagen.
- [ ] Añadir una colección de pruebas de contrato o fixtures compartidos.

**Salida:** los dos frontends pueden generar modelos contra un contrato estable.

### Fase 1 — cerrar el flujo ciudadano del backend

- [ ] Rechazar coordenadas ausentes/no numéricas en lugar de convertirlas a cero.
- [ ] Corregir cursores y mapeo de categoría/imagen en reportes propios.
- [ ] Implementar detalle propio.
- [ ] Completar `hasImage` e historial público.
- [ ] Hacer atómica la creación de reporte + imagen.
- [ ] Validar firma, tamaño y dimensiones de archivos sin cargar innecesariamente todo en memoria.

**Salida:** registro, login, alta anónima/asociada, lista, detalle, imagen y seguimiento estables.

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

- [ ] Fusionar/rebasar la rama visual elegida.
- [ ] Cambiar correo por DNI.
- [ ] Agregar cliente HTTP, permiso de Internet y almacenamiento seguro del token.
- [ ] Consumir categorías, mapa/lista, alta multipart, detalle, Mis reportes y tracking.
- [ ] Mantener visible el mapa cuando no hay resultados.
- [ ] Diferenciar red, 400, 401, 409, 429 y 500.
- [ ] Incorporar mapa, ubicación y cámara reales.
- [ ] Probar contra Docker en dispositivo/emulador, incluyendo IP local y CORS/red Android.

**Salida:** recorrido ciudadano real en un dispositivo.

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

1. contrato API completo;
2. autenticación admin;
3. endpoints admin sin 501;
4. Android real por DNI;
5. web real;
6. E2E con Docker;
7. seguridad, rendimiento y despliegue.

No conviene seguir ampliando pantallas simuladas antes de cerrar los puntos 1 a 3, porque aumenta la cantidad de modelos y comportamientos que luego habrá que reemplazar.

## 14. Evidencia reproducible

### Backend

```powershell
./gradlew.bat test --rerun-tasks
./gradlew.bat integrationTest --rerun-tasks
./gradlew.bat build --rerun-tasks
```

Las pruebas de integración requieren Docker activo porque usan Testcontainers con PostgreSQL/PostGIS.

### Web

En la copia temporal de auditoría se utilizó pnpm por no estar `npm` disponible en PATH:

```powershell
pnpm install
pnpm exec ng build --configuration production
pnpm exec ng test --watch=false
```

La compilación pasó y las pruebas quedaron 5/8, con los fallos documentados en la sección 8.4.

### GitHub

Se verificaron ramas, commits, pull requests y ejecuciones mediante metadatos públicos de GitHub. El estado corresponde al momento de la auditoría y puede cambiar.

## 15. Reglas de continuidad para próximas tareas

1. Modificar y subir únicamente `ZeroVeCC/MapaUrbano-backend-ktor`, salvo instrucción expresa posterior del usuario.
2. Tratar Android, web y monorepo como referencias de lectura.
3. Antes de afirmar que una función de Android está o no está, indicar si se observó `main` o `codex/frontend-stitch`.
4. La autenticación ciudadana vigente es por DNI, no correo.
5. Las correcciones de escritura y lectura PostGIS ya están en `main` y tienen pruebas.
6. Ejecutar pruebas unitarias e integración antes de subir cambios que afecten persistencia o rutas.
7. Subir los cambios autorizados al `main` del backend y comprobar GitHub Actions.
8. Actualizar esta constancia cuando cambie un hito estructural, una referencia revisada o el estado de integración.

## 16. Conclusión técnica

El backend tiene suficiente base para convertirse en el centro del sistema, y su persistencia geográfica crítica está funcionando. La siguiente etapa debe transformar los contratos y módulos parciales en flujos cerrados. Android y web muestran bien la intención del producto, pero el código publicado sigue siendo principalmente demostrativo. El proyecto se considerará integrado cuando ambos clientes reemplacen los datos locales por la misma API, la administración pueda autenticarse y ninguna ruta necesaria responda 501.

Esta constancia debe leerse junto con:

- `docs/AUTENTICACION_DNI.md` para el contrato ciudadano actual;
- `docs/INTEGRACION_REPOSITORIOS_2026-10-06.md` como revisión anterior;
- `docs/ESTADO_ACTUAL_Y_DECISIONES.md` para las decisiones del backend;
- la rama `develop` del monorepo original para requisitos y alcance.
