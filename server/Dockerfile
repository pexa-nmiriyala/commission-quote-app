# Expects the jar to be pre-built on the host via:
#   ./gradlew bootJar --no-daemon -x test
#
# Build the image with:
#   docker build -t commission-quote-server .
#
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
