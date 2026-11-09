FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI (they need Docker for Testcontainers).
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"UP"' || exit 1
# The free plan has 512 MB; the default heap (25% of RAM) is too small for Spring Boot, so allow 75%.
# Dates like "today" in the books are Lima dates, not the server's UTC ones (Lima is 5 hours behind).
ENTRYPOINT ["java", "-Duser.timezone=America/Lima", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
