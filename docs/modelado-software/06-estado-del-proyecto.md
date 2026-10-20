# 6. Estado del proyecto y alcance de esta entrega

## Evidencia revisada

- Código Kotlin y documentación incorporada por Mauro en `b0478e4`.
- `README.md`, `application/Routing.kt`, `reports/api/PublicReportRoutes.kt` y `reports/domain/Report.kt`.
- `docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md`,
  `docs/ENTREGA_BACKEND_MAIN_2026-10-20.md` y
  `docs/ESTADO_ACTUAL_Y_DECISIONES.md`.

## Implementación y límites

El repositorio contiene código de registro/autenticación ciudadana, rutas de reportes propios, operaciones administrativas, categorías, equipos, estadísticas, auditoría, imágenes y notificaciones. **Que exista una ruta o módulo no prueba por sí solo que la integración completa esté funcionando.**

El cierre del 20/10 corrige la incompatibilidad de cookie del WebSocket
administrativo, confirma persistencia atómica de reporte e imagen e incorpora
cursores fecha+UUID. La verificación local fue repetida con JDK 21, Docker y
PostgreSQL/PostGIS: 41 pruebas unitarias y 19 integraciones terminaron sin fallos.
La integración web y la demostración extremo a extremo de los tres componentes
continúan pendientes.

## Cambios frente a diseños iniciales

- Los ciudadanos actuales deben autenticarse con DNI y contraseña; no describir el alta anónima como disponible.
- El almacenamiento actual de fotografías usa `BYTEA` en PostgreSQL según el registro de decisiones; la propuesta S3 pertenece a documentación histórica.
- El backend es independiente del frontend Android y web.

## Pendientes de validación para la entrega académica

1. Confirmar con el docente si exige notación UML formal.
2. Validar nombres y cardinalidades del diagrama de datos contra las migraciones antes de tratarlo como modelo físico.
3. Usar el `main` publicado como fuente y revisar cambios posteriores antes de la exposición.
4. Confirmar con el equipo qué diagramas presentará en la exposición.

## Referencias internas

- [Auditoría 19/10](../AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md)
- [Entrega backend 20/10](../ENTREGA_BACKEND_MAIN_2026-10-20.md)
- [Autenticación DNI](../AUTENTICACION_DNI.md)
- [Swagger](../SWAGGER.md)

[Volver al índice](README.md)
