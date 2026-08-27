-- Chạy một lần khi volume Postgres còn trống (docker-entrypoint-initdb.d).
-- Keycloak uses the "keycloak" database (POSTGRES_DB).
-- The monolith uses one shared core database.

CREATE DATABASE telecare;
GRANT ALL PRIVILEGES ON DATABASE telecare TO telecare;
