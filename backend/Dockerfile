# syntax=docker/dockerfile:1

# Build Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY config ./config
COPY src ./src

# Testcontainers tests run separately where Docker is available.
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    mvn -B -DskipTests verify

# Run Spring Boot application
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
