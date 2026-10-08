# syntax=docker/dockerfile:1
# Imagen de un microservicio. El mismo Dockerfile construye cualquiera de ellos:
#   docker build --build-arg SERVICIO=citas-service -t veterinaria/citas-service .

# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
ARG SERVICIO
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/ common/
COPY auth-service/ auth-service/
COPY pacientes-service/ pacientes-service/
COPY citas-service/ citas-service/
COPY historias-service/ historias-service/

RUN --mount=type=cache,target=/root/.m2 \
    test -n "$SERVICIO" \
    && ./mvnw -B -q -pl "$SERVICIO" -am -DskipTests package \
    && cp "$SERVICIO"/target/"$SERVICIO"-*.jar /app/app.jar

# --- Run stage ---
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /app/app.jar app.jar

# Sin estado: cualquier réplica atiende cualquier petición (JWT, sin sesión en memoria).
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
