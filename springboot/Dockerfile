FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle wrapper
RUN ./gradlew clean build --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh
EXPOSE 8080
ENTRYPOINT ["/wait-for-it.sh", "keycloak:8080", "--strict", "--timeout=60", "--", "java", "-jar", "/app/app.jar"] 