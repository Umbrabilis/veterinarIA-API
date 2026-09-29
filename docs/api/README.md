# Contrato OpenAPI (exportado)

Copia del OpenAPI de cada microservicio, para que el frontend trabaje sin tener el backend corriendo.
Base URL: el gateway, `http://localhost:8080` en local.

Generar tipos TypeScript en `veterinarIA-web`:

```bash
npx openapi-typescript ../veterinarIA-API/docs/api/citas-service.json -o src/api/tipos/citas.ts
```

Volver a exportar después de cambiar un endpoint o DTO (con `docker compose up -d` corriendo):

```bash
for s in auth pacientes citas historias; do curl -s localhost:8080/v3/api-docs/$s-service > docs/api/$s-service.json; done
```
