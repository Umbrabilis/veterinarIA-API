# veterinarIA-API

Backend del software de gestión para clínicas veterinarias. Contexto de negocio en `../docs/brief.md`
(si existe en tu máquina); lo esencial está resumido aquí.

## Stack (no cambiar sin discusión del equipo)
Java 21, Spring Boot 4.1.1, Spring Framework 7, Spring Security 7, PostgreSQL 16, Flyway, Maven, Lombok.
Jackson 3: los imports son `tools.jackson.*`, NUNCA `com.fasterxml.jackson.*`
(excepto `com.fasterxml.jackson.annotation.*`, que sigue en ese paquete).
Ante dudas de API de Spring Boot 4, Spring Security 7 o Jackson 3, consultar con Context7 antes de escribir código:
casi todo el material anterior a finales de 2025 describe Boot 3 y está desactualizado.

## Comandos
- `./mvnw verify` — compila y corre todos los tests. Debe pasar antes de dar una tarea por terminada.
- `docker compose up -d` — PostgreSQL local (y la API) según `docker-compose.yml`.
- Los tests necesitan Docker en ejecución (Testcontainers).

## Estructura
Paquetes por módulo de dominio bajo `tech.veterinaria_api`: `auth`, `usuarios`, `common`, `config`, `security`.
Cada módulo nuevo sigue el mismo patrón: `XController` → `XService` → `XRepository`, DTOs en `x/dto/` como records.
Errores de negocio como excepciones en `common/` mapeadas en `GlobalExceptionHandler` a `ApiError`.
API versionada bajo `/api/v1/`.

## Base de datos
- `spring.jpa.hibernate.ddl-auto=validate`: el esquema SOLO cambia vía Flyway (`src/main/resources/db/migration`).
- Migraciones nuevas con versión por timestamp: `V202609081430__descripcion.sql`
  (evita choques de número entre los 3 desarrolladores backend).
- NUNCA modificar una migración ya mergeada en `main`. Se corrige con una nueva migración.
- H2 prohibido: el esquema usará características propias de PostgreSQL (EXCLUDE USING gist para la agenda,
  JSONB, tsvector). Todo test que toque SQL corre contra PostgreSQL real con Testcontainers.
- `open-in-view=false`: cargar lo necesario dentro del servicio transaccional, no en el controlador.

## Tests
- Integración con Testcontainers usando `TestcontainersConfiguration` (`@Import(TestcontainersConfiguration.class)`).
  Ejemplo de referencia: `auth/AuthControllerTest`.
- Nunca mocks de repositorio para probar SQL.
- Planeado (aún no está en el pom): reglas de arquitectura con ArchUnit. Cuando exista, si un test ArchUnit falla
  se corrige el código, no el test.

## Seguridad
- Autenticación con OAuth2 Resource Server de Spring Security. Los JWT se emiten con `NimbusJwtEncoder`
  (`security/JwtKeysConfig`). No usar jjwt ni validar JWT a mano.
- El rol viaja en el claim `rol` y se mapea a `ROLE_<rol>` en `SecurityConfig`.
- Todo endpoint nuevo requiere autenticación por defecto; las rutas públicas se listan explícitamente en `RUTAS_PUBLICAS`.
- Autorización por pertenencia, no solo por rol: un usuario no puede leer recursos de otra clínica/propietario
  cambiando un ID en la URL.
- Datos personales de propietarios (nombre, teléfono, correo, dirección) nunca en logs ni en mensajes de error.
  Ley 1581 de 2012 (habeas data, Colombia).

## Reglas de negocio críticas
- Los resúmenes generados por IA NUNCA se envían al dueño sin aprobación del veterinario.
  Esta regla se refuerza en la base de datos; no crear ningún atajo que la evite.
- Los datos personales del propietario NUNCA se envían al modelo de IA: el prompt recibe solo datos clínicos y de la mascota.
- Se registra modelo, versión de prompt, contenido generado y contenido final aprobado de cada resumen.
- La historia clínica es inmutable: una consulta cerrada no se edita, se enmienda, con registro de quién y cuándo.

## Convenciones
- Código de dominio, mensajes y nombres en español; commits con Conventional Commits
  (`feat(auth): ...`, `fix(citas): ...`, `chore: ...`).
- Ramas desde `main`; todo entra por PR con dos aprobaciones.

## Agentes del proyecto (`.claude/agents/`)
- `flyway-reviewer`: usar al crear o modificar cualquier archivo en `src/main/resources/db/`.
- `security-reviewer`: usar en cambios de autenticación, autorización, controladores, datos personales o resúmenes IA.
