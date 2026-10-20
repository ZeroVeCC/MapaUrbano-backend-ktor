# 5. Diagramas de modelado

Los diagramas Mermaid se visualizan directamente en GitHub. Son modelos didácticos del flujo observado en el código, no una certificación de ejecución de extremo a extremo.

## 5.1 Componentes

```mermaid
flowchart LR
  C[Ciudadano / Android] --> API[API Ktor]
  A[Administrador / Web] --> API
  API --> UC[Casos de uso]
  UC --> DB[(PostgreSQL + PostGIS)]
  API --> WS[Notificaciones WebSocket]
```

## 5.2 Casos de uso (representación funcional)

```mermaid
flowchart LR
  V[Ciudadano] --> R[Registrarse / ingresar]
  V --> CR[Crear reporte]
  V --> VR[Consultar reportes propios]
  M[Administrador] --> L[Ingresar al panel]
  M --> E[Modificar estado / prioridad]
  M --> EQ[Gestionar equipos]
  M --> ST[Consultar estadísticas]
```

Este segundo gráfico es un **mapa de casos de uso**, no un diagrama UML formal de óvalos y relaciones `include`/`extend`.

## 5.3 Secuencia: creación de reporte

```mermaid
sequenceDiagram
  actor Vecino
  participant Android
  participant API as Ktor / Ruta
  participant Caso as CreateReportUseCase
  participant DB as PostgreSQL
  Vecino->>Android: Completa formulario
  Android->>API: POST /api/v1/reports + Bearer
  API->>API: Valida identidad y campos
  API->>Caso: execute(...)
  Caso->>DB: Persiste reporte mediante repositorio
  DB-->>Caso: Resultado
  Caso-->>API: Reporte creado
  API-->>Android: 201 Created
```

El tratamiento opcional de imágenes está omitido aquí para mantener legible el flujo principal; ver `reports/api/PublicReportRoutes.kt` y `media/`.

[Volver al índice](README.md)
