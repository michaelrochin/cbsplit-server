# Use OpenJDK with Gradle pre-installed
FROM gradle:8.3-jdk17 AS build

# Copy source code
WORKDIR /app
COPY . .

# Fix source structure for Railway
RUN mkdir -p src/main/kotlin/com/cbsplit
RUN cp src/main/kotlin/com/cbsplit/Main.kt src/main/kotlin/com/cbsplit/Main.kt 2>/dev/null || true
RUN cp -r functions src/main/kotlin/ 2>/dev/null || true

# Build the application
RUN gradle clean shadowJar --no-daemon

# Runtime stage  
FROM openjdk:17-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy jar from build stage
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Start the application
CMD ["java", "-Xmx512m", "-jar", "app.jar"]