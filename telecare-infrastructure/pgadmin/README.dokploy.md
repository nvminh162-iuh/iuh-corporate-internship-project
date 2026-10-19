# pgAdmin server provisioning on Dokploy

`docker-dokploy.yml` supplies `/pgadmin4/servers.json` through an inline Compose config.
The definition has the same fields as local `pgadmin/servers.json`, but takes its
values from the infrastructure `.env.dokploy`. No host bind mount or custom image
is needed. Inline `configs.content` requires Docker Compose 2.23.1 or later.

| Variable | Production value |
| --- | --- |
| `PGADMIN_REPLACE_SERVERS_ON_STARTUP` | `True` |
| `PGADMIN_SERVER_NAME` | `telecare-postgres` |
| `PGADMIN_SERVER_GROUP` | `Servers` |
| `PGADMIN_SERVER_HOST` | `telecare-staging-postgres` |
| `PGADMIN_SERVER_PORT` | `5432` |
| `PGADMIN_SERVER_SSL_MODE` | `prefer` |

The existing `POSTGRES_DB` and `POSTGRES_USER` supply MaintenanceDB and Username.
All variables are required; none have interpolation fallbacks. Keep server names,
groups, hosts and usernames JSON-safe (no unescaped quotes or backslashes), and
use an integer TCP port.

## Apply to the existing server

1. In Dokploy > telecare-infrastructure > General > Raw, replace the Compose with
   the updated `docker-dokploy.yml` and save. Keep the existing domain settings.
2. Add the six variables above to that Compose service's Environment and save.
   Keep existing credentials unchanged. Never upload the private `.env.dokploy`.
3. Deploy. There is no need to rebuild the Keycloak image or use Fresh Volumes.
4. Refresh pgAdmin and expand Servers > telecare-postgres. Enter the existing
   `POSTGRES_PASSWORD` when connecting; passwords are not stored in servers.json.

`True` makes the JSON authoritative: on startup pgAdmin replaces the owning
user's connection registrations (including registrations manually added later).
It does **not** delete PostgreSQL databases, schemas or data. If manual
registrations must be retained, use `False`; pgAdmin then imports JSON only when
creating a fresh configuration database, not into an existing volume.

The Compose remains a Raw deployment: Git pushes and image webhooks do not copy
changes to this YAML or its new environment variables into the Dokploy UI.

References:

- [pgAdmin container settings](https://www.pgadmin.org/docs/pgadmin4/latest/container_deployment.html)
- [pgAdmin server import/export](https://www.pgadmin.org/docs/pgadmin4/latest/import_export_servers.html)
- [Compose configs content](https://docs.docker.com/reference/compose-file/configs/#content)
