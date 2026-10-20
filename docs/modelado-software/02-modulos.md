# 2. Módulos y responsabilidades

La organización de paquetes en `src/main/kotlin/com/mapaurbano/` permite separar responsabilidades:

| Paquete | Responsabilidad principal |
|---|---|
| `application` | Arranque, configuración, rutas generales, seguridad y dependencias |
| `users` | Registro, inicio de sesión y datos de vecinos |
| `auth` | Autenticación y sesiones de usuarios y administradores |
| `reports` | Alta, consulta, estado y prioridad de reportes |
| `categories` | Clasificación de incidencias |
| `assignments` | Equipos, integrantes y asignación de reportes |
| `media` | Validación y recuperación de imágenes |
| `statistics` | Datos agregados para administración |
| `audit` | Registro de acciones relevantes |
| `notifications` | Comunicación de eventos por WebSocket |
| `health` | Comprobaciones de salud del servicio |
| `database` e `infrastructure` | Esquema y persistencia |
| `shared` | Elementos comunes entre módulos |

## Ejemplo de responsabilidad

Al crear un reporte, la ruta `reports/api/PublicReportRoutes.kt` recibe un formulario multipart, comprueba la identidad y los campos, llama a `CreateReportUseCase` y, si se adjuntó una imagen, a `ProcessImageUseCase`. La lógica se reparte entre archivos en vez de concentrarse en una pantalla o una sola función.

## Rutas y autorización

`application/Routing.kt` distingue rutas ciudadanas con `user-bearer` y rutas administrativas con `admin-session`. El login administrativo se registra por separado. Las operaciones sensibles administrativas requieren controles adicionales, descritos en los archivos de seguridad y rutas.

[Volver al índice](README.md)
