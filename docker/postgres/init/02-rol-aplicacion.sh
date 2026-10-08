#!/bin/sh
# Rol con el que corren los microservicios. Flyway migra con el rol dueño (POSTGRES_USER); la aplicación usa
# veterinaria_app, que no es dueño de las tablas: no puede desactivar ni borrar los triggers que protegen la
# historia clínica y la aprobación de resúmenes, y no tiene DELETE ni TRUNCATE.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -c "CREATE ROLE veterinaria_app LOGIN PASSWORD 'veterinaria_app'"

for db in auth_db pacientes_db citas_db historias_db; do
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" <<SQL
GRANT CONNECT ON DATABASE $db TO veterinaria_app;
GRANT USAGE ON SCHEMA public TO veterinaria_app;
ALTER DEFAULT PRIVILEGES FOR ROLE $POSTGRES_USER IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO veterinaria_app;
SQL
done
