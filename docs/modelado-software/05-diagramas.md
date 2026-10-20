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
  participant Imagen as ProcessImageUseCase
  participant Repo as ReportSubmissionRepository
  participant DB as PostgreSQL
  Vecino->>Android: Completa formulario
  Android->>API: POST /api/v1/reports + Bearer
  API->>API: Valida identidad Bearer y multipart
  API->>Caso: execute(campos, imagen)
  Caso->>Imagen: Prepara y valida evidencia opcional
  Imagen-->>Caso: ReportImage o sin imagen
  Caso->>Repo: create(reporte, imagen)
  Repo->>DB: INSERT reporte + imagen en una transacción
  DB-->>Repo: Commit
  Repo-->>Caso: Reporte creado
  Caso->>Caso: Publica report.created
  Caso-->>API: Reporte creado
  API-->>Android: 201 Created
```

Si una inserción falla, PostgreSQL revierte la transacción completa y el evento no
se publica. Ver `reports/api/PublicReportRoutes.kt`, `CreateReportUseCase` y
`ReportSubmissionRepositoryImpl`.

[Volver al índice](README.md)
