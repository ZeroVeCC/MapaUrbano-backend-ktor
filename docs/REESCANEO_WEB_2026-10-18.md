# Reescaneo del frontend web

> **Corte histórico.** Web avanzó posteriormente a `fced3ea`. La auditoría
> vigente, con el nuevo delta, los errores de contrato y el diagnóstico exacto de
> CI, está en `docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md`.

**Fecha de corte:** 18 de octubre de 2026  
**Repositorio auditado:** `Santy112/MapaUrbano-frontend-web`  
**Rama:** `main`  
**Revisión:** `85832a4183d6d1fde96b49161bee1142445f3cc6`  
**Revisión anterior:** `7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c`

El repositorio web se revisó como referencia de solo lectura. No se modificó ningún archivo de ese proyecto. Esta constancia compara el código publicado con el contrato OpenAPI del backend conservado en `src/main/resources/openapi/documentation.yaml`.

## 1. Resultado general

Sí hubo cambios importantes. El panel avanzó desde una maqueta puramente local hacia una primera arquitectura de integración:

- se agregaron `HttpClient`, entornos para REST y WebSocket y servicios administrativos;
- se creó una pantalla de login, una guarda de rutas y logout;
- se agregaron clientes para reportes, categorías, estadísticas, auditoría, equipos y tiempo real;
- se reemplazó el `setHTML` inseguro del mapa por nodos DOM con `textContent`;
- se corrigieron las ocho pruebas existentes para que pasen localmente;
- se ajustó el presupuesto CSS y el build dejó de advertir por `reports.css`;
- se agregó un workflow de GitHub Actions;
- se prepararon controles visuales para estado, conflicto 409, equipos y baja de reportes.

El avance todavía no constituye una integración funcional. Gran parte de la UI continúa usando datos de respaldo cuando la API falla, varios modelos TypeScript no coinciden con Swagger y acciones anunciadas en el código no están conectadas a la pantalla. Además, las tres ejecuciones del workflow nuevo fallaron antes del build.

## 2. Cambios publicados

Desde la revisión anterior se agregaron tres commits y 42 archivos cambiaron, con 1.921 líneas agregadas y 640 eliminadas:

| Commit | Fecha local del autor | Contenido principal |
|---|---|---|
| `7eb4484` | 16 de octubre | pruebas, presupuesto CSS, corrección XSS y workflow CI |
| `d274fe6` | 17 de octubre | servicios HTTP, login/logout, guarda, modelos, categorías y WebSocket |
| `85832a4` | 17 de octubre | servicio de equipos, preparación de asignaciones y baja de reportes |

En GitHub solamente existe la rama `main`, no hay pull requests abiertos o históricos visibles y la rama no está protegida.

## 3. Verificaciones ejecutadas

### 3.1. Build

El build de producción terminó correctamente:

```text
Initial total: 369.45 kB
Estimated transfer: 87.04 kB
```

No apareció la advertencia anterior de presupuesto de `reports.css`. El límite por estilo se cambió de 4/8 kB a 6/10 kB y el bundle inicial también bajó respecto de la medición anterior.

### 3.2. Pruebas locales

El comando compatible con el builder actual terminó correctamente:

```text
ng test --watch=false
Test Files: 7 passed
Tests: 8 passed
```

Las ocho pruebas son de creación de componentes, excepto una aserción básica del título. Todavía no prueban autenticación, guarda, cookies, servicios HTTP, errores, contratos, filtros, mutaciones, WebSocket ni XSS.

### 3.3. GitHub Actions

Las tres ejecuciones disponibles fallaron:

| Run | Commit | Resultado |
|---:|---|---|
| `38993082920` | `7eb4484` | fallo |
| `38998284342` | `d274fe6` | fallo |
| `38999555595` | `85832a4` | fallo |

En la ejecución más reciente, checkout, Node y `npm ci` terminaron correctamente. El paso **Run unit tests** falló y el build quedó omitido. El workflow ejecuta:

```bash
npm test -- --watch=false --browsers=ChromeHeadless
```

El proyecto usa el builder de pruebas de Angular/Vitest y no tiene instalado un proveedor de navegador. La reproducción local del comando exacto devuelve que la opción `browsers` requiere `@vitest/browser-playwright`, `@vitest/browser-webdriverio` o `@vitest/browser-preview`.

La corrección mínima recomendada es usar `npm test -- --watch=false`, que ya pasa, o instalar y configurar explícitamente un proveedor si realmente se necesitan pruebas en navegador.

## 4. Qué se resolvió correctamente

### Base técnica

- `provideHttpClient(withFetch())` está configurado.
- Existen archivos `environment.ts` y `environment.prod.ts` para URL REST y WebSocket.
- Todas las rutas funcionales están detrás de `adminAuthGuard`; `/login` queda pública.
- Las solicitudes administrativas usan `withCredentials: true`.
- No se guardan respuestas ni credenciales en `localStorage`.
- Logout limpia el usuario mantenido en memoria y vuelve al login.

### Seguridad de presentación

- El popup de MapLibre usa `setDOMContent` y `textContent`; se eliminó la interpolación mediante `setHTML` que podía producir XSS al usar datos remotos.

### Interfaz

- Existe login administrativo con mensajes diferenciados para 401 y 403.
- Reportes contempla carga, búsqueda, filtro por estado y conflicto 409.
- Categorías tiene alta, slug, color, orden y activación.
- Estadísticas y auditoría ya consultan servicios.
- Hay servicios iniciales para equipos y WebSocket.

## 5. Problemas que debe corregir el equipo web

### P0 — CI siempre rojo

Cambiar el comando de pruebas del workflow. Hasta hacerlo, ningún commit puede demostrar automáticamente que pruebas y build pasan.

**Criterio:** una nueva ejecución de `CI Frontend` debe completar `npm ci`, pruebas y build en verde.

### P0 — Modelos incompatibles con Swagger

Los modelos fueron escritos manualmente y contienen diferencias que romperán la UI aunque el backend habilite los endpoints.

| Tema | Web actual | Contrato backend |
|---|---|---|
| ID de reporte | `number` | UUID `string` |
| estado | incluye `REJECTED` | `PENDING`, `IN_PROGRESS`, `RESOLVED` |
| prioridad | incluye `CRITICAL` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| paginación | `page`, `pageSize`, `total`, `totalPages` | `cursor`, `limit`, `nextCursor`, `totalCount` |
| filtro de categoría | `categorySlug` | query `category` |
| resumen de reporte | espera descripción, dirección e `imageUrl` | el resumen no contiene esos campos; expone `hasImage` |
| detalle | reutiliza el elemento del listado | requiere `GET /admin/reports/{id}` |
| imagen | usa una URL del DTO | requiere endpoint binario administrativo, todavía pendiente en backend |
| categoría | `displayOrder` | `sortOrder` |
| equipo | `specialty`, `membersCount` | `description`, `memberCount`, `createdAt` |
| estadísticas | objetos con claves fijas y porcentajes | mapas dinámicos y `categorySlug/categoryName/count`, más `byTeam` |
| auditoría | ID numérico y campos `timestamp/actor/role/module/details/ip` | UUID y `occurredAt/action/entityType/metadata/sourceIp` |
| login | espera `id/fullName/role` | responde `adminUserId/username` |
| `/auth/me` | espera un `AdminUser` completo | hoy solo declara `adminUserId` y `username`; la implementación actual devuelve únicamente `adminUserId` |

La prioridad y los estados falsos no son diferencias visuales: enviar `REJECTED` o `CRITICAL` producirá validación fallida en el backend.

**Criterio:** generar los tipos desde OpenAPI o alinearlos manualmente, agregar pruebas de contrato con respuestas reales de ejemplo y no usar `any` para ocultar diferencias.

### P0 — Los fallos del servidor se convierten en éxitos simulados

Los servicios de reportes, categorías, equipos, estadísticas y auditoría sustituyen errores por datos locales. También simulan cambios de estado, creación, activación y borrado. Esto puede mostrar al operador que una acción quedó guardada cuando PostgreSQL nunca cambió.

Casos especialmente peligrosos:

- `deleteReport` elimina la tarjeta local ante cualquier error HTTP;
- `updateStatus` simula éxito para casi cualquier error distinto de 409;
- crear o activar una categoría simula éxito ante errores de validación, sesión o servidor;
- los listados inventados aparecen ante 404, 501 o servidor inaccesible;
- el login crea un administrador local cuando recibe 501 o error de red.

**Corrección:** permitir datos demo solo bajo una bandera de entorno explícita y visible. En modo integrado, propagar el error, mantener el estado anterior y mostrar el mensaje correspondiente.

**Criterio:** con API caída o respuesta 500, ninguna mutación aparece como aplicada y ningún usuario obtiene acceso simulado.

### P0 — Autenticación y autorización incompletas

La guarda y `withCredentials` son una base correcta, pero faltan:

- interceptor global para 401/403;
- retorno seguro a login y limpieza de todos los datos administrativos;
- prevención de navegación al login cuando ya existe sesión;
- CSRF para las mutaciones por cookie;
- tratamiento separado de red, 400, 401, 403, 404, 409, 429, 500 y 501;
- pruebas de login, restauración de sesión, expiración y logout;
- eliminación total del bypass de login local en producción.

Además, el backend todavía bloquea el login dentro del grupo autenticado y no instaló/configuró el transporte de `Sessions`, por lo que esta parte no puede validarse de extremo a extremo hasta que se corrija el servidor.

### P1 — Reportes incompletos

- El detalle no consulta `/admin/reports/{id}`; abre el resumen ya cargado.
- La fotografía no se descarga desde una ruta administrativa.
- No hay paginación por cursor.
- Solo se usa búsqueda y estado; faltan categoría, prioridad, equipo y responsable.
- No se puede cambiar prioridad ni fecha objetivo.
- La asignación de equipo solo cambia una propiedad local y ni siquiera está expuesta en el HTML actual.
- El método de baja existe, pero no hay botón que lo invoque.
- El modal HTML termina incompleto y no muestra historial, asignación, autoría ni prioridad histórica.
- No hay estado de error visible para fallos de listado normales.

### P1 — Dashboard todavía local

El mapa, los cinco marcadores y los contadores siguen siendo constantes. El `AdminReportsService` y `AdminStatisticsService` no se usan en el dashboard. La corrección XSS está bien, pero todavía debe aplicarse a datos reales y reaccionar a filtros/actualizaciones.

### P1 — WebSocket preparado pero nunca conectado

`AdminRealtimeService` existe, pero ningún componente llama a `connect()`. Tampoco se desconecta explícitamente durante logout, no interpreta el contrato real de eventos, no limita la reconexión ni maneja 401 de la conexión. Actualmente no produce ninguna actualización.

### P1 — Equipos y responsables incompletos

- No existe pantalla de administración de cuadrillas.
- No se consumen detalle, creación o modificación de equipos.
- No se consumen miembros ni `/admin/assignees`.
- La asignación y desasignación de reportes no realizan solicitudes REST.
- Los nombres de campos no coinciden con `TeamSummary`.

### P1 — Categorías parcialmente conectadas

La pantalla existe, pero `displayOrder` debe ser `sortOrder`. El slug no forma parte de `UpdateCategoryRequest`, por lo que debe tratarse como inmutable después de crear. Faltan errores reales, confirmación de cambios y pruebas de duplicado/conflicto desde servidor.

### P1 — Estadísticas y auditoría incompatibles

Los DTO web no coinciden con las respuestas previstas. Los porcentajes deben calcularse en el cliente si el backend solo entrega conteos. Los filtros de auditoría enviados por web no existen en Swagger, que hoy define únicamente `cursor` y `limit`.

### P2 — Calidad y operación

- Agregar pruebas de servicios con `HttpTestingController` o equivalente.
- Agregar pruebas de guarda, 401/403, 409, errores de mutación y limpieza de sesión.
- Agregar al menos un recorrido E2E de login, listado, detalle y logout.
- Documentar configuración, variables, ejecución, dependencia del backend y estado de endpoints; el README sigue siendo el generado por Angular CLI.
- Definir despliegue y proxy HTTPS/WSS.
- Agregar accesibilidad: foco del modal, cierre con Escape, etiquetas, estados de carga anunciados y navegación por teclado.
- Revisar responsive real en 375 px y tablet.
- Reemplazar `any` en MapLibre y eventos WebSocket por tipos concretos.
- Evitar suscripciones sin ciclo de vida o usar `takeUntilDestroyed` cuando corresponda.

## 6. Dependencias que corresponden al backend

El responsable web no puede completar la integración por sí solo. El backend debe entregar primero:

1. login administrativo público, cookie segura y sesión validada/persistida;
2. CORS con credenciales y origen parametrizado;
3. CSRF definido para operaciones de escritura;
4. listado y detalle administrativo de reportes, actualmente 501;
5. imagen administrativa protegida;
6. listado/detalle de equipos y responsables, actualmente 501;
7. estadísticas calculadas desde PostgreSQL;
8. auditoría paginada, actualmente 501;
9. DTO estable para la respuesta de cambio de estado;
10. WebSocket administrativo autenticado y probado.

Mientras estos puntos continúen bloqueados, web debe mostrar estados no disponibles de manera honesta y no reemplazarlos silenciosamente por datos que parezcan reales.

## 7. Orden de implementación recomendado para web

1. Corregir GitHub Actions y exigir CI verde.
2. Regenerar/alinear todos los modelos y parámetros con OpenAPI.
3. Eliminar fallbacks de producción y centralizar manejo de errores.
4. Completar login, `/me`, logout, interceptor, CSRF y limpieza de sesión.
5. Integrar listado paginado de reportes con filtros del servidor.
6. Integrar detalle e imagen administrativa.
7. Conectar estado, prioridad, fecha objetivo y baja con control de versión/409.
8. Conectar equipos, responsables, asignación y desasignación.
9. Alimentar dashboard y mapa con datos reales.
10. Adaptar estadísticas y auditoría al contrato real.
11. Activar WebSocket después de estabilizar REST.
12. Agregar pruebas de servicios, seguridad y E2E.

## 8. Escenario de aceptación final

1. Sin cookie, toda ruta del panel redirige a login y no consulta datos privados.
2. Credenciales válidas crean una sesión real; recargar conserva el acceso mediante `/me`.
3. El panel lista reportes reales con UUID y paginación por cursor.
4. El detalle, la imagen y los historiales provienen del backend.
5. Estado, prioridad, fecha y asignación persisten tras recargar.
6. Un conflicto 409 recarga el expediente sin perder silenciosamente cambios.
7. Un 401 limpia reportes, estadísticas, auditoría, mapa y usuario antes de volver al login.
8. Un 500 o caída de red muestra error y nunca simula una escritura exitosa.
9. El dashboard muestra los mismos reportes y totales que la lista.
10. Logout invalida la sesión en el servidor y cierra WebSocket.
11. Las pruebas y el build pasan en GitHub Actions.

## 9. Mensaje breve para el responsable web

> Revisamos nuevamente `main` hasta `85832a4`. Hay un avance grande: agregaste login, guarda, servicios HTTP, `withCredentials`, servicios para reportes/categorías/estadísticas/auditoría/equipos, WebSocket y corregiste el XSS del mapa. El build pasa y las 8 pruebas pasan localmente. Sin embargo, las tres ejecuciones de GitHub Actions están rojas porque el workflow usa `--browsers=ChromeHeadless` sin proveedor Vitest; usá `npm test -- --watch=false` o configurá un proveedor. Antes de integrar, alineá los modelos con Swagger: reportes usan UUID string, no number; no existen `REJECTED` ni `CRITICAL`; la prioridad correcta es `URGENT`; la paginación es cursor/limit; categorías usan `sortOrder`; y equipos, estadísticas, auditoría y login tienen campos distintos. Eliminá los fallbacks que simulan login o escrituras exitosas cuando la API falla. Luego completá detalle/imagen, filtros y cursor, prioridad/fecha, asignación real, baja con botón, dashboard real, estadísticas/auditoría, conexión efectiva del WebSocket y pruebas de auth/HTTP/409/logout. Varias rutas siguen bloqueadas o en 501 del lado backend; las vamos a habilitar desde Ktor, pero el panel debe mostrar ese estado como error y no inventar datos guardados.
