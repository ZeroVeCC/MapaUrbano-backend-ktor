# Auditoría de los tres repositorios principales — 19 de octubre de 2026

## Regla de alcance vigente

Desde este corte, las auditorías ordinarias de Mapa Urbano incluyen únicamente:

1. backend Ktor: `ZeroVeCC/MapaUrbano-backend-ktor`;
2. aplicación Android: `Jquincal/Mapaurbano_frontend_kotlin`;
3. panel web Angular: `Santy112/MapaUrbano-frontend-web`.

El monorepo original `Jquincal/Mapa-Urbano` fue útil para reconstruir el inicio,
los requisitos y la separación del proyecto. Ya no es un componente vigente y
queda fuera de futuras auditorías, comparaciones y planes, salvo que el usuario
solicite expresamente revisarlo. No fue consultado como fuente remota en este
corte.

Solo el repositorio backend puede modificarse desde este checkout. Android y web
se descargaron en copias temporales y se trataron como fuentes de solo lectura.

## Referencias exactas

| Componente | Rama | Revisión auditada | Cambio desde el corte anterior |
|---|---|---|---|
| Backend Ktor | `main` | `49e3a52fbb76ffe4ba443557d1d03baee8f87691` | documentación de continuidad posterior al corte anterior |
| Android | `main` | `e1035852acfe98c46a629440eccc06d927fd7967` | sin cambios |
| Web Angular | `main` | `4101b24f0c1754fe94f3c3bea6f3373b3eb39699` | 3 commits posteriores a `fced3ea`, principalmente visuales y README |

No hay pull requests ni issues abiertos en ninguno de los tres repositorios. Las
ramas adicionales de backend y Android son ramas anteriores; `main` es la fuente
vigente de cada componente.

## Método y evidencia

La revisión incluyó historial y diferencias Git, código fuente, rutas, DTO,
seguridad, configuración, pruebas, documentación y workflows públicos. Se comparó
el frontend web directamente con las clases serializadas y rutas del backend; no
se asumió que un tipo TypeScript coincidía con la respuesta real solo porque
compilara.

Validaciones realizadas:

- backend: `gradlew test --rerun-tasks`, 34 pruebas, cero fallos;
- backend: GitHub Actions `39282196620`, correcto sobre `a9e418d`; ese `build`
  incluye la integración PostgreSQL/PostGIS configurada en `check`;
- resultados de integración locales del 18/10, sobre el mismo código funcional:
  17 pruebas, cero fallos;
- Android: GitHub Actions `39260551604`, correcto sobre `e103585`;
- web con Node 24: 8 pruebas correctas y build de producción correcto;
- web en GitHub: `39319377043`, `39319826468` y `39320285717` fallaron en el
  paso de pruebas y omitieron el build.

Docker Desktop no estaba iniciado durante este corte local. No se repitió
Testcontainers en la computadora; el mismo código funcional ya estaba cubierto
por los resultados locales anteriores y por el workflow verde actual.

## Resultado ejecutivo

Backend y Android mantienen alineado el recorrido ciudadano privado: DNI y
contraseña, Bearer, cuenta obligatoria, listado/detalle/imagen propios, cursor,
fotografía real y limpieza de sesión.

Web avanzó desde una integración principalmente simulada hacia servicios que
intentan usar el contrato real. El login y las lecturas básicas están más cerca
del backend, pero el panel todavía no puede considerarse integrado. Las
incompatibilidades de respuestas rompen detalle, imágenes, estadísticas,
auditoría, equipos y varias mutaciones aunque TypeScript compile.

El sistema completo aún no está listo para una demostración E2E fiable. Primero
debe corregirse el WebSocket administrativo del backend y completarse la
adaptación web. Android necesita la prueba física final y una corrección de
protección local del token.

## Backend Ktor

### Estado confirmado

- Cuenta ciudadana obligatoria y autenticación por DNI + contraseña.
- Bearer opaco persistido como hash en PostgreSQL.
- Listado, detalle e imagen propios con comprobación de pertenencia.
- Rutas globales y seguimiento anónimo retirados para ciudadanos.
- Los reportes anónimos históricos son visibles únicamente para administración.
- Login administrativo fuera del bloque protegido.
- Sesiones administrativas persistidas, cookie HttpOnly, CSRF y CORS con
  credenciales.
- Reportes administrativos, categorías, equipos, responsables, estadísticas,
  auditoría, asignaciones y mutaciones implementados.
- Control de versión para estado, prioridad y asignaciones.
- Validación de firma, tamaño y dimensiones de imágenes.
- Coordenadas PostGIS, readiness y `jsonb` verificados.
- OpenAPI cubre las rutas registradas y el CI actual está verde.

### Hallazgo B01 — cookie incompatible con el WebSocket administrativo

**Prioridad:** alta.

La cookie `mapa_urbano_admin` se configura con `Path=/api/v1/admin`, pero el
handshake está en `/api/v1/ws/admin`. Las reglas de cookies del navegador no
envían una cookie a una ruta que queda fuera de su `Path`. Por eso
`authenticate("admin-session")` no recibe la sesión cuando el panel intenta abrir
ese WebSocket.

Corrección recomendada:

- usar `Path=/api/v1`, o mover el canal a una ruta bajo `/api/v1/admin`;
- mantener HttpOnly, SameSite y Secure según el entorno;
- agregar una integración que haga login, conserve `Set-Cookie` y abra el
  handshake WebSocket como lo haría un navegador;
- comprobar 401 sin cookie y conexión correcta con cookie válida/revocada.

### Hallazgo B02 — alta de reporte e imagen en transacciones separadas

La imagen se valida antes del alta, pero la fila del reporte y la fila de imagen
se insertan en operaciones distintas. Un fallo en la segunda puede dejar un
reporte que el usuario creyó crear con evidencia, pero sin su imagen.

Debe evaluarse una operación de repositorio única que inserte ambos elementos en
la misma transacción o una compensación explícita y probada.

### Hallazgo B03 — cursores basados solo en fecha

Listados de reportes y auditoría ordenan y generan el cursor solo con
`createdAt/occurredAt`. Si dos filas comparten exactamente el mismo instante, la
siguiente página puede omitirlas al usar una comparación estrictamente menor.

El cursor debería incluir fecha e UUID, y el orden aplicar ambos campos de forma
estable. Android ya trata el cursor como opaco, por lo que el cambio no necesita
conocer su formato.

### Deuda operativa

- El bus de eventos vive en memoria y no tiene replay ni outbox.
- Falta definir alojamiento de Angular, HTTPS/WSS, secretos, backup/restore,
  observabilidad y carga.
- Faltan pruebas E2E de los tres clientes contra una sola instancia.

## Android

### Estado confirmado

Android no cambió desde `e103585` y conserva:

- login y registro por DNI;
- espera de restauración de sesión;
- uso exclusivo de `/users/me/reports` para mapa/lista, detalle e imagen;
- Bearer en todas las operaciones privadas;
- cursor opaco, carga adicional, deduplicación y protección ante cursor repetido;
- cámara, Photo Picker, FileProvider y multipart `image` real;
- validación de MIME, firma, 5 MiB y dimensiones;
- limpieza inmediata de datos privados al cerrar sesión;
- descarte de respuestas tardías y aislamiento entre cuentas A/B;
- errores diferenciados y conservación del borrador cuando corresponde;
- CI verde con pruebas, Lint, APK debug y JaCoCo.

### Hallazgo A01 — token local y reglas de backup

El Bearer se guarda sin cifrado propio en Preferences DataStore. La aplicación
tiene `allowBackup=true` y los XML excluyen el dominio `root`, pero no excluyen de
forma explícita el dominio `file` donde DataStore conserva
`files/datastore/user_session.preferences_pb`.

Antes de distribución se debe impedir que la credencial participe en backup o
transferencia de dispositivo, o moverla a almacenamiento respaldado por Android
Keystore. La corrección debe verificarse inspeccionando las reglas efectivas del
APK y una restauración de prueba.

### Hallazgo A02 — configuración release pendiente

El valor predeterminado de `API_BASE_URL` es HTTP para emulador. El permiso de
cleartext existe solo en `debug`, por lo que un release construido sin reemplazar
la URL no podrá comunicarse. El build de entrega debe inyectar una URL HTTPS real
y documentar el procedimiento reproducible.

### Pendiente de aceptación

- Instalar `e103585` en el Redmi y recorrer cámara, galería, GPS, mapa, scroll,
  alta, imagen, detalle, logout y cambio de cuenta.
- Ejecutar pruebas instrumentadas del selector, FileProvider y ciclo de vida.
- Repetir el recorrido con la web real actualizada y el mismo backend.

## Web Angular

### Delta nuevo

Desde `85832a4` se publicaron:

1. `06b6db1` — mocks HTTP en pruebas de componentes;
2. `41d217d` — rediseño institucional del login;
3. `fced3ea` — corrección del estado de envío del login.

El delta modifica 26 archivos: 843 líneas agregadas y 644 eliminadas.

### Avances reales

- El workflow dejó de pedir ChromeHeadless a Vitest.
- Login interpreta `{adminUserId, username, csrfToken}`.
- El guard restaura la sesión mediante `/admin/auth/me`.
- Se agregó un interceptor que usa `withCredentials`, adjunta
  `X-CSRF-Token` en mutaciones administrativas y limpia sesión ante 401.
- UUID, estados y prioridades principales ahora son `string` y enums correctos.
- El listado usa `cursor`, `limit`, filtros y `totalCount`.
- Reportes, categorías, estadísticas y auditoría dejaron de devolver varios
  arreglos simulados dentro de sus servicios.
- El mapa consulta reportes administrativos reales y crea popups sin `innerHTML`.
- Las 8 pruebas unitarias pasan localmente con Node 24 y el build se genera.

### Hallazgo W01 — CI sigue rojo por Node incompatible

El workflow fija `node-version: 20`. El `package-lock.json` actual contiene
dependencias de Angular 22 que exigen Node `^22.22.3 || ^24.15.0 || >=26` y otras
que exigen al menos `22.18` o `24.11`. Las tres ejecuciones nuevas fallan en
`Run unit tests` con código 3 y omiten el build.

Cambios requeridos:

- usar Node 24 LTS compatible, o como mínimo 22.22.3;
- actualizar las acciones que GitHub marca por runtime Node 20 obsoleto;
- mantener `npm ci`, pruebas y build como pasos obligatorios;
- comprobar un workflow verde en `main`.

### Hallazgo W02 — un único `AdminReport` mezcla tres contratos

El backend devuelve:

- `ReportAdminSummary` en el listado;
- `ReportAdminDetail` en el detalle;
- `ReportMutationResponse` al cambiar estado/prioridad;
- HTTP 204 al asignar o desasignar.

Web declara todos como `AdminReport`. Como consecuencia:

- el listado intenta mostrar `description`, que no existe en el resumen;
- el detalle visual abre el resumen sin llamar `getReportDetail()`;
- `updateStatus()` reemplaza el reporte completo por una respuesta parcial y
  pierde título, coordenadas y categoría;
- asignar/desasignar espera un JSON inexistente después del 204.

Se necesitan tipos separados y, después de cada mutación, fusionar únicamente los
campos presentes o volver a consultar el detalle/listado.

### Hallazgo W03 — detalle, fotografía y asignación no están conectados

- `getReportDetail()` existe, pero ninguna pantalla lo llama.
- `getReportImageBlob()` existe, pero la UI usa siempre `/bache.jpg` cuando no
  tiene `imageUrl`; esto puede presentar una evidencia falsa incluso cuando el
  reporte no tiene foto.
- Cambiar la cuadrilla solo modifica `assignedTeamId` en memoria; no ejecuta
  `assignTeamAndUser()`.
- No se cargan responsables desde `/admin/assignees`.
- Prioridad y `dueAt` tienen métodos de servicio, pero no controles operativos.
- No existe acción de desasignación real en la interfaz.

### Hallazgo W04 — estadísticas y auditoría usan formas equivocadas

El backend entrega `byStatus` y `byPriority` con claves enum en mayúsculas. Web
busca `pending`, `inProgress`, `resolved`, `urgent`, etc., por lo que muestra
ceros. `byCategory` y `byTeam` son listas de objetos; web los declara como mapas.

Auditoría espera `timestamp`, `actor`, `role`, `module`, `details` e `ip`, pero el
backend devuelve `occurredAt`, IDs de actor, `entityType`, `entityId`, `metadata`
y `sourceIp`. La tabla queda vacía o con valores indefinidos y la búsqueda puede
fallar al invocar `toLowerCase()` sobre `details` inexistente.

### Hallazgo W05 — categorías y equipos aún no respetan el contrato

- Crear categoría envía `isActive`, un campo que `CreateCategoryRequest` no
  acepta. El backend usa `ignoreUnknownKeys=false`, por lo que esa alta responde
  400.
- Equipos espera `specialty` y `membersCount`; el backend devuelve `description`
  y `memberCount`.
- El servicio de equipos conserva cuatro equipos ficticios y convierte errores
  de lectura o actualización en éxito simulado.

Los request DTO deben separarse de los response DTO y todos los fallbacks deben
eliminarse del flujo productivo.

### Hallazgo W06 — WebSocket no está integrado

`AdminRealtimeService` existe, pero ningún componente llama `connect()` ni
`disconnect()` al iniciar/cerrar sesión. Solo reacciona a dos clases de evento y
no fuerza una recarga completa tras reconectar. Además, hoy lo bloquea B01 en el
backend.

Después de corregir la cookie debe conectarse una sola vez por sesión, cerrarse
en logout, restablecer la intención de reconexión al ingresar nuevamente y volver
a consultar REST después de toda reconexión.

### Hallazgo W07 — manejo de errores y pruebas insuficientes

- El interceptor centraliza solo 401.
- El mensaje `errorMessage` del listado se calcula, pero la plantilla no lo
  muestra.
- Categorías, estadísticas y auditoría pueden quedar en estado de carga tras un
  error.
- No hay tratamiento uniforme para 400, 403, 404, 409, 429 y 5xx.
- Las 8 pruebas solo comprueban creación de componentes; no validan URL, DTO,
  cookie, CSRF, paginación, respuestas 204, conflicto 409 ni errores.

Se requieren pruebas HttpTesting de cada contrato, pruebas del interceptor/guard
y al menos un E2E real de login, listado, detalle, imagen y mutaciones.

### Hallazgo W08 — afirmaciones de seguridad sin evidencia

El login muestra de forma fija “TLS 1.3 / AES-256”, certificación ISO/IEC 27001,
ENS, un ID de sesión y latencia de 14 ms. El entorno local funciona por HTTP y no
hay evidencia versionada de esas certificaciones o mediciones. Esas leyendas
deben retirarse o reemplazarse por información obtenida y acreditada realmente.

## Matriz de compatibilidad actual

| Operación | Backend | Android | Web | Resultado |
|---|---|---|---|---|
| DNI/Bearer ciudadano | implementado | alineado | no aplica | correcto |
| Privacidad por pertenencia | implementada | alineada | admin global | correcto en código |
| Foto ciudadana | implementada | alineada | admin no la muestra | parcial |
| Login admin/cookie/CSRF | implementado | no aplica | parcialmente alineado | falta E2E |
| Listado admin y mapa | implementado | no aplica | campos básicos compatibles | parcial funcional |
| Detalle admin | implementado | no aplica | servicio sin uso | pendiente |
| Imagen admin | implementada | no aplica | se usa imagen ficticia | incorrecto |
| Estado/prioridad | implementados | lectura | estado corrompe el modelo; prioridad sin UI | incorrecto |
| Asignación | implementada, 204 | no aplica | solo cambio local/espera JSON | incorrecto |
| Equipos/responsables | implementados | no aplica | DTO y fallback incorrectos | incorrecto |
| Categorías | implementadas | lectura pública | alta envía campo extra | parcial |
| Estadísticas | implementadas | no aplica | forma y claves incorrectas | incorrecto |
| Auditoría | implementada | no aplica | DTO incorrecto | incorrecto |
| WebSocket admin | ruta implementada | no aplica | no conectado; cookie no llega | bloqueado |
| CI | verde | verde | rojo | sistema incompleto |

## Orden de implementación recomendado

### Backend

1. Corregir B01 y probar el handshake WebSocket con cookie real.
2. Hacer robustos los cursores con fecha + UUID.
3. Evaluar transacción única para reporte + imagen.
4. Definir despliegue, secretos, HTTPS/WSS, backup y observabilidad.

### Web

1. Cambiar CI a Node 24 y obtener verde.
2. Separar todos los DTO según OpenAPI/backend.
3. Corregir estado/prioridad/204 y recargar datos tras mutaciones.
4. Implementar detalle e imagen reales sin fotografías ficticias.
5. Conectar equipos, responsables, asignación, prioridad y fecha objetivo.
6. Corregir estadísticas, auditoría y creación de categorías.
7. Retirar mocks, éxitos simulados y afirmaciones de seguridad no acreditadas.
8. Conectar WebSocket después de B01.
9. Añadir pruebas de contrato y E2E.

### Android

1. Excluir el token de backup/transferencia o protegerlo con Keystore.
2. Definir URL HTTPS y procedimiento de build release.
3. Ejecutar validación física e instrumentada en el Redmi.

### Aceptación conjunta

1. Ciudadano A crea un reporte con imagen y solo ve sus datos.
2. Logout elimina la información antes de esperar la red.
3. Ciudadano B no ve información de A en el mismo dispositivo.
4. Administrador inicia sesión, ve A/B e históricos anónimos.
5. Web abre detalle e imagen reales y pagina sin perder filas.
6. Administrador cambia estado, prioridad, fecha y asignación.
7. Un conflicto de versión produce 409 y recarga controlada.
8. Android A recupera el cambio al refrescar.
9. WebSocket autentica, se cierra en logout y recupera REST al reconectar.
10. Ningún fallo HTTP se presenta como operación exitosa.
11. Backend, Android y web terminan con CI verde.

## Estado final del corte

El flujo ciudadano backend–Android está alineado en código y pendiente de prueba
física. La administración del backend está implementada, pero su WebSocket tiene
un defecto de alcance de cookie. Web mejoró autenticación y lecturas básicas, pero
todavía necesita una corrección estructural de DTO y operaciones antes de poder
validar el sistema completo.

## Actualización posterior del mismo día

La auditoría web se repitió sobre `4101b24`. Los cambios nuevos ajustan el
dashboard y declaran integración completa en README, pero no corrigen los
contratos pendientes. Pruebas y build pasan localmente con Node 24; GitHub Actions
`39334403684` sigue rojo porque el workflow usa Node 20.

Los hallazgos backend B01, B02 y B03 se corrigieron en la rama pendiente
`codex/auditoria-web-ajustes-backend`: cookie compatible con WebSocket y prueba de
handshake, reporte e imagen en una transacción, y cursores fecha+UUID para reporte
propio, reporte administrativo y auditoría. También se endurecieron sesiones de
administradores desactivados, equipos, miembros, categorías y filtros. Las 41
pruebas unitarias y 19 integraciones PostgreSQL/PostGIS terminaron correctamente.

El detalle actualizado y la lista autocontenida para el equipo web están en
`docs/AUDITORIA_WEB_Y_CIERRE_BACKEND_2026-10-19.md`. Esta actualización prevalece
sobre B01–B03 y las referencias web anteriores de este documento.

## Cierre publicado en `main` — 20 de octubre de 2026

El cambio funcional preparado como `72701ee` quedó integrado en la historia final
de `main` como `ac59243`, después de conservar el aporte documental `b0478e4` de
Mauro. Incluye las correcciones B01, B02 y B03: cookie
compatible con el WebSocket administrativo, persistencia atómica de reporte e
imagen y cursores estables fecha+UUID. También incorpora las validaciones
administrativas y pruebas descritas en la actualización anterior.

Antes de promoverlo se repitieron las validaciones con JDK 21.0.10, Gradle 9.5.0,
Docker Desktop y PostgreSQL/PostGIS mediante Testcontainers. Pasaron 41 pruebas
unitarias y 19 integraciones, sin fallos, errores ni pruebas omitidas. `gradlew
check` finalizó correctamente.

La constancia de entrega y el contrato esperado para la prueba conjunta se
encuentran en `docs/ENTREGA_BACKEND_MAIN_2026-10-20.md`. Esta sección reemplaza
las menciones anteriores que describen B01–B03 como pendientes del backend.
