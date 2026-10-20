# Entrega del backend en `main` — 20 de octubre de 2026

## Propósito

Esta constancia registra el corte del backend preparado para la prueba general de
Mapa Urbano con Android y el panel web. Solamente se modificó el repositorio
`ZeroVeCC/MapaUrbano-backend-ktor`; Android y web permanecen bajo responsabilidad
de sus respectivos equipos.

La base de la entrega es el cambio funcional `72701ee`, construido sobre
`main` `49e3a52`. El commit que contiene este documento completa la promoción y
documentación de ese trabajo en `main`.

## Funcionalidad backend incluida

- autenticación ciudadana por DNI y contraseña con Bearer opaco;
- cuenta obligatoria y acceso ciudadano limitado a reportes propios;
- autenticación administrativa mediante cookie HttpOnly y CSRF;
- cookie administrativa con `Path=/api/v1`, compatible con REST y el WebSocket
  `/api/v1/ws/admin`;
- rechazo de sesiones cuando el administrador fue desactivado;
- listado, detalle, imagen, categorías, equipos, responsables, estadísticas,
  auditoría y mutaciones administrativas;
- control de concurrencia mediante `version` y respuesta 409;
- reporte e imagen guardados en una única transacción PostgreSQL;
- publicación de `report.created` solamente después del commit;
- cursores estables compuestos por fecha e UUID para reportes propios, reportes
  administrativos y auditoría;
- lectura y escritura de coordenadas PostgreSQL/PostGIS sin inventar `0.0, 0.0`;
- validación de imágenes JPEG, PNG y WebP por firma, tamaño y dimensiones;
- Swagger/OpenAPI servido en `/swagger` y alineado con las rutas registradas;
- health checks, CORS, rate limiting, errores API controlados y WebSocket
  administrativo.

## Verificación repetida antes de publicar

Entorno local:

- Windows;
- JDK 21.0.10;
- Gradle Wrapper 9.5.0;
- Docker Desktop 4.93.0;
- Docker Engine 29.8.1;
- Testcontainers con PostgreSQL/PostGIS.

Comandos ejecutados:

```powershell
.\gradlew.bat test --rerun-tasks --no-daemon
.\gradlew.bat integrationTest --rerun-tasks --no-daemon
.\gradlew.bat check --no-daemon
```

Resultados:

| Grupo | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| Unitarias | 41 | 0 | 0 | 0 |
| Integración PostgreSQL/PostGIS | 19 | 0 | 0 | 0 |
| Total | 60 | 0 | 0 | 0 |

La integración cubrió login administrativo, cookie, restauración de sesión,
CSRF, WebSocket HTTP 101, consulta y mutación de reportes, asignación HTTP 204,
conflicto 409, logout, administrador desactivado, PostGIS, rollback atómico de
reporte e imagen y paginación con fechas empatadas.

El workflow `.github/workflows/build.yml` usa JDK 21 y ejecuta `./gradlew build`,
por lo que `check` incluye tanto las pruebas unitarias como las de integración.

## Contrato para la prueba conjunta

Android debe usar DNI y contraseña, conservar el Bearer solamente para la sesión
ciudadana y consumir `/api/v1/users/me/reports`, su detalle y su imagen. Un
ciudadano no puede consultar los reportes de otro.

Web debe conservar la cookie con credenciales, enviar `X-CSRF-Token` en las
mutaciones y usar los DTO publicados en OpenAPI. Las asignaciones responden 204;
estado y prioridad devuelven una respuesta parcial de mutación; el detalle debe
consultarse por separado. Después de reconectar el WebSocket, el panel debe
recargar el estado por REST porque el bus no conserva eventos.

## Trabajo fuera de este repositorio

El backend queda preparado para la integración. Las tareas restantes del sistema
completo pertenecen principalmente a los clientes y al entorno de despliegue:

- web: corregir CI/Node, separar DTO, conectar detalle e imagen reales,
  asignaciones, estadísticas, auditoría y WebSocket, y eliminar fallbacks
  simulados;
- Android: proteger el token frente a backup/transferencia, definir la URL HTTPS
  de release y completar la prueba física;
- equipo: configurar HTTPS/WSS, secretos, CORS, backup, observabilidad y ejecutar
  un E2E con dos ciudadanos y un administrador sobre una sola instancia.

El detalle de compatibilidad web y los criterios de aceptación se conserva en
`docs/AUDITORIA_WEB_Y_CIERRE_BACKEND_2026-10-19.md`. La auditoría vigente de los
tres repositorios está en `docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md`.
