# Mapa Urbano - Backend Ktor

Este repositorio contiene el backend independiente del proyecto Mapa Urbano, originalmente extraído del monorepo principal.

**Actualización:** registro/login de vecinos por **DNI + contraseña**. Ver [contrato, migración y pruebas](docs/AUTENTICACION_DNI.md). El informe del 1 de octubre es histórico; esta actualización documenta los cambios posteriores.

La [revisión de integración del 6 de octubre](docs/INTEGRACION_REPOSITORIOS_2026-10-06.md) queda como antecedente histórico de las ramas y diferencias detectadas antes de conectar Android.

La **[constancia de auditoría integral](docs/CONSTANCIA_AUDITORIA_INTEGRAL_2026-10-13.md)** registra la cronología del trabajo, el estado de cada requisito, las pruebas ejecutadas y la ruta priorizada para completar la integración.

La **[auditoría vigente de los tres repositorios principales del 19 de octubre](docs/AUDITORIA_TRES_REPOSITORIOS_2026-10-19.md)** compara backend, Android y web en sus `main` actuales. El monorepo original quedó retirado del alcance salvo pedido expreso del usuario.

La **[auditoría específica del frontend web del 21 de octubre](docs/AUDITORIA_WEB_2026-10-21.md)** verifica nuevamente el `main` web, sus pruebas, build, CI, dependencias y compatibilidad operación por operación con el backend publicado.

La **[entrega del backend en `main` del 20 de octubre](docs/ENTREGA_BACKEND_MAIN_2026-10-20.md)** registra el corte preparado para la prueba conjunta, las 60 pruebas correctas y el contrato que deben consumir Android y web.

La **[guía de Swagger y del contrato OpenAPI](docs/SWAGGER.md)** explica cómo probar la API desde `http://localhost:8080/swagger`, cómo autorizarse con el token ciudadano y cómo interpretar el estado real de cada endpoint.

El **[cierre del backend y plan de adecuación web del 18 de octubre](docs/CIERRE_BACKEND_Y_PLAN_WEB_2026-10-18.md)** registra la implementación de privacidad, administración, CSRF, consultas, pruebas PostGIS y el mensaje de trabajo que debe recibir el equipo web.

Las **[instrucciones detalladas para las IA de Android y web](docs/TAREAS_IA_ANDROID_Y_WEB_2026-10-18.md)** parten de los últimos `main` revisados y contienen tareas, contratos, pruebas y criterios de aceptación listos para copiar a cada equipo.

El **[reescaneo anterior del 19 de octubre](docs/REESCANEO_REPOSITORIOS_2026-10-19.md)** registra los dos commits de integración privada de Android y queda como corte histórico anterior a los cambios web posteriores.

El **[reescaneo y plan de privacidad del 15 de octubre](docs/REESCANEO_Y_PLAN_PRIVACIDAD_2026-10-15.md)** registra las revisiones actuales de los cuatro repositorios, el problema de visibilidad global de reportes y las tareas separadas para backend, Android y web.

El **[reescaneo específico del frontend web del 18 de octubre](docs/REESCANEO_WEB_2026-10-18.md)** compara los tres commits nuevos con Swagger, registra build, pruebas y GitHub Actions, y deja el trabajo pendiente priorizado para el responsable web.

## 🏗 Arquitectura y tecnologías

El backend está desarrollado en Kotlin y utiliza las siguientes tecnologías:

- **Framework web:** [Ktor](https://ktor.io/) con Netty
- **Inyección de dependencias:** [Koin](https://insert-koin.io/)
- **Base de datos y ORM:** PostgreSQL + PostGIS con [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- **Migraciones:** [Flyway](https://flywaydb.org/)
- **Conexiones:** HikariCP
- **Autenticación y seguridad:** sesiones y Bearer tokens de Ktor, con contraseñas protegidas mediante BCrypt.

## 📦 Proceso de extracción

Este backend fue extraído de un monorepo que contenía la web y la aplicación Android. Los cambios principales durante la extracción fueron:

1. **Independencia de migraciones:** los scripts SQL de Flyway se encuentran en `src/main/resources/db/migration`. Flyway los detecta y ejecuta automáticamente al iniciar la aplicación.
2. **Desacoplamiento:** se eliminaron las referencias a tareas externas en `build.gradle.kts`. El proyecto compila de manera independiente.

## ⚙️ Configuración y variables de entorno

El servidor está configurado mediante `src/main/resources/application.conf`. Soporta valores locales predeterminados y se adapta a producción mediante variables de entorno:

- PORT: Puerto donde corre el servidor Ktor (por defecto 8080).
- DATABASE_JDBC_URL: URL de la base de datos (ej. jdbc:postgresql://localhost:5432/mapa_urbano).
- DATABASE_USER: Usuario de PostgreSQL (por defecto postgres).
- DATABASE_PASSWORD: Contraseña de PostgreSQL.
- SESSION_SECRET: Secreto para firmar las sesiones de Ktor.
- SESSION_SECURE_COOKIE: `true` en producción para limitar la cookie administrativa a HTTPS.
- CORS_ALLOWED_HOSTS: hosts del frontend separados por coma, sin esquema (por ejemplo `panel.municipio.gob.ar`).
- ADMIN_BOOTSTRAP_USERNAME y ADMIN_BOOTSTRAP_PASSWORD: crean el primer administrador si ambos están definidos y todavía no existe.

## 🚀 Cómo ejecutar el proyecto

### Prerrequisitos

- JDK 21
- PostgreSQL con la extensión PostGIS habilitada (`CREATE EXTENSION postgis;`).
- Docker Desktop para las pruebas de integración con Testcontainers.

### Comandos de Gradle
El proyecto incluye el wrapper de Gradle (gradlew), por lo que no necesitas tener Gradle instalado globalmente.

- **Compilar y ejecutar todas las pruebas:** `./gradlew build`
- **Ejecutar pruebas unitarias:** `./gradlew test`
- **Ejecutar integración PostgreSQL/PostGIS:** `./gradlew integrationTest`
- **Correr el servidor localmente:** `./gradlew run`

En Windows se usa `gradlew.bat`. Al ejecutar el servidor, la aplicación se conecta a la base configurada y aplica automáticamente las migraciones pendientes de Flyway antes de aceptar tráfico.

## 📚 Documentación detallada

Para conocer la arquitectura y las decisiones del proyecto, consulta **[Estado actual y decisiones arquitectónicas](docs/ESTADO_ACTUAL_Y_DECISIONES.md)**.
