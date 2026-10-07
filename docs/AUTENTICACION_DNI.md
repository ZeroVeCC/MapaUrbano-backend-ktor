# Autenticación ciudadana por DNI

Actualización del 6 de octubre de 2026. Los vecinos inician sesión con **DNI y contraseña**. El correo deja de formar parte del contrato de registro/login. Los administradores conservan el contrato separado con `username`; sus cookies, CSRF y login siguen siendo un pendiente distinto.

## Contrato para Android

`POST /api/v1/users/register`, JSON:

```json
{"dni":"30123456","displayName":"Vecino","password":"una-clave-segura"}
```

Devuelve 201 con `id` y `token`. `POST /api/v1/users/login` recibe `dni` y `password` y devuelve 200 con `userId` y `token`. No enviar `email`: los campos desconocidos se rechazan con 400.

El DNI es un **string**, nunca un número. La política inicial del MVP admite 7 u 8 dígitos ASCII, incluyendo ceros iniciales, pero no todos ceros. Acepta `30.123.456`, `1.234.567` y espacios exteriores; guarda exclusivamente dígitos. Rechaza letras, guiones, separadores mal ubicados y otras longitudes. Es validación de formato, no verificación de identidad ante un organismo.

Registro exige contraseña de al menos 8 caracteres y hasta 72 bytes UTF-8, y nombre recortado de 1–100 caracteres. DNI repetido: 409 `DNI_ALREADY_REGISTERED`. Login devuelve el mismo 401 para documento inexistente/inválido, contraseña incorrecta o cuenta inactiva.

Enviar `Authorization: Bearer <token>` para:

- `GET /api/v1/users/me`: perfil propio incluido DNI; sin contraseña ni hash.
- `POST /api/v1/users/logout`: revoca la sesión actual, devuelve 204.
- `DELETE /api/v1/users/me`: desactiva cuenta y revoca sus sesiones, devuelve 204.
- `GET /api/v1/users/me/reports`: lista propia; los problemas anteriores de paginación no forman parte de este cambio.

Tokens aleatorios de 32 bytes, Base64URL, vencimiento absoluto de 30 días; en servidor se conserva SHA-256. La autenticación comprueba hash, vencimiento, revocación, usuario activo y DNI asignado. Logout revoca el identificador de sesión, no el token ni el ID de usuario. Expiración por inactividad: pendiente.

El alta multipart de reportes reconoce sesión opcional: `submissionMode=account` obtiene el autor desde el token; `anonymous` no guarda vínculo con la cuenta. Los DTO públicos de reportes no reciben DNI. OpenAPI documenta estas operaciones de autenticación, todavía no toda la API.

## Migración de cuentas

V1 y V2 permanecen intactas. `V3__add_user_dni.sql` agrega DNI, validación e índice único parcial. Permite correo nulo en nuevos registros y preserva correos, hashes, IDs y reportes anteriores. No se deducen documentos de correos ni se asignan DNI ficticios.

Las cuentas anteriores quedan con `dni=NULL`: no pueden usar el nuevo login ni reutilizar tokens hasta recibir un DNI verificado. Antes de desplegar, asignar los documentos mediante un proceso operativo de verificación de identidad. No permitir autovinculación pública de cuentas heredadas. Registro por API siempre exige DNI; el nulo existe para compatibilidad con datos anteriores.

El repositorio traduce el conflicto concurrente del índice `users_dni_uq` a 409. Se agregaron pruebas V2→V3, preservación de datos, formato y duplicados, pendientes de ejecución con Docker/PostGIS.

## Dependencias y pruebas

Se actualizó Koin 3.5.6 a 4.2.0: las pruebas HTTP encontraron `NoClassDefFoundError: io/ktor/server/routing/RoutingKt` al resolver dependencias con Ktor 3.5.1. Ver [integración Koin/Ktor](https://insert-koin.io/docs/reference/koin-ktor/ktor/).

Se repararon firmas y expectativas antiguas en pruebas de reportes, tests JUnit que devolvían excepciones en lugar de `Unit`, y una espera del EventBus sin suscripción previa ni timeout.

```powershell
.\gradlew.bat test --tests '*UserAuthenticationTest' --tests '*RegisterUserUseCaseTest' --tests '*LoginUserUseCaseTest' --tests '*ChangeReportStatusUseCaseTest' --tests '*CreateReportUseCaseTest' --tests '*EventBusTest'
```

Estas pruebas usan repositorios en memoria únicamente dentro de tests; producción sigue usando PostgreSQL. No validan las migraciones ni sustituyen integración con base real.

Resultado comprobado: 13 pruebas seleccionadas aprobadas. La prueba HTTP usa el routing del servidor y comprueba registro, login, perfil, rechazo de token falso, logout, baja y autoría de reportes en modo cuenta/anónimo. La suite general ejecutó 19 pruebas: 15 aprobadas y 4 fallidas. `RoutingTest.business endpoints...` agotó su espera al intentar usar repositorios reales sin base preparada; tres `PublicReportRoutesTest` fallaron por falta de configuración de base al arrancar el módulo completo. Esos tests heredados necesitan un entorno de integración correctamente configurado y aserciones más fuertes que «no es 404»; no se deshabilitaron para ocultar los fallos.

Docker/WSL todavía no pudo iniciar: Windows informa virtualización deshabilitada. Revisar BIOS/UEFI y Plataforma de máquina virtual antes de ejecutar `integrationTest` o el backend conectado a PostgreSQL.
