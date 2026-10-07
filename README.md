# Mapa Urbano - Backend Ktor

Este repositorio contiene el backend independiente (standalone) del proyecto Mapa Urbano, originalmente extraido del monorepo principal.

**Actualización:** registro/login de vecinos por **DNI + contraseña**. Ver [contrato, migración y pruebas](docs/AUTENTICACION_DNI.md). El informe del 1 de octubre es histórico; esta actualización documenta los cambios posteriores.

La [revisión de integración de los tres repositorios](docs/INTEGRACION_REPOSITORIOS_2026-10-06.md) identifica las ramas actuales de Android/web, diferencias de contrato y pruebas pendientes antes de conectarlos.

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

