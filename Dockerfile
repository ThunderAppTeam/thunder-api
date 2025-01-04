FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon

FROM openjdk:21-jdk-slim-buster
WORKDIR /app

COPY --from=builder /app/build/libs/thunder-api-0.0.1.jar /app/thunder-api-0.0.1.jar
COPY .env.production /app/.env

EXPOSE 9000
ENTRYPOINT ["/bin/sh", "-c", "set -a && . /app/.env && exec java -jar /app/thunder-api-0.0.1.jar"]