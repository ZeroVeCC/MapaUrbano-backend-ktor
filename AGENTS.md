# Contexto obligatorio para trabajar en este repositorio

Antes de analizar o modificar el proyecto, leer
`docs/CONSTANCIA_AUDITORIA_INTEGRAL_2026-10-13.md`. Allí están registradas las
revisiones auditadas de backend, Android y web, junto con las pruebas ejecutadas,
diferencias de contrato y tareas pendientes. Las menciones al monorepo original
se conservan solamente como historia del proyecto.

Para el estado vigente, leer también
`docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md`, que reemplaza los cortes
anteriores cuando exista una contradicción.

Reglas de continuidad acordadas con el usuario:

- Este checkout corresponde al backend Ktor y es el único repositorio que se
  debe modificar, salvo que el usuario indique expresamente otra cosa.
- Los repositorios Android y web se usan como referencias de solo lectura para
  evaluar compatibilidad.
- El monorepo original `Jquincal/Mapa-Urbano` queda fuera de las auditorías,
  comparaciones y planes futuros. Consultarlo únicamente cuando el usuario lo
  pida de forma expresa.
- La autenticación ciudadana vigente es DNI + contraseña.
- Antes de subir cambios funcionales del backend, ejecutar las pruebas que
  correspondan; para persistencia usar también la integración PostgreSQL/PostGIS.
- Los cambios autorizados para este proyecto se publican en `main` y luego se
  verifica el resultado de GitHub Actions.

Actualizar la constancia cuando cambien las ramas auditadas, el contrato entre
componentes o el estado de una etapa importante de integración.
