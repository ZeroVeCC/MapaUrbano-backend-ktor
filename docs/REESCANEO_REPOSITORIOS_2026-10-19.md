# Reescaneo de repositorios — 19 de octubre de 2026

> **Corte histórico.** Una auditoría posterior del mismo día comprobó cambios
> nuevos en web y retiró el monorepo original del alcance habitual. Usar
> `docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md` como estado vigente. El
> repositorio antiguo solo se vuelve a consultar ante pedido expreso del usuario.

## Alcance

Se revisaron nuevamente las referencias publicadas de los cuatro repositorios de
Mapa Urbano. Solo se modificó el repositorio backend para conservar esta
constancia; Android, web y el monorepo original se trataron como fuentes de solo
lectura.

| Componente | Referencia revisada | Commit | Cambio desde el corte anterior |
|---|---|---|---|
| Backend Ktor | `ZeroVeCC/MapaUrbano-backend-ktor/main` | `907e0c3afc4fe4a141e77a9d1d095bcb87146109` | cierre funcional `34d219e` y documentación `907e0c3` |
| Android | `Jquincal/Mapaurbano_frontend_kotlin/main` | `e1035852acfe98c46a629440eccc06d927fd7967` | dos commits nuevos desde `4cfc824` |
| Web Angular | `Santy112/MapaUrbano-frontend-web/main` | `85832a4183d6d1fde96b49161bee1142445f3cc6` | sin cambios |
| Monorepo original | `Jquincal/Mapa-Urbano/main` | `13150ab4142ccf15434d7fc1160f941b17fde8c7` | sin cambios |
| Documentación original | `Jquincal/Mapa-Urbano/develop` | `9b28a7e1a36dcd647ab81fc6b7f7e95c85cb2a96` | sin cambios |

La comparación incluyó historial Git, archivos modificados, rutas y DTO, pruebas,
workflows públicos y pull requests abiertos. No se modificaron las copias de los
repositorios de los compañeros.

## Resultado general

Backend y Android ya coinciden en la política de cuenta obligatoria y reportes
propios. Android cerró las incompatibilidades críticas que seguían presentes en
`4cfc824`: no consulta rutas públicas, pagina mediante cursor, abre detalle e imagen
privados, adjunta fotografías reales y limpia el estado entre sesiones.

La integración completa todavía no está cerrada porque el panel web no publicó
cambios desde la auditoría anterior. Sus tipos y operaciones continúan desalineados
del OpenAPI administrativo, conserva simulaciones ante fallos y su workflow sigue
rojo. Tampoco se acreditó todavía el recorrido final en un teléfono físico y desde
el panel Angular contra la misma instancia del backend.

## Backend Ktor

### Cambios funcionales publicados en `34d219e`

- Cuenta ciudadana obligatoria para crear reportes.
- Retiro de listado, detalle, imagen, seguimiento y WebSocket públicos.
- Listado, detalle e imagen propios con Bearer y comprobación de pertenencia.
- Respuesta 404 tanto para un reporte inexistente como para uno ajeno.
- Sesiones administrativas persistidas en PostgreSQL mediante la migración V4.
- Cookie HttpOnly `mapa_urbano_admin`, CSRF y CORS con credenciales.
- Bootstrap opcional del primer administrador.
- Listado, filtros, cursor, total y detalle administrativos reales.
- Cambio de estado, prioridad y fecha objetivo con control de versión.
- Asignación y desasignación atómicas con control optimista.
- Equipos, miembros, responsables, categorías administrativas, estadísticas y
  auditoría reales.
- Imagen administrativa protegida y WebSocket solo para administradores.
- Readiness mediante consulta a PostgreSQL.
- Validación de imágenes por firma, tamaño y dimensiones.
- Corrección del mapeo `jsonb` de auditoría en Exposed.
- OpenAPI 2.0.0 alineado exactamente con las rutas registradas.
- Eliminación de stubs 501 y casos de uso públicos/anónimos residuales.

Los registros anónimos históricos permanecen en PostgreSQL y solo pueden aparecer
en consultas administrativas.

### Pruebas verificadas

- `./gradlew.bat build --rerun-tasks`: correcto, 14 tareas ejecutadas.
- Integración PostgreSQL/PostGIS por Testcontainers: correcta.
- Flujo HTTP probado: login admin, cookie, `/me`, listado, detalle, rechazo CSRF,
  estado, asignación, conflicto 409, auditoría, logout y posterior 401.
- GitHub Actions funcional `39174410111`: `success` sobre `34d219e`.
- GitHub Actions de documentación `39175135077`: `success` sobre `907e0c3`.
- `main` local y remoto estaban sincronizados al comenzar este reescaneo.

### Deuda backend que no bloquea el contrato actual

- La imagen se valida antes del alta, pero la creación del reporte y la inserción
  posterior de la imagen todavía no forman una única transacción de base de datos.
- El bus WebSocket vive en memoria y no conserva replay; los clientes deben volver
  a consultar REST después de reconectar.
- Falta definir el despliegue final: HTTPS/WSS, secretos productivos, backup/restore,
  observabilidad y si Angular se sirve desde Ktor o desde un host separado.
- Faltan pruebas de carga y el E2E final con Android y web reales.

## Android

### Commits nuevos

1. `30d301a0efe4953d6cf63a194db4b1a31f3393a9` —
   `feat(android): integrate private reports, pagination and real photos`.
2. `e1035852acfe98c46a629440eccc06d927fd7967` —
   `fix(auth): clear private state immediately on logout`.

Entre `4cfc824` y `e103585` cambiaron 28 archivos, con 936 líneas agregadas y 498
eliminadas.

### Privacidad y contrato

- Se eliminaron `getPublicReports()` y `getPublicReport()` de API y repositorio.
- Mapa y “Mis reportes” usan exclusivamente
  `GET /api/v1/users/me/reports` con Bearer.
- El detalle usa `GET /api/v1/users/me/reports/{id}`.
- La fotografía usa `GET /api/v1/users/me/reports/{id}/image`.
- Ya no se mezclan reportes públicos o de otras cuentas con los propios.
- `DemoContent` queda limitado a fakes y previews.
- Las respuestas tardías se descartan mediante generaciones de sesión, listado,
  detalle, foto y envío.
- Un 401 limpia credenciales y datos privados; durante un envío conserva únicamente
  el borrador para permitir volver a iniciar sesión.

### Paginación

- `nextCursor` se conserva como valor opaco y se reenvía al backend.
- “Cargar más” agrega resultados sin duplicar identificadores.
- Se detiene al recibir `null` y detecta cursores repetidos.
- Existen estados separados para carga adicional y error recuperable de página.

### Fotografías reales

- Se agregó Photo Picker para galería y `TakePicture` con `FileProvider` para cámara.
- Los archivos de cámara se crean dentro del caché privado de la aplicación.
- `PhotoRepository` limita la lectura a 5 MiB y valida tipo decodificado y declarado.
- Se aceptan JPEG, PNG y WebP entre 1 y 12000 píxeles por lado.
- El multipart usa exactamente la parte `image`, con bytes y Content-Type reales.
- El detalle descarga la imagen con Bearer y conserva solo los bytes del detalle
  activo.

### Logout y aislamiento de cuenta

- `e103585` oculta reportes, imágenes, filtros, navegación y borrador antes de
  iniciar cualquier solicitud de red.
- El token se borra antes de solicitar su revocación al servidor.
- La llamada de logout tiene un máximo de diez segundos y el cierre local termina
  incluso si el servidor falla o no responde.
- Iniciar otra cuenta crea un estado privado nuevo.

### Pruebas y documentación Android

- Se agregaron pruebas con MockEngine de rutas privadas, Bearer, cursor, multipart
  `image`, DTO de errores y limpieza del token.
- Las pruebas del ViewModel cubren dos cuentas, 401 en lista/detalle/imagen/alta,
  paginación, respuestas tardías, foto inválida y logout demorado.
- README y documentación ahora describen únicamente el contrato privado.
- El repositorio registra una validación local de 21 pruebas, Lint, APK debug y
  JaCoCo, además de 41 comprobaciones HTTP contra el backend.
- GitHub Actions `39260551604`: `success` sobre `e103585`.

### Pendiente Android

- La propia documentación declara pendiente instalar esta revisión en un teléfono
  físico y verificar cámara, galería, mapa, scroll, GPS, creación con imagen,
  detalle, logout y cambio A/B desde la interfaz.
- Las pruebas unitarias no reemplazan una prueba instrumentada del selector de
  fotos, permisos del proveedor y ciclo de vida real de Android.

## Web Angular

`main` permanece exactamente en `85832a4`; no existen cambios posteriores que
revisar. Continúa vigente el diagnóstico de `docs/REESCANEO_WEB_2026-10-18.md`.

### Lo que ya existe

- Base de login y guarda.
- Servicios HTTP con `withCredentials`.
- Clientes iniciales para reportes, categorías, estadísticas, auditoría y equipos.
- Servicio WebSocket todavía no conectado a la aplicación.
- Corrección de XSS en marcadores.
- Build de producción correcto y ocho pruebas locales correctas con el comando
  compatible con Vitest.

### Lo que sigue pendiente

- Eliminar bypasses de login, mocks y fallbacks que convierten fallos en éxitos.
- Ajustar auth a `{adminUserId, username, csrfToken}`, cookie HttpOnly y
  `X-CSRF-Token` para todas las mutaciones.
- Usar UUID `string`, estados `PENDING/IN_PROGRESS/RESOLVED` y prioridades
  `LOW/MEDIUM/HIGH/URGENT`.
- Reemplazar `page/pageSize` por `cursor/limit` y conservar `version`.
- Corregir categorías a `sortOrder` y alinear equipos, estadísticas y auditoría.
- Conectar listado, filtros, detalle, imagen, estado, prioridad, fecha, asignación,
  desasignación y baja reales.
- Alimentar dashboard y mapa desde la API.
- Activar `/api/v1/ws/admin`, cerrar en logout y recargar REST tras reconectar.
- Centralizar 400, 401, 403, 404, 409, 429 y 5xx.
- Agregar pruebas de sesión, CSRF, mutaciones, conflicto y E2E.
- Corregir GitHub Actions: las ejecuciones `38993082920`, `38998284342` y
  `38999555595` continúan en `failure` por la configuración ChromeHeadless/Vitest.

## Monorepo original

No cambió desde la auditoría anterior:

- `main`: `13150ab`.
- `develop`: `9b28a7e` con workflow histórico correcto `35103038336`.
- Continúan abiertos PR #9 (documentación UX), #11 (backend Ktor antiguo) y #12
  (Android Stitch antiguo).

Debe seguir usándose como fuente histórica de alcance y requisitos. El código
ejecutable vigente está en los tres repositorios separados; fusionar hoy esos PR
antiguos sin compararlos podría reintroducir contratos obsoletos, incluyendo
reportes anónimos y rutas públicas.

## Compatibilidad actual

| Integración | Estado al 19/10/2026 | Evidencia |
|---|---|---|
| Backend ↔ Android: DNI/Bearer | alineada | login, restauración y rutas privadas |
| Backend ↔ Android: privacidad | alineada en código | solo `users/me/reports`; falta teléfono físico |
| Backend ↔ Android: foto | alineada en código | multipart e imagen privada; falta teléfono físico |
| Backend ↔ Android: cursor | alineada | cursor opaco y detección de repetición |
| Backend ↔ web: auth/CSRF | pendiente | web sin adaptación al contrato final |
| Backend ↔ web: DTO/admin | pendiente | modelos y mutaciones desalineados |
| Backend ↔ web: CI | bloqueado en web | tres workflows rojos |
| Sistema completo | pendiente | falta E2E Android + web + Ktor/PostGIS |

## Próximo orden de trabajo

1. Adaptar web al OpenAPI actual y obtener CI verde.
2. Probar Android `e103585` en el Redmi con cámara, galería y cambio de cuenta.
3. Ejecutar el escenario conjunto con ciudadanos A/B y un administrador.
4. Corregir cualquier diferencia encontrada sin reabrir rutas públicas.
5. Decidir despliegue de Angular, configurar HTTPS/WSS y secretos.
6. Añadir backup/restore, observabilidad y pruebas de carga.
7. Considerar una transacción única para reporte e imagen.

## Escenario conjunto de aceptación

1. A inicia sesión y crea un reporte con imagen.
2. A ve mapa, lista, detalle, historial e imagen.
3. A cierra sesión; la interfaz borra inmediatamente sus datos.
4. B inicia sesión en el mismo teléfono y no ve información de A.
5. B crea otro reporte.
6. Un administrador inicia sesión en web y ve A, B y anónimos históricos.
7. El administrador cambia estado, prioridad y asignación de A.
8. Android A recupera el cambio al refrescar.
9. Dos pestañas provocan un conflicto de versión y la segunda recibe 409.
10. Logout invalida cookie/token y cierra el WebSocket.
11. Ningún error de red o 5xx se representa como una escritura exitosa.
12. Los tres workflows terminan en verde.

