# 3. Actores y casos de uso

## Actores

- **Ciudadano autenticado:** se registra o inicia sesión y gestiona sus reportes propios.
- **Administrador municipal:** consulta y administra reportes, categorías, equipos y estadísticas mediante rutas protegidas.
- **Servicios de persistencia:** participan como componentes técnicos, no como actores humanos.

## Casos de uso principales

| ID | Actor | Caso | Resultado esperado |
|---|---|---|---|
| CU-01 | Ciudadano | Registrarse / iniciar sesión | Obtiene acceso autenticado |
| CU-02 | Ciudadano | Crear reporte | Se valida y registra una incidencia con ubicación |
| CU-03 | Ciudadano | Consultar reportes propios | Accede solo a reportes autorizados |
| CU-04 | Administrador | Iniciar sesión | Accede a rutas administrativas protegidas |
| CU-05 | Administrador | Cambiar estado o prioridad | Se actualiza un reporte sujeto a validaciones |
| CU-06 | Administrador | Gestionar equipos/asignaciones | Organiza atención de incidencias |
| CU-07 | Administrador | Consultar estadísticas y auditoría | Revisa actividad y datos agregados |

## CU-02: Crear reporte — descripción

**Precondición:** el ciudadano tiene una sesión Bearer válida. **Entrada:** título, descripción, categoría, latitud, longitud y foto opcional. **Flujo principal:** Android envía el formulario; el backend identifica al usuario, valida datos y ejecuta la creación; si hay imagen, la procesa; devuelve una respuesta de creación. **Alternativas:** sesión ausente o inválida, coordenadas incorrectas, datos insuficientes o imagen rechazada. **Resultado:** reporte persistido si la operación concluye correctamente.

**Precisión:** este es el flujo pretendido a partir de las rutas. La consistencia entre creación de reporte e imagen ante fallos intermedios debe validarse por separado; no se afirma atomicidad de ambas operaciones.

[Volver al índice](README.md)
