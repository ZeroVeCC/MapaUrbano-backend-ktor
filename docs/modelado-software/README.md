# Modelado de Software — Backend de Mapa Urbano

**Entrega documental para la materia Modelado de Software.** Corte de análisis: ZIP del repositorio `MapaUrbano-backend-ktor-main` compartido el 20/10/2026. Preparado por Mauro para revisión del equipo.

> Esta documentación describe el código disponible y modelos conceptuales simplificados. No certifica despliegue ni pruebas ejecutadas por el autor de estos documentos. No reemplaza la documentación técnica preexistente.

**Actualización del equipo — 20/10/2026:** el modelado fue contrastado con el
cierre funcional integrado después del ZIP. La atomicidad reporte-imagen, la
cookie del WebSocket administrativo y los resultados de pruebas se actualizaron
con la evidencia de `docs/ENTREGA_BACKEND_MAIN_2026-10-20.md`.

## Índice

1. [Arquitectura del sistema](01-arquitectura.md)
2. [Módulos y responsabilidades](02-modulos.md)
3. [Actores y casos de uso](03-casos-de-uso.md)
4. [Modelo conceptual de datos](04-modelo-de-datos.md)
5. [Diagramas de modelado](05-diagramas.md)
6. [Estado y límites de la revisión](06-estado-del-proyecto.md)

## Fuentes del repositorio

- Código: `src/main/kotlin/com/mapaurbano/`
- Rutas: `src/main/kotlin/com/mapaurbano/application/Routing.kt`
- Esquema: `src/main/resources/db/migration/` y `src/main/kotlin/com/mapaurbano/database/`
- Contrato API: `src/main/resources/openapi/documentation.yaml`
- Auditoría previa: [Auditoría de tres repositorios del 19/10](../AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md)

**Importante:** la propuesta histórica de reportes ciudadanos anónimos fue modificada: en el código actual, la creación ciudadana requiere autenticación. Los reportes anónimos históricos se mencionan en las auditorías, pero no deben describirse como flujo ciudadano nuevo habilitado.
