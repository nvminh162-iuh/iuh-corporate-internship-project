# ==============================================================================
# Multi-stage Dockerfile for TeleCare Backend Monolith (Spring Boot 4 / Java 21)
# ==============================================================================

# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Cache maven dependencies first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code and build application
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B --no-transfer-progress

# Stage 2: Minimal Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl tzdata

# Create dedicated non-root application user
RUN addgroup -S telecare && adduser -S telecare -G telecare

# Copy executable jar from builder stage
COPY --from=builder --chown=telecare:telecare /build/target/telecare-backend-0.0.1-SNAPSHOT.jar app.jar

USER telecare

ENV PORT=55080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 55080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:55080/api/v1/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
