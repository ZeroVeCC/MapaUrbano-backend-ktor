# Swagger y contrato OpenAPI

La documentación interactiva del backend se publica en:

```text
http://localhost:8080/swagger
```

La fuente versionada es
`src/main/resources/openapi/documentation.yaml`. El archivo usa OpenAPI 3.0.3 y
describe todas las rutas HTTP y los handshakes WebSocket registrados actualmente
por Ktor.

## Cómo usarlo con el backend local

1. Levantar PostgreSQL/PostGIS y el backend.
2. Abrir `http://localhost:8080/swagger`.
3. Desplegar una operación, completar sus parámetros y elegir **Execute**.
4. Para rutas ciudadanas protegidas, ejecutar primero
   `POST /api/v1/users/register` o `POST /api/v1/users/login`.
5. Copiar el valor `token`, pulsar **Authorize** y pegar solamente el token. Swagger
   agrega el prefijo `Bearer`.

El servidor declarado es `/`, por lo que Swagger envía las solicitudes al mismo
host y puerto desde el que se abrió la interfaz. Esto evita fijar `localhost` en el
contrato y permite usar el mismo archivo en otro entorno.

## Creación de reportes

`POST /api/v1/reports` usa `multipart/form-data`. Sus partes son:

| Parte | Tipo | Obligatoria | Observación |
|---|---|---:|---|
| `title` | texto | sí | título del problema |
| `description` | texto | sí | detalle de la incidencia |
| `categorySlug` | texto | sí | por ejemplo `bache` |
| `latitude` | número | sí | entre -90 y 90 |
| `longitude` | número | sí | entre -180 y 180 |
| `submissionMode` | texto | sí | `ANONYMOUS` o `ACCOUNT` |
| `image` | archivo | no | el nombre real de la parte es `image` |

El modo `ACCOUNT` requiere el token Bearer. El modo `ANONYMOUS` devuelve un
`trackingCode` opaco de 10 caracteres, que luego se envía a
`POST /api/v1/report-status`.

## Estado real de cada operación

La extensión `x-implementation-status` evita confundir el contrato previsto con el
estado del código:

| Valor | Significado |
|---|---|
| `implemented` | funciona en la implementación actual |
| `partial` | funciona con una limitación documentada |
| `stub` | la ruta existe pero responde HTTP 501 |
| `blocked` | hay código, pero la autenticación administrativa impide acceder |
| `placeholder` | responde con la forma prevista, todavía sin datos reales |

Swagger muestra las rutas administrativas porque el frontend web necesita conocer
su contrato. Esto no significa que ya estén operativas. El login administrativo
está dentro del bloque que exige una sesión previa, falta instalar el transporte de
cookie de Ktor Sessions y varios listados responden 501.

## WebSockets

OpenAPI no ejecuta WebSockets desde Swagger UI. Los canales
`/api/v1/ws/public` y `/api/v1/ws/admin` se documentan como handshakes `GET` con
respuesta `101` y una extensión `x-websocket-events`. Los eventos usan:

```json
{
  "type": "report.updated",
  "occurredAt": "2026-10-14T18:15:00Z",
  "payload": {}
}
```

El cliente debe cargar primero su estado por REST y usar WebSocket para cambios
posteriores. El bus actual está en memoria y no ofrece replay.

## Regla para mantener el contrato

Toda ruta, parámetro, nombre de parte multipart o DTO que cambie en el backend debe
actualizar este OpenAPI en el mismo commit. Si una operación todavía no funciona,
debe conservar su estado explícito y documentar el HTTP 501 o el bloqueo real, sin
presentarla como terminada.
