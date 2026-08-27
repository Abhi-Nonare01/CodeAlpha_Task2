# Multi-stage build for lightweight production container
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/stock-trading-platform-1.0.0.jar app.jar

# Expose default web server port
EXPOSE 8080
ENV PORT=8080

# Run standalone platform
ENTRYPOINT ["java", "-jar", "app.jar"]
