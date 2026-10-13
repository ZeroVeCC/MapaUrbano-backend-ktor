# Contexto obligatorio para trabajar en este repositorio

Antes de analizar o modificar el proyecto, leer
`docs/CONSTANCIA_AUDITORIA_INTEGRAL_2026-10-13.md`. Allí están registradas las
revisiones auditadas de backend, Android, web y monorepo original, junto con las
pruebas ejecutadas, diferencias de contrato y tareas pendientes.

Reglas de continuidad acordadas con el usuario:

- Este checkout corresponde al backend Ktor y es el único repositorio que se
  debe modificar, salvo que el usuario indique expresamente otra cosa.
- Los repositorios Android, web y el monorepo original se usan como referencia
  de lectura para evaluar compatibilidad.
- La autenticación ciudadana vigente es DNI + contraseña.
- Antes de subir cambios funcionales del backend, ejecutar las pruebas que
  correspondan; para persistencia usar también la integración PostgreSQL/PostGIS.
- Los cambios autorizados para este proyecto se publican en `main` y luego se
  verifica el resultado de GitHub Actions.

Actualizar la constancia cuando cambien las ramas auditadas, el contrato entre
componentes o el estado de una etapa importante de integración.
