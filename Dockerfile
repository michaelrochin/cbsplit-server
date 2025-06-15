# Multi-stage build for CBSplit Server
FROM gradle:8.3-jdk11 AS build

# Copy source code
WORKDIR /app
COPY build.gradle.kts .
COPY src/ src/
COPY functions/ src/main/kotlin/functions/

# Build the application
RUN gradle shadowJar --no-daemon

# Runtime stage
FROM openjdk:11-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN useradd -m -u 1001 cbsplit

# Copy jar from build stage
WORKDIR /app
COPY --from=build /app/build/libs/cbsplit-server.jar .

# Change ownership to app user
RUN chown -R cbsplit:cbsplit /app
USER cbsplit

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Start the application
CMD ["java", "-Xmx512m", "-jar", "cbsplit-server.jar"]