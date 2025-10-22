# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cache optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S labyrinth && \
    adduser -u 1001 -S labyrinth -G labyrinth

# Copy JAR from build stage
COPY --from=build /app/target/labyrinth-server.jar ./labyrinth-server.jar

# Change ownership
RUN chown -R labyrinth:labyrinth /app

# Switch to non-root user
USER labyrinth

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "labyrinth-server.jar"]
