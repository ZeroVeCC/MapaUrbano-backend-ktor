# Estado Actual, Cambios y Decisiones Arquitectonicas

Este documento sirve como registro principal de todo el trabajo realizado en este repositorio y como punto de entrada de lectura (contexto) para cualquier miembro del equipo o asistente de Inteligencia Artificial que revise el proyecto.

## 1. Contexto y Origen

Este proyecto nacio como una extraccion (spin-off) de un monorepo general llamado Mapa-Urbano. El objetivo principal fue independizar el backend de Ktor para aislar la logica de servidor, dependencias y despliegues (CI/CD) de los equipos de Frontend y Mobile. 

A partir de la extraccion, este repositorio es 100% *standalone* (independiente).

## 2. Decisiones Arquitectonicas Base

- **Framework Core:** [Ktor](https://ktor.io/) con Netty.
- **Inyeccion de Dependencias:** Se usa [Koin](https://insert-koin.io/). Todo nuevo servicio o UseCase debe registrarse en src/main/kotlin/com/mapaurbano/application/DiModules.kt.
- **Base de Datos y ORM:** Utilizamos PostgreSQL junto con su extension geoespacial **PostGIS**. Las interacciones en Kotlin se hacen con [JetBrains Exposed](https://github.com/JetBrains/Exposed).
- **Tipos Geograficos:** Se creo un tipo de columna custom (GeographyPointColumnType y GeoPoint) para lidiar con PostGIS usando EWKT crudo. Esto se decidio para evitar dependencias inestables como postgis-jdbc.
- **Migraciones:** Administradas por [Flyway](https://flywaydb.org/). Toda alteracion de la BD debe escribirse en un script SQL puro en src/main/resources/db/migration siguiendo la convencion de nombres (ej. V3__descripcion.sql).
- **Autenticacion:** Basada en Bearer Tokens / Sesiones Ktor usando BCrypt para los hashes de contrasena.

## 3. Ultimos Cambios y Funcionalidades Anadidas (Changelog)

Las siguientes tareas criticas fueron implementadas y validadas por el equipo:

1. **Entorno Local y Docker:**
   - Se crearon un Dockerfile (multi-stage super ligero usando Alpine y JRE 21) y un docker-compose.yml.
   - Levantar el proyecto es tan facil como ejecutar docker-compose up --build.

2. **Integracion Continua (CI):**
   - GitHub Actions se encarga de probar cada commit (.github/workflows/build.yml) ejecutando ./gradlew build.

3. **CORS y SwaggerUI:**
   - Configuracion de CORS activa en Plugins.kt (para habilitar Web en localhost:3000 y localhost:5173).
   - Generacion e integracion de SwaggerUI en el endpoint /swagger a traves de openapi/documentation.yaml.

4. **Upload de Imagenes Multipart (Static Storage):**
   - Se implemento el guardado local de imagenes. Ktor ahora sirve estaticamente el contenido de la carpeta /uploads.
   - Se desarrollo StorageService para guardar las imagenes que llegan como PartData.FileItem y generarles nombres unicos via UUID.
   - **Endpoint de Reportes:** POST /api/v1/reports ahora soporta multipart/form-data. Escanea los campos de texto para armar el modelo y si encuentra una image, la guarda localmente, obtiene la URL y la inserta en la base de datos de PostgreSQL mediante un update en el esquema (V3__add_image_url_to_reports.sql).

## 4. Guia para IAs y Nuevos Desarrolladores

Si eres una Inteligencia Artificial analizando este documento para entender como proseguir, ten en cuenta estas reglas:
- **No generes mocks** para la base de datos, el flujo de Koin + Exposed + Postgres ya es robusto y esta operativo en produccion/local.
- Antes de modificar el modelo de base de datos (Tables.kt), recuerda siempre crear el archivo de migracion .sql correspondiente para Flyway.
- **Ruteo:** El enrutador principal esta en Routing.kt, pero este delega en sub-rutas por area (ej. PublicReportRoutes.kt, AuthRoutes.kt).
- **Media:** Si necesitas modificar como se suben imagenes en otros endpoints (ej. perfil de usuario), reutiliza StorageService.

## 5. Posibles Siguientes Pasos (Roadmap sugerido)

- **Panel Administrativo:** Completar o refinar las rutas bajo /admin para gestionar las asignaciones, estadisticas y eliminacion logica de reportes.
- **Notificaciones Real-Time:** Terminar la integracion del WebSocketRoutes.kt con nuestro EventBus en memoria para emitir un evento cada vez que cambia el estado de un reporte.
- **Despliegue a Produccion:** Configurar los *Secrets* en GitHub Actions para orquestar un despliegue hacia un servidor Cloud o un registro de imagenes Docker.
