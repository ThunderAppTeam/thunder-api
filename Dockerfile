FROM gradle:8.11-jdk21 AS builder
ARG MODULE_NAME
WORKDIR /app
COPY . .
RUN ./gradlew core:core-api:build -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/core/core-api/build/libs/*.jar /app/thunder-api.jar

RUN addgroup --system appuser && adduser --system --ingroup appuser appuser
USER appuser

EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/thunder-api.jar"]