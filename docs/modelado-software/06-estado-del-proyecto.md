# 6. Estado del proyecto y alcance de esta entrega

## Evidencia revisada

- Código Kotlin y estructura del ZIP recibido el 20/10/2026.
- `README.md`, `application/Routing.kt`, `reports/api/PublicReportRoutes.kt` y `reports/domain/Report.kt`.
- `docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md` y `docs/ESTADO_ACTUAL_Y_DECISIONES.md`.

## Implementación y límites

El repositorio contiene código de registro/autenticación ciudadana, rutas de reportes propios, operaciones administrativas, categorías, equipos, estadísticas, auditoría, imágenes y notificaciones. **Que exista una ruta o módulo no prueba por sí solo que la integración completa esté funcionando.**

La auditoría fechada el 19/10 informa pruebas del backend y un CI correcto en el corte allí analizado, pero advierte una incompatibilidad de cookie con WebSocket administrativo, integración web pendiente y necesidad de una demostración extremo a extremo confiable. Son resultados **reportados por el equipo en ese documento**, no pruebas repetidas durante esta elaboración.

## Cambios frente a diseños iniciales

- Los ciudadanos actuales deben autenticarse con DNI y contraseña; no describir el alta anónima como disponible.
- El almacenamiento actual de fotografías usa `BYTEA` en PostgreSQL según el registro de decisiones; la propuesta S3 pertenece a documentación histórica.
- El backend es independiente del frontend Android y web.

## Pendientes de validación para la entrega académica

1. Confirmar con el docente si exige notación UML formal.
2. Validar nombres y cardinalidades del diagrama de datos contra las migraciones antes de tratarlo como modelo físico.
3. Revisar con Juan que no haya cambios posteriores al ZIP.
4. Confirmar con el equipo qué diagramas presentará en la exposición.

## Referencias internas

- [Auditoría 19/10](../AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md)
- [Autenticación DNI](../AUTENTICACION_DNI.md)
- [Swagger](../SWAGGER.md)

[Volver al índice](README.md)
