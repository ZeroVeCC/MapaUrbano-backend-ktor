# Reescaneo de repositorios y plan de privacidad

**Fecha de corte:** 15 de octubre de 2026  
**Repositorio que conserva el documento:** `ZeroVeCC/MapaUrbano-backend-ktor`  
**Alcance:** backend Ktor, aplicación Android, panel Angular y monorepo original.  
**Regla de trabajo:** solamente se modifica el repositorio del backend; los demás se revisan como referencia de solo lectura.

## 1. Resultado ejecutivo

Las ramas públicas no contienen cambios nuevos en Android, web ni el monorepo desde la auditoría anterior:

| Componente | Rama revisada | Revisión actual | Cambio desde la auditoría anterior |
|---|---|---|---|
| Backend Ktor | `main` | `a33c1fdce1ac0bfc53cf02ae61242c86290bfb2e` | documentación OpenAPI completa, pruebas de contrato y constancia |
| Android | `main` | `8c866deb2a7bdff1a112baf34f319b17be099822` | ninguno; es exactamente la revisión ya auditada |
| Web | `main` | `7ba06adba4920acfc3f5f36a7aa7d52bb8cadf6c` | ninguno; es exactamente la revisión ya auditada |
| Monorepo original | `main` | `13150ab4142ccf15434d7fc1160f941b17fde8c7` | ninguno |
| Documentación original | `develop` | `9b28a7e1a36dcd647ab81fc6b7f7e95c85cb2a96` | ninguno |

Por lo tanto, el cambio que supuestamente obliga a iniciar sesión en Android todavía no está publicado en `main`, no está en otra rama remota visible y no forma parte de ningún pull request abierto del repositorio Android.

El problema de visibilidad sí involucra al backend. Actualmente el servidor expone listado, detalle e imagen de reportes sin exigir sesión. Ocultar esos datos en Android no resolvería la seguridad porque cualquier persona podría consultar directamente la API.

## 2. Evidencia de GitHub y pruebas

### Backend

- `main`: `a33c1fd`.
- Sin pull requests abiertos.
- GitHub Actions `38661972318`: correcto sobre `a33c1fd`.
- Suite local registrada: 32 pruebas unitarias y 15 de integración PostgreSQL/PostGIS, 47/47 correctas.
- Inventario actual: 135 archivos versionados y 107 archivos Kotlin.

### Android

- `main`: `8c866de`, sin commits posteriores.
- Solo existe la rama remota `main`.
- Sin pull requests abiertos.
- GitHub Actions `38567264700`: correcto.
- Inventario actual: 79 archivos versionados, 35 archivos Kotlin y 6.020 líneas Kotlin.
- No se repitió Gradle localmente porque esta computadora continúa sin Android SDK; la evidencia de compilación vigente es el workflow remoto verde.

### Web

- `main`: `7ba06ad`, sin commits posteriores.
- Solo existe la rama remota `main`.
- Sin pull requests abiertos y sin GitHub Actions.
- Inventario actual: 52 archivos versionados y 33 archivos TypeScript/HTML/CSS.
- Compilación de producción repetida el 15 de octubre: correcta, 409,17 kB iniciales y 92,24 kB de transferencia estimada.
- Continúa la advertencia de presupuesto: `reports.css` supera 4 kB por 272 bytes.
- Pruebas repetidas: 5/8 correctas. Siguen fallando dos pruebas de `App` y una de `Sidebar` por falta de `ActivatedRoute`; la aserción antigua del título también continúa desactualizada.

### Monorepo

- `main` y `develop` no cambiaron.
- Continúan abiertos PR #9, #11 y #12.
- La documentación original todavía establece mapa público, detalle público, imagen pública y alta anónima opcional.

## 3. Diagnóstico de privacidad

### 3.1. Política nueva solicitada

La política indicada durante esta revisión es:

1. la aplicación Android requiere una cuenta ciudadana;
2. cada cuenta puede listar, abrir y descargar solamente sus propios reportes;
3. solo los administradores del panel web pueden listar y consultar todos los reportes;
4. el alta anónima deja de ser parte del flujo normal;
5. los reportes anónimos históricos quedan visibles solamente para administradores.

Esta política contradice decisiones explícitas del alcance original:

- RF-20 exigía elegir entre cuenta y anónimo;
- ADR-011 conservaba el modo anónimo;
- `GET /api/v1/reports`, su detalle y su imagen se definieron como públicos;
- Android se diseñó con mapa público y login opcional.

Por eso el comportamiento observado no nació como un bypass accidental: implementa el contrato original. A partir del nuevo requisito pasa a ser un defecto de autorización y un cambio de alcance que debe cerrarse de extremo a extremo.

### 3.2. Exposición actual del backend

| Superficie | Comportamiento actual | Riesgo frente a la política nueva |
|---|---|---|
| `GET /api/v1/reports` | autenticación Bearer opcional; consulta todos los reportes no eliminados dentro del área | cualquier cliente obtiene reportes de todas las cuentas y anónimos |
| `GET /api/v1/reports/{id}` | no verifica sesión ni pertenencia | quien conoce un UUID obtiene el detalle |
| `GET /api/v1/reports/{id}/image` | no verifica sesión ni pertenencia | quien conoce un UUID descarga la evidencia |
| `POST /api/v1/reports` | permite `ANONYMOUS`; el valor inválido cae silenciosamente a anónimo | se pueden crear reportes sin cuenta y errores del cliente pueden convertirse en anónimos |
| `POST /api/v1/report-status` | permite consultar por código sin sesión | conserva el modelo de seguimiento anónimo |
| `/api/v1/ws/public` | canal sin sesión | puede convertirse en otra fuente de datos globales al ampliar eventos |
| `GET /api/v1/users/me/reports` | exige Bearer y filtra por `user_id` | es la base correcta para el listado privado, aunque tiene errores de cursor/categoría/imagen |
| `/api/v1/admin/*` | pretende exigir sesión administrativa | conceptualmente es la superficie correcta para acceso global, pero el login y la cookie siguen bloqueados |

La aplicación no puede corregir esta exposición por sí sola. El control de pertenencia tiene que aplicarse en Ktor antes de consultar o devolver el recurso.

### 3.3. Comportamiento actual de Android

- La pantalla inicial es `Map`, aunque no exista sesión.
- `AppViewModel.init` ejecuta `refreshPublicContent()` antes de restaurar la sesión.
- `MapaUrbanoApi.getPublicReports()` llama a `GET /api/v1/reports` sin Bearer.
- `RemoteReportRepository` incorpora todos esos elementos a `reports`.
- Al iniciar sesión, `refreshMyReports()` mezcla los reportes propios con los reportes públicos ya cargados; no elimina los ajenos.
- El detalle continúa usando `GET /api/v1/reports/{id}`.
- El formulario permite `Anonymous` y `Account`.
- Android conserva pantalla de seguimiento y consulta por código.
- Al cerrar sesión vuelve al mapa, donde el contenido global continúa siendo parte del estado.

El cambio de “cuenta obligatoria” mencionado por el equipo todavía no está en GitHub. Antes de coordinar la integración, el responsable Android debe publicarlo mediante commit o pull request.

### 3.4. Estado del panel web

El panel no origina la filtración observada porque aún no consume la API. Sus reportes, marcadores, categorías, estadísticas y auditoría son arreglos locales. Sin embargo, tampoco ofrece hoy una frontera administrativa real:

- no existe login;
- todas las rutas Angular son accesibles sin guarda;
- no existe `HttpClient` ni cliente API;
- no usa cookie, CSRF ni WebSocket;
- no maneja 401/403;
- no tiene CI.

Cuando se conecte, debe usar exclusivamente `/api/v1/admin/*`. Nunca debe consumir el listado ciudadano para obtener todos los reportes.

## 4. Matriz de acceso objetivo

Esta es la política recomendada para implementar el requisito nuevo:

| Operación | Sin sesión | Ciudadano autenticado | Administrador autenticado |
|---|---:|---:|---:|
| registro/login ciudadano | sí | sí | no aplica |
| categorías activas | sí o autenticado | sí | sí |
| crear reporte | no | sí, siempre asociado a su sesión | sí solo si se diseña un alta administrativa específica |
| listar reportes | no | solo propios | todos |
| detalle de reporte | no | solo propio | cualquiera |
| imagen de reporte | no | solo propia | cualquiera |
| historial ciudadano | no | solo del reporte propio | completo |
| tracking anónimo | no en la política estricta | no | consulta interna si se necesita |
| cambios de estado/prioridad/asignación | no | no | sí |
| estadísticas/auditoría | no | no | sí |
| WebSocket | ninguno público | canal propio opcional | canal administrativo |

Para evitar enumeración, solicitar un reporte ajeno debe devolver el mismo `404` genérico que un UUID inexistente. No debe responder `403` revelando que el recurso existe.

## 5. Plan del backend Ktor

Este trabajo corresponde a nuestro repositorio y debe ejecutarse antes de considerar segura la aplicación.

### B-01 — Congelar la política

- Confirmar con el equipo y el profesor que se elimina RF-20 y que la cuenta pasa a ser obligatoria.
- Registrar qué ocurre con reportes anónimos históricos.
- Recomendación: conservarlos en PostgreSQL y permitir su consulta solamente desde el panel administrativo.
- Decidir si `POST /report-status` se desactiva de inmediato o mantiene una transición limitada para códigos ya emitidos. La política estricta requiere desactivarlo.

**Criterio de aceptación:** existe una decisión escrita y la misma regla figura en backend, Android, web y OpenAPI.

### B-02 — Cerrar las rutas públicas de reportes

- Quitar `optional = true` del grupo de reportes.
- No usar `GET /api/v1/reports` como listado global ciudadano.
- Mantener como superficie ciudadana `GET /api/v1/users/me/reports`.
- Implementar `GET /api/v1/users/me/reports/{id}` con verificación de pertenencia.
- Proteger la imagen con una ruta de cuenta o con una autorización que compruebe el propietario.
- Retirar o proteger el detalle público actual.

**Criterio de aceptación:** sin token, listado, detalle e imagen responden 401; con token de otra cuenta, detalle e imagen responden 404.

### B-03 — Hacer obligatoria la autoría por cuenta

- Exigir Bearer en `POST /api/v1/reports`.
- Derivar siempre `userId` del principal autenticado.
- Rechazar `submissionMode=ANONYMOUS`.
- Rechazar valores desconocidos; nunca convertir silenciosamente un valor inválido a anónimo.
- Simplificar el contrato para que el cliente no tenga que elegir autoría.
- Conservar los registros anónimos anteriores sin asignarlos artificialmente a una cuenta.

**Criterio de aceptación:** no se crea ninguna fila nueva con `user_id=NULL`, y un alta sin sesión devuelve 401.

### B-04 — Corregir el listado y detalle propios

- Corregir `categorySlug`, que hoy recibe `categoryId`.
- Calcular `hasImage` desde `report_images`.
- Unificar el cursor: generarlo y leerlo con el mismo formato.
- Completar historial visible.
- Agregar consulta de repositorio por `reportId + userId`, evitando cargar primero el reporte global y comprobar después en la ruta.
- Excluir reportes eliminados en todas las consultas.

**Criterio de aceptación:** una cuenta recibe datos correctos y únicamente sus filas en lista, detalle, historial e imagen.

### B-05 — Separar la lectura administrativa

- Corregir primero la autenticación administrativa: login fuera del bloque protegido, plugin `Sessions`, cookie `HttpOnly`, `SameSite`, `Secure` en producción, firma y persistencia/revocación.
- Implementar `GET /api/v1/admin/reports` y `GET /api/v1/admin/reports/{id}`.
- Incorporar endpoint administrativo de imagen.
- Mantener filtros, paginación, prioridad, asignación e historial interno en DTO administrativos.
- No reutilizar DTO ciudadanos si eso amplía información de manera accidental.

**Criterio de aceptación:** una sesión admin válida ve reportes de todas las cuentas y anónimos históricos; una sesión ciudadana nunca accede a `/admin`.

### B-06 — Revisar tiempo real

- Desactivar `/ws/public` si la aplicación ya no tiene datos públicos.
- Si Android necesita tiempo real, crear un canal ciudadano autenticado que emita solo IDs pertenecientes a esa cuenta.
- Mantener `/ws/admin` para el panel, después de corregir la sesión administrativa.
- Probar que eventos de un usuario no lleguen a otro.

### B-07 — Actualizar contrato, documentación y pruebas

- Actualizar OpenAPI para quitar la idea de reportes públicos y del alta anónima.
- Marcar como obsoletas las rutas retiradas antes de eliminarlas si se necesita una transición.
- Actualizar la constancia y las decisiones arquitectónicas.
- Agregar pruebas unitarias e integración con dos ciudadanos y un administrador.

Pruebas mínimas obligatorias:

1. ciudadano A crea reporte A;
2. ciudadano B crea reporte B;
3. A lista únicamente A;
4. B lista únicamente B;
5. A intenta abrir y descargar imagen de B y recibe 404;
6. una solicitud sin token recibe 401;
7. `ANONYMOUS` es rechazado;
8. un administrador válido lista A, B y anónimos históricos;
9. ciudadano y administrador no pueden intercambiar sus credenciales;
10. logout o sesión vencida revocan el acceso.

## 6. Plan para el equipo Android

### A-01 — Publicar primero el trabajo existente

- Subir a una rama el cambio de cuenta obligatoria que actualmente solo existe de forma local, si efectivamente fue realizado.
- Abrir pull request contra `main` y adjuntar captura o prueba del flujo.
- No afirmar que está integrado hasta que el commit sea visible en GitHub y el workflow esté verde.

### A-02 — Colocar una barrera de autenticación real

- Restaurar la sesión antes de decidir la pantalla inicial.
- Si no existe una sesión válida, mostrar login/registro y no cargar el mapa ni reportes.
- Evitar que `Map` sea el destino inicial visible durante la restauración.
- Tratar 401 como sesión vencida, limpiar token/datos y volver al acceso.

### A-03 — Consumir solamente reportes propios

- Eliminar `getPublicReports()` y `getPublicReport()` del flujo ciudadano.
- Cargar mapa y lista desde `GET /users/me/reports`.
- Abrir detalle desde `GET /users/me/reports/{id}`.
- Descargar imágenes mediante el endpoint protegido definido por backend.
- Dejar de mezclar `accountReports` con reportes públicos.

### A-04 — Retirar el modo anónimo

- Eliminar el selector `Anonymous/Account`.
- Enviar siempre un reporte asociado a la sesión.
- Eliminar código de seguimiento, pantalla Tracking y mensajes relacionados si el equipo confirma la política estricta.
- Conservar el borrador cuando la sesión venza y permitir reintentar después del login.

### A-05 — Aislar datos entre sesiones

- Al cerrar sesión, borrar reportes, detalle seleccionado, imágenes y filtros derivados de la cuenta anterior.
- Al cambiar de cuenta en el mismo dispositivo, no reutilizar datos del usuario anterior.
- No guardar respuestas privadas en almacenamiento compartido sin una clave por usuario.

### A-06 — Pruebas Android

- El arranque sin token no llama a rutas de reportes.
- El arranque con token restaura sesión y carga solo `me/reports`.
- Logout elimina los reportes visibles.
- 401 borra la sesión y los datos privados.
- La cuenta B no conserva datos mostrados durante la cuenta A.
- No existe una acción de alta anónima.
- El mapa vacío sigue visible cuando la cuenta todavía no tiene reportes.

## 7. Plan para el equipo web

El panel debe ser la única interfaz capaz de ver el conjunto completo.

### W-01 — Reparar la base antes de integrar

- Corregir las tres pruebas desactualizadas (`ActivatedRoute` y título).
- Agregar GitHub Actions para instalación reproducible, pruebas y build.
- Mantener el presupuesto CSS o ajustar el límite de forma justificada.

### W-02 — Implementar autenticación administrativa

- Crear pantalla de login contra `POST /api/v1/admin/auth/login` cuando backend la habilite.
- Enviar cookie con `withCredentials`.
- Crear guarda para todas las rutas del panel.
- Consultar `/admin/auth/me` al recargar.
- En 401, limpiar estado y volver al login; en 403, mostrar falta de permiso.
- Incorporar CSRF según el mecanismo publicado por backend.

### W-03 — Consumir exclusivamente la API administrativa

- Generar o escribir modelos a partir del OpenAPI actualizado.
- Reemplazar arreglos locales por servicios para reportes, detalle, imagen, categorías, estadísticas y auditoría.
- Consumir únicamente `/api/v1/admin/*` para datos globales.
- Implementar paginación y filtros del servidor; no descargar todo para filtrar en el navegador.
- No usar `/users/me/reports` ni credenciales ciudadanas.

### W-04 — Proteger la presentación

- Sustituir `Popup.setHTML()` con interpolación por nodos de texto o contenido escapado antes de conectar datos reales.
- Evitar que notas internas o identidad del autor se mezclen con vistas que después pudieran reutilizarse públicamente.
- Limpiar todos los datos al cerrar sesión.
- No persistir respuestas administrativas en `localStorage`.

### W-05 — Completar operaciones y pruebas

- Listar todos los reportes, incluidos anónimos históricos.
- Consultar detalle e imagen administrativa.
- Cambiar estado, prioridad, fecha objetivo y asignación con control de versión.
- Mostrar conflictos 409 y recargar el recurso.
- Agregar pruebas de guarda, cookie, 401/403, filtros, XSS y logout.

## 8. Orden de implementación recomendado

1. **Decisión de alcance:** confirmar cuenta obligatoria y destino de reportes anónimos históricos.
2. **Backend, autorización ciudadana:** cerrar rutas públicas, autoría obligatoria y ownership.
3. **Backend, pruebas:** dos ciudadanos, recursos cruzados e imágenes.
4. **Android:** publicar su rama, agregar barrera de login y migrar a rutas propias.
5. **Backend, administración:** reparar sesión admin e implementar listados/detalle globales.
6. **Web:** login, guardas y reemplazo de datos locales por `/admin`.
7. **Tiempo real:** canales separados por rol después de estabilizar REST.
8. **E2E:** dispositivo A, dispositivo B y navegador admin contra la misma base.

No se debe publicar primero un cambio Android que solo oculte reportes. La seguridad queda incompleta mientras la API continúe entregándolos sin autorización.

## 9. Escenario E2E final

| Paso | Resultado esperado |
|---|---|
| abrir Android sin sesión | solo acceso/registro; no se solicita ningún reporte |
| registrar cuenta A y crear reporte A | `user_id` deriva del token A |
| registrar cuenta B y crear reporte B | `user_id` deriva del token B |
| abrir mapa/lista con A | aparece A y nunca B |
| solicitar UUID o imagen de B usando token A | 404 genérico |
| solicitar listado/detalle sin token | 401 |
| intentar enviar modo anónimo | 400 o 403 con código controlado |
| iniciar sesión administrativa web | cookie segura y rutas protegidas habilitadas |
| listar desde el panel | aparecen A, B y anónimos históricos |
| usar token ciudadano en ruta admin | 401 |
| cerrar ambas sesiones | no quedan datos privados visibles ni acceso posterior |

## 10. Mensaje listo para el equipo Android

> Revisamos el `main` publicado (`8c866de`) y todavía carga `GET /api/v1/reports` al iniciar, muestra reportes globales, permite envío anónimo y mezcla los reportes propios con los públicos. El cambio de cuenta obligatoria que comentamos no está subido a GitHub. Necesitamos que publiquen una rama/PR donde la aplicación restaure sesión antes de mostrar el mapa, obligue login/registro, consuma únicamente `/users/me/reports` y su detalle protegido, retire el modo anónimo/seguimiento y limpie todos los datos al cerrar o cambiar de cuenta. Agreguen pruebas para arranque sin token, sesión vencida, logout y cambio entre dos cuentas. No implementen un filtro local como medida de seguridad: backend también cerrará las rutas públicas.

## 11. Mensaje listo para el equipo web

> Revisamos el `main` publicado (`7ba06ad`) y continúa con datos locales, sin `HttpClient`, login, cookie, guardas, CSRF, WebSocket ni CI; las pruebas siguen 5/8. El panel debe ser la única interfaz con acceso a todos los reportes. Primero corrijan las pruebas y agreguen CI. Después implementen login administrativo, cookie con `withCredentials`, guarda de todas las rutas y servicios que consuman exclusivamente `/api/v1/admin/*`. Reemplacen los arreglos simulados por listado, detalle, imagen, categorías, estadísticas y auditoría del servidor. Manejen 401/403/409, limpien datos al cerrar sesión y sustituyan `setHTML` con interpolación antes de usar datos reales para evitar XSS. La integración queda bloqueada hasta que backend habilite la sesión y los endpoints administrativos.

## 12. Conclusión

La exposición observada se origina en una decisión anterior de producto y hoy está implementada tanto en backend como en Android. Frente a la política nueva, el backend es el punto obligatorio de corrección: debe hacer cumplir pertenencia y rol aunque un cliente esté modificado o sea malicioso. Android debe dejar de pedir y mezclar datos globales. Web todavía no expone datos reales, pero debe convertirse en la única interfaz global después de que la autenticación administrativa sea operativa.
