FROM gradle:8.11-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon

FROM openjdk:21-jdk-slim-buster
WORKDIR /app

ARG ENV_FILE_PATH

COPY --from=builder /app/build/libs/thunder-api*.jar /app/thunder-api.jar
COPY ${ENV_FILE_PATH} /app/.env

EXPOSE 9000
ENTRYPOINT ["/bin/sh", "-c", "set -a && . /app/.env && exec java -jar /app/thunder-api.jar"]