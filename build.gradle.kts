plugins {
    application
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("io.ktor.plugin") version "3.5.1"
}

group = "com.mapaurbano"
version = "0.1.0-SNAPSHOT"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

val exposedVersion = "0.54.0"
val flywayVersion = "13.2.0"
val testcontainersVersion = "2.0.5"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-rate-limit")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-server-csrf")
    implementation("io.ktor:ktor-server-swagger")

    // Database â€“ Exposed ORM + connection pool
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.postgresql:postgresql:42.7.13")

    // Dependency Injection
    val koinVersion = "3.5.6"
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Security â€“ password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.33")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
}

tasks.test {
    useJUnitPlatform { excludeTags("database") }
}



val integrationTest by tasks.registering(Test::class) {
    description = "Verifica migraciones y restricciones sobre PostgreSQL/PostGIS real (requiere Docker)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("database") }
    shouldRunAfter(tasks.test)
}

tasks.check { dependsOn(integrationTest) }

tasks.register<JavaExec>("migrateDatabase") {
    description = "Aplica Flyway a la base aprovisionada indicada por DATABASE_JDBC_URL, DATABASE_USER y DATABASE_PASSWORD."
    group = "database"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.mapaurbano.database.DatabaseMigrationsKt")
}

ktor {
    fatJar {
        archiveFileName.set("mapa-urbano-backend.jar")
    }
}
