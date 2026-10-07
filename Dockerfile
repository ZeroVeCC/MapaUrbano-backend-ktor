# Etapa de construccion
FROM gradle:8.7-jdk21-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Compilamos el Fat JAR omitiendo los tests para acelerar la imagen
RUN chmod +x gradlew && ./gradlew buildFatJar --no-daemon -x test

# Etapa de ejecucion
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
WORKDIR /app
# Copiamos el JAR generado desde la etapa de construccion
COPY --from=build /home/gradle/src/build/libs/mapa-urbano-backend.jar /app/ktor-app.jar

# Ejecutamos la aplicacion
ENTRYPOINT ["java", "-jar", "/app/ktor-app.jar"]
