# Auditoría del frontend web — 21 de octubre de 2026

## 1. Alcance y regla de trabajo

Se auditó únicamente el repositorio web vigente:

- repositorio: `Santy112/MapaUrbano-frontend-web`;
- rama: `main`;
- commit: `4101b24f0c1754fe94f3c3bea6f3373b3eb39699`;
- último commit: `Merge pull request #1 from Santy112/implementacion`;
- fecha del último commit: 19 de octubre de 2026.

El repositorio web se usó como referencia de solo lectura. No se modificó ni se
publicó nada allí. El contrato de comparación fue el OpenAPI y los DTO del backend
Ktor en `main` `0c2ee21f2c35dcde44fe3e43cb7ef15c6c462815`. El monorepo original no
formó parte del análisis.

No hay cambios web nuevos desde el corte del 19 de octubre: el `main` remoto sigue
en `4101b24`. Tampoco hay pull requests ni issues abiertos en GitHub al momento de
esta revisión.

## 2. Verificaciones ejecutadas

Se creó una copia limpia temporal del repositorio y se revisaron los 61 archivos
versionados, incluyendo configuración Angular, workflow, modelos, servicios,
guard, interceptor, componentes, plantillas y pruebas.

Resultados reproducidos:

| Verificación | Entorno | Resultado |
|---|---|---|
| Instalación desde `package-lock.json` | Node 20.19.5, npm 10.8.2 | correcta, con advertencias de engine |
| Pruebas `ng test --watch=false` | Node 20.19.5 | 7 suites, 8 pruebas, todas correctas |
| Build de producción | Node 20.19.5 | correcto, 375.37 kB iniciales |
| Pruebas `ng test --watch=false` | Node 24.19.0 | 7 suites, 8 pruebas, todas correctas |
| Build de producción | Node 24.19.0 | correcto, 363.84 kB iniciales |
| `npm audit` | lock vigente | 6 vulnerabilidades: 1 baja, 2 moderadas y 3 altas |

El intento adicional de reproducir Linux con Docker no pudo ejecutarse porque el
motor de Docker Desktop no estaba iniciado. Esto no invalida los resultados de
Windows, pero deja pendiente una reproducción local idéntica a `ubuntu-latest`.

GitHub Actions mantiene como último resultado el workflow
[`39334403684`](https://github.com/Santy112/MapaUrbano-frontend-web/actions/runs/39334403684),
fallido para el mismo commit `4101b24`. La instalación terminó correctamente y la
etapa `Run unit tests` finalizó con código 3; el build quedó omitido. GitHub no
expone el log completo sin permisos administrativos. Las anotaciones públicas
solo muestran el código de salida y la advertencia de deprecación de Node 20 en
las acciones.

Aunque el error no se reprodujo hoy en Windows, el workflow sigue configurado con
Node 20 y el lock instala Angular 22.2.1, cuyas dependencias declaran Node
`^22.22.3 || ^24.15.0 || >=26`. Por eso Node 20 es un entorno no soportado y debe
reemplazarse por Node 24. El workflow también debe volver a ejecutarse hasta quedar
verde.

## 3. Estado que sí está bien implementado

- Login administrativo contra `/api/v1/admin/auth/login`.
- Recuperación de sesión con `/api/v1/admin/auth/me`.
- Cookie incluida mediante `withCredentials` en las rutas administrativas.
- Conservación y envío de `X-CSRF-Token` en POST, PUT, PATCH y DELETE.
- Limpieza de sesión y redirección al recibir HTTP 401.
- Guard de rutas administrativas.
- Listado administrativo básico con filtros de estado y búsqueda.
- Cambio de estado con `version` y recarga ante conflicto HTTP 409.
- Baja de reportes contra la ruta administrativa.
- Mapa con coordenadas del resumen administrativo y construcción segura del popup
  mediante `textContent`.
- Rutas y servicios iniciales para categorías, estadísticas, auditoría, equipos y
  WebSocket.
- Build de producción dentro de los presupuestos configurados.

Estos avances permiten iniciar sesión y realizar una lectura administrativa
básica, pero no constituyen todavía una integración completa del panel.

## 4. Hallazgos bloqueantes

### W01 — un solo DTO mezcla resumen, detalle y respuesta de mutación

`AdminReport` se usa para tres respuestas incompatibles:

- el listado devuelve `ReportAdminSummary`, sin descripción, modo de envío,
  dirección, IDs de asignación ni historiales;
- el detalle devuelve `ReportAdminDetail`, con `assignment` anidado;
- estado y prioridad devuelven `ReportMutationResponse`, con solo ID, estado,
  prioridad, fecha objetivo, versión y fecha de actualización.

La plantilla intenta leer `description`, `address`, `imageUrl`,
`submissionMode` y `assignedTeamId` directamente del resumen. Esos campos no
existen en esa respuesta. Además, el servicio reemplaza el objeto completo del
listado con la respuesta reducida de una mutación, por lo que después de cambiar
el estado pueden desaparecer título, categoría, coordenadas y fechas en la
interfaz.

**Corrección requerida:** definir tipos separados para resumen, detalle, mutación,
asignación e historiales. Después de una mutación se debe fusionar únicamente el
resultado compatible o volver a consultar el detalle/listado.

### W02 — el detalle y la fotografía reales no se usan

`getReportDetail()` y `getReportImageBlob()` existen, pero la pantalla no los
invoca. `verDetalle()` abre el objeto resumido y la imagen siempre usa
`imageUrl || '/bache.jpg'`. El backend no entrega `imageUrl`; entrega `hasImage` y
la imagen binaria en `/api/v1/admin/reports/{id}/image`.

El resultado actual muestra datos incompletos y puede presentar una foto de bache
ficticia para reportes sin imagen o de otra categoría.

**Corrección requerida:** al abrir el modal, solicitar el detalle; si `hasImage`
es verdadero, descargar el blob autenticado, crear un object URL y revocarlo al
cerrar o destruir el componente. Si no hay imagen, mostrar un estado neutral.

### W03 — la asignación solo cambia memoria local

El selector de cuadrilla ejecuta:

```ts
reporte.assignedTeamId = teamId;
```

No llama a `PUT /assignment`, no envía `version`, no selecciona responsable y no
trata 204 ni 409. La asignación desaparece al recargar. Tampoco se consume
`GET /api/v1/admin/assignees`.

El método de servicio existente también modela incorrectamente la operación: hace
obligatorios `teamId` y `responsibleAdminUserId` y espera un `AdminReport`, cuando
el backend admite valores nulos, exige `version` y responde HTTP 204 sin JSON.

**Corrección requerida:** cargar equipos y responsables reales; enviar el DTO de
asignación correcto; tratar 204 como éxito; volver a consultar el detalle; agregar
desasignación y conflicto 409.

### W04 — estadísticas son incompatibles con la respuesta real

El backend devuelve:

- claves `PENDING`, `IN_PROGRESS`, `RESOLVED` y prioridades en mayúsculas;
- `byCategory` como lista `{categorySlug, categoryName, count}`;
- `byTeam` como lista `{teamId, teamName, count}`.

Web busca `pending`, `inProgress`, `resolved`, `urgent`, etc. y tipa las dos
listas como `Record<string, number>`. Por ello muestra ceros y calcula barras con
objetos en lugar de números.

**Corrección requerida:** alinear el tipo exacto, leer claves de enum en
mayúsculas y recorrer las listas con `*ngFor`.

### W05 — la pantalla de auditoría falla con datos reales

Web espera `timestamp`, `actor`, `role`, `module`, `details` e `ip`. El backend
devuelve `occurredAt`, `actorAdminUserId`, `actorUserId`, `entityType`, `entityId`,
`metadata` y `sourceIp`.

Cuando llega una fila real, `logsFiltrados` intenta ejecutar
`item.details.toLowerCase()` sobre un campo inexistente. La tabla no representa el
contrato y la búsqueda puede lanzar una excepción.

**Corrección requerida:** modelar `AuditEvent` exactamente, decidir cómo presentar
actor y metadata, mostrar valores nulos de forma segura y agregar paginación por
cursor.

### W06 — categorías y equipos envían o esperan campos inexistentes

- La creación de categoría envía `isActive`, pero `CreateCategoryRequest` solo
  admite `slug`, `name`, `colorHex` y `sortOrder`. El backend no ignora campos
  desconocidos, por lo que el alta puede responder 400.
- El equipo web espera `specialty` y `membersCount`; el backend entrega
  `description` y `memberCount`.
- Existen dos definiciones distintas de `AdminTeam` en el proyecto.
- El servicio conserva cuatro cuadrillas ficticias.
- Un error de lectura instala esas cuadrillas falsas y un error al actualizar una
  cuadrilla se presenta como éxito local.

**Corrección requerida:** usar un modelo único y exacto, retirar los fallbacks de
producción y mostrar el error real sin simular persistencia.

### W07 — WebSocket implementado pero nunca conectado

`AdminRealtimeService` no se inyecta en la raíz ni en el flujo de sesión. Ningún
componente llama `connect()`. La corrección de cookie ya fue publicada en backend,
por lo que el bloqueo anterior del servidor dejó de existir.

El servicio además deja `shouldReconnect=false` después de `disconnect()` y no lo
restablece al iniciar otra sesión. Solo atiende `report.created` y
`report.updated`; ignora `report.deleted`, `category.updated`, `team.updated` y
`statistics.updated`.

**Corrección requerida:** conectar una vez después de autenticar/recuperar sesión,
cerrar en logout, restablecer reconexión, resincronizar REST al abrir/reabrir el
socket y tratar todos los eventos publicados.

## 5. Hallazgos altos y medios

### W08 — CI rojo y versiones sin política reproducible

- `setup-node` usa Node 20, fuera del engine requerido por el lock actual.
- `package.json` declara `packageManager: npm@11.19.0`, mientras la prueba con
  Node 20 usa npm 10.8.2 y el workflow no activa Corepack ni fija npm.
- Los rangos con `^` permiten que una instalación sin lock obtenga versiones
  diferentes.
- El último workflow sigue rojo aunque hoy las pruebas pasen localmente.

Se recomienda fijar Node 24, una versión real y compartida de npm, añadir
`engines.node`, mantener el lock y comprobar `npm ci`, pruebas y build en un nuevo
commit.

### W09 — dependencias con vulnerabilidades conocidas

`npm audit` reportó seis vulnerabilidades en herramientas de desarrollo:

- altas: `@angular/cli`, `@modelcontextprotocol/server` y `vite`;
- moderadas: `vitest` y `@vitest/mocker`;
- baja: `@angular/build`.

Todas informan corrección disponible. No se observó una vulnerabilidad crítica ni
una dependencia de runtime de la aplicación marcada directamente, pero las altas
afectan herramientas ejecutadas durante desarrollo/CI y deben actualizarse junto
con el lock, repitiendo pruebas y build.

### W10 — las pruebas no verifican el contrato

Las ocho pruebas son de creación de componentes. No comprueban URL, parámetros,
DTO, cookies, CSRF, blob, 204, 400, 401, 409, 429, paginación ni WebSocket. Algunas
ni siquiera ejecutan `detectChanges()`, por lo que no disparan `ngOnInit` ni las
lecturas HTTP.

Se necesitan pruebas `HttpTestingController` de cada servicio/interceptor y un E2E
con backend real que cubra login, listado, detalle, imagen, estado, prioridad,
asignación, estadísticas, auditoría y logout.

### W11 — errores incompletos y cargas que pueden quedar bloqueadas

Categorías, estadísticas y auditoría activan `isLoading`, pero no usan `finalize`
ni manejan el error; una falla deja el spinner activo. El listado guarda
`errorMessage`, aunque la plantilla nunca lo muestra. No existe tratamiento
uniforme para 400, 403, 404, 409, 429 y 5xx.

### W12 — paginación incompleta

Los servicios guardan `nextCursor`, pero reportes no ofrece acción de continuar y
auditoría no conserva/acumula páginas. El mapa solo carga 50 reportes. Con más
datos, el administrador ve un subconjunto sin una indicación clara.

### W13 — MapLibre se carga por dos mecanismos y con versiones distintas

`package.json` instala MapLibre 6.x y Angular incorpora su CSS, pero `index.html`
carga JavaScript y CSS 4.7.1 desde un CDN y el componente depende de la variable
global `maplibregl`. Esto mezcla versiones, duplica CSS y depende de red externa.

Se debe importar MapLibre desde el paquete npm, retirar el CDN y sus globales, y
tipar el mapa, popup y marcadores.

### W14 — mensajes institucionales y de seguridad no acreditados

El login afirma de forma fija TLS 1.3/AES-256, certificación ISO/IEC 27001, ENS,
un ID de sesión y una latencia de 14 ms. El entorno local usa HTTP y no existe
evidencia versionada de esas certificaciones o mediciones. Los botones de idioma y
ayuda y los enlaces del pie tampoco tienen funcionalidad.

Debe retirarse o reemplazarse esa información por datos verificables del entorno
desplegado.

## 6. Matriz funcional vigente

| Función web | Backend disponible | Estado web |
|---|---:|---|
| Login, cookie y CSRF | sí | implementado, pendiente E2E real |
| Recuperar sesión y logout | sí | implementado |
| Listar reportes admin | sí | lectura básica parcial |
| Mapa administrativo | sí | funcional para primera página |
| Detalle completo | sí | servicio sin uso |
| Imagen autenticada | sí | servicio sin uso; imagen ficticia |
| Cambiar estado | sí | llamada real, modelo posterior incorrecto |
| Cambiar prioridad/fecha | sí | servicio sin interfaz |
| Asignar/desasignar | sí | interfaz simula cambio local |
| Equipos | sí | DTO incorrecto y fallback ficticio |
| Responsables | sí | no consumido |
| Categorías | sí | listado/edición parcial; alta incompatible |
| Estadísticas | sí | DTO y claves incorrectos |
| Auditoría | sí | DTO incorrecto y riesgo de error en runtime |
| WebSocket admin | sí | no conectado |
| Paginación | sí | no resuelta en interfaz |
| CI | no aplica | rojo en último workflow |

## 7. Plan de implementación para el equipo web

### Prioridad 0 — dejar una base confiable

1. Cambiar GitHub Actions y desarrollo a Node 24.
2. Fijar npm y `engines`, actualizar lock y resolver `npm audit`.
3. Separar todos los DTO según OpenAPI.
4. Agregar pruebas HTTP de contrato antes de ajustar pantallas.

**Aceptación:** `npm ci`, pruebas y build verdes en GitHub Actions; ningún modelo
mezcla respuestas distintas.

### Prioridad 1 — completar reportes administrativos

1. Listar con `ReportAdminSummary`.
2. Abrir con `GET /admin/reports/{id}`.
3. Descargar y liberar la imagen binaria.
4. Implementar estado, prioridad y fecha objetivo.
5. Cargar equipos y responsables.
6. Implementar asignación y desasignación con 204 y 409.
7. Recargar/mezclar correctamente después de mutaciones.
8. Implementar paginación.

**Aceptación:** una recarga conserva toda mutación y nunca aparecen fotos o equipos
ficticios.

### Prioridad 2 — corregir módulos auxiliares

1. Alinear estadísticas y sus claves en mayúsculas.
2. Alinear auditoría y hacer segura la búsqueda.
3. Corregir DTO de equipos y categoría de alta.
4. Añadir estados de error y `finalize` en todas las cargas.

### Prioridad 3 — tiempo real, mapa y presentación

1. Integrar el WebSocket con el ciclo de sesión y reconexión.
2. Resincronizar REST tras cada conexión.
3. Importar MapLibre desde npm y retirar CDN/global.
4. Retirar afirmaciones de seguridad y controles decorativos sin función.

### Prioridad 4 — prueba conjunta

Ejecutar con dos cuentas ciudadanas y un administrador:

1. cada ciudadano solo ve sus propios reportes en Android;
2. el administrador ve todos en web;
3. detalle, coordenadas e imagen coinciden;
4. estado, prioridad, fecha y asignación persisten;
5. 409 recarga datos sin corromper la pantalla;
6. WebSocket actualiza o provoca resincronización;
7. logout invalida la sesión y ninguna operación fallida aparenta éxito.

## 8. Conclusión

No hubo cambios nuevos del equipo web desde la auditoría anterior. El proyecto
compila y sus ocho pruebas de humo pasan, pero el panel todavía no está listo para
la prueba general: detalle, imagen, asignación, estadísticas, auditoría, equipos,
WebSocket y paginación siguen incompletos o desacoplados del contrato real. El
backend ya expone esas operaciones; las correcciones restantes de este recorrido
pertenecen al repositorio web.
