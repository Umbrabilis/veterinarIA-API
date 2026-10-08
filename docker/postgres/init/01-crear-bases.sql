-- Una base de datos por microservicio (cada servicio es dueño de su esquema y lo migra con Flyway).
-- En producción pueden vivir en servidores distintos; en local comparten el contenedor de PostgreSQL.
-- Este script solo corre la primera vez que se crea el volumen de datos.
CREATE DATABASE auth_db;
CREATE DATABASE pacientes_db;
CREATE DATABASE citas_db;
CREATE DATABASE historias_db;
