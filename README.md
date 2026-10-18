# Mapa Urbano - Backend Ktor

Este repositorio contiene el backend independiente (standalone) del proyecto Mapa Urbano, originalmente extraido del monorepo principal.

**Actualización:** registro/login de vecinos por **DNI + contraseña**. Ver [contrato, migración y pruebas](docs/AUTENTICACION_DNI.md). El informe del 1 de octubre es histórico; esta actualización documenta los cambios posteriores.

La [revisión de integración del 6 de octubre](docs/INTEGRACION_REPOSITORIOS_2026-10-06.md) queda como antecedente histórico de las ramas y diferencias detectadas antes de conectar Android.

La **[constancia de auditoría integral, actualizada el 18 de octubre de 2026](docs/CONSTANCIA_AUDITORIA_INTEGRAL_2026-10-13.md)** registra las revisiones exactas de los cuatro repositorios, incluida la integración Android del PR #1, la cronología del trabajo, el estado de cada requisito, las pruebas ejecutadas y la ruta priorizada para completar la integración.

La **[guía de Swagger y del contrato OpenAPI](docs/SWAGGER.md)** explica cómo probar la API desde `http://localhost:8080/swagger`, cómo autorizarse con el token ciudadano y cómo interpretar el estado real de cada endpoint.

El **[cierre del backend y plan de adecuación web del 18 de octubre](docs/CIERRE_BACKEND_Y_PLAN_WEB_2026-10-18.md)** registra la implementación de privacidad, administración, CSRF, consultas, pruebas PostGIS y el mensaje de trabajo que debe recibir el equipo web.

El **[reescaneo y plan de privacidad del 15 de octubre](docs/REESCANEO_Y_PLAN_PRIVACIDAD_2026-10-15.md)** registra las revisiones actuales de los cuatro repositorios, el problema de visibilidad global de reportes y las tareas separadas para backend, Android y web.

El **[reescaneo específico del frontend web del 18 de octubre](docs/REESCANEO_WEB_2026-10-18.md)** compara los tres commits nuevos con Swagger, registra build, pruebas y GitHub Actions, y deja el trabajo pendiente priorizado para el responsable web.

## 🏗 Arquitectura y Tecnologias

El backend esta desarrollado completamente en Kotlin y utiliza las siguientes tecnologias:
- **Framework Web:** [Ktor](https://ktor.io/) (Netty engine)
- **Inyeccion de Dependencias:** [Koin](https://insert-koin.io/)
- **Base de Datos y ORM:** PostgreSQL + PostGIS (datos geoespaciales) con [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- **Migraciones:** [Flyway](https://flywaydb.org/)
- **Conexiones:** HikariCP
- **Autenticacion y Seguridad:** Sesiones y Bearer Tokens de Ktor, encriptacion con BCrypt.

## 📦 Proceso de Extraccion

Este backend fue extraido de un monorepo que contenia la Web y la App. Los cambios principales durante la extraccion fueron:
1. **Independencia de Migraciones:** Los scripts SQL de Flyway (anteriormente externos) fueron movidos nativamente a src/main/resources/db/migration. Flyway los detecta y ejecuta automaticamente al iniciar la aplicacion.
2. **Desacoplamiento:** Se eliminaron las referencias a tareas externas en el uild.gradle.kts. El proyecto ahora compila de manera 100% independiente.

## ⚙️ Configuracion y Variables de Entorno

El servidor esta configurado a traves del archivo src/main/resources/application.conf. Soporta configuracion local por defecto, pero se adapta a produccion mediante las siguientes variables de entorno:

- PORT: Puerto donde corre el servidor Ktor (por defecto 8080).
- DATABASE_JDBC_URL: URL de la base de datos (ej. jdbc:postgresql://localhost:5432/mapa_urbano).
- DATABASE_USER: Usuario de PostgreSQL (por defecto postgres).
- DATABASE_PASSWORD: Contrasena de PostgreSQL.
- SESSION_SECRET: Secreto para firmar las sesiones de Ktor.
- SESSION_SECURE_COOKIE: `true` en producción para limitar la cookie administrativa a HTTPS.
- CORS_ALLOWED_HOSTS: hosts del frontend separados por coma, sin esquema (por ejemplo `panel.municipio.gob.ar`).
- ADMIN_BOOTSTRAP_USERNAME y ADMIN_BOOTSTRAP_PASSWORD: crean el primer administrador si ambos están definidos y todavía no existe.

## 🚀 Como ejecutar el proyecto

### Prerrequisitos
- JDK 21
- PostgreSQL con la extension PostGIS habilitada (CREATE EXTENSION postgis;).

### Comandos de Gradle
El proyecto incluye el wrapper de Gradle (gradlew), por lo que no necesitas tener Gradle instalado globalmente.

- **Compilar el proyecto:**
  `ash
  ./gradlew build
  `
- **Ejecutar pruebas unitarias:**
  `ash
  ./gradlew test
  `
- **Correr el servidor localmente:**
  `ash
  ./gradlew run
  `
  *(Nota: Al ejecutar, la aplicacion intentara conectarse a la base de datos y ejecutara automaticamente las migraciones pendientes de Flyway antes de levantar el servidor).*

## 📚 Documentacion Detallada

Para conocer a fondo el estado actual del repositorio, los cambios recientes, la arquitectura exacta y como continuar colaborando, por favor lee: **[Estado Actual y Decisiones Arquitectonicas](docs/ESTADO_ACTUAL_Y_DECISIONES.md)**.

