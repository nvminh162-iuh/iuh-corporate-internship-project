"""Validate Raw Compose provisioning using synthetic credentials only.

RUN_PGADMIN_CONTAINER_TESTS=1 also tests fresh and existing isolated pgAdmin data.
No production containers, networks or volumes are used.
"""

import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import unittest
import uuid


INFRA = Path(__file__).resolve().parents[1]
NEW_VARIABLES = (
    "PGADMIN_REPLACE_SERVERS_ON_STARTUP",
    "PGADMIN_SERVER_NAME",
    "PGADMIN_SERVER_GROUP",
    "PGADMIN_SERVER_HOST",
    "PGADMIN_SERVER_PORT",
    "PGADMIN_SERVER_SSL_MODE",
)


def sample_environment():
    environment = os.environ.copy()
    for line in (INFRA / ".env.dokploy.example").read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            environment[key] = value
    environment.update({
        "POSTGRES_PASSWORD": "synthetic-postgres-password",
        "PGADMIN_DEFAULT_EMAIL": "pgadmin-test@telecare.vn",
        "PGADMIN_DEFAULT_PASSWORD": "synthetic-pgadmin-password",
        "KEYCLOAK_ADMIN_PASSWORD": "synthetic-keycloak-password",
    })
    return environment


def render_compose(overrides=None, check=True):
    environment = sample_environment()
    environment.update(overrides or {})
    result = subprocess.run(
        ["docker", "compose", "--env-file", ".env.dokploy.example",
         "-f", "docker-dokploy.yml", "config", "--format", "json"],
        cwd=INFRA, env=environment, text=True, capture_output=True, timeout=20,
    )
    if check and result.returncode:
        raise AssertionError(result.stderr)
    return json.loads(result.stdout) if check else result


@unittest.skipUnless(shutil.which("docker"), "Docker Compose CLI is required")
class PgAdminDokployConfigurationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.compose = render_compose()

    def test_definition_matches_local_except_server_dns(self):
        definition = json.loads(self.compose["configs"]["pgadmin-servers"]["content"])
        local = json.loads((INFRA / "pgadmin" / "servers.json").read_text(encoding="utf-8"))
        local["Servers"]["1"]["Host"] = "telecare-staging-postgres"
        self.assertEqual(local, definition)

    def test_config_is_mounted_without_a_host_file(self):
        pgadmin = self.compose["services"]["pgadmin"]
        self.assertEqual("/pgadmin4/servers.json", pgadmin["environment"]["PGADMIN_SERVER_JSON_FILE"])
        self.assertEqual("pgadmin-servers", pgadmin["configs"][0]["source"])
        self.assertEqual("/pgadmin4/servers.json", pgadmin["configs"][0]["target"])
        self.assertNotIn("file", self.compose["configs"]["pgadmin-servers"])
        self.assertTrue(all(mount["type"] == "volume" for mount in pgadmin["volumes"]))

    def test_existing_volume_import_is_explicit_and_can_be_disabled(self):
        self.assertEqual("True", self.compose["services"]["pgadmin"]["environment"]["PGADMIN_REPLACE_SERVERS_ON_STARTUP"])
        disabled = render_compose({"PGADMIN_REPLACE_SERVERS_ON_STARTUP": "False"})
        self.assertEqual("False", disabled["services"]["pgadmin"]["environment"]["PGADMIN_REPLACE_SERVERS_ON_STARTUP"])

    def test_definition_uses_environment_including_existing_postgres_variables(self):
        overrides = {
            "PGADMIN_SERVER_NAME": "Test PostgreSQL",
            "PGADMIN_SERVER_GROUP": "Test Group",
            "PGADMIN_SERVER_HOST": "test-postgres",
            "PGADMIN_SERVER_PORT": "55432",
            "PGADMIN_SERVER_SSL_MODE": "require",
            "POSTGRES_DB": "test_database",
            "POSTGRES_USER": "test_user",
        }
        definition = json.loads(render_compose(overrides)["configs"]["pgadmin-servers"]["content"])
        self.assertEqual({
            "Name": "Test PostgreSQL", "Group": "Test Group", "Host": "test-postgres",
            "Port": 55432, "SSLMode": "require", "MaintenanceDB": "test_database",
            "Username": "test_user",
        }, definition["Servers"]["1"])

    def test_missing_or_empty_new_variables_fail_instead_of_falling_back(self):
        for key in NEW_VARIABLES:
            with self.subTest(variable=key):
                result = render_compose({key: ""}, check=False)
                self.assertNotEqual(0, result.returncode)
                self.assertIn(key + " must be set and non-empty", result.stderr)

    def test_credentials_are_excluded_and_production_volume_names_are_unchanged(self):
        content = self.compose["configs"]["pgadmin-servers"]["content"]
        self.assertNotIn("Password", content)
        self.assertNotIn("synthetic-", content)
        self.assertEqual("telecare-staging-postgres-data", self.compose["volumes"]["postgres-data"]["name"])
        self.assertEqual("telecare-staging-pgadmin-data", self.compose["volumes"]["pgadmin-data"]["name"])
        self.assertEqual({"postgres", "postgres-init", "kafka", "keycloak", "redis",
                          "pgadmin", "redis-insight", "kafka-ui"}, set(self.compose["services"]))


@unittest.skipUnless(os.environ.get("RUN_PGADMIN_CONTAINER_TESTS") == "1",
                     "Set RUN_PGADMIN_CONTAINER_TESTS=1 for isolated container smoke test")
class PgAdminDokployContainerTest(unittest.TestCase):
    def test_imports_on_fresh_and_existing_pgadmin_volume(self):
        rendered = render_compose()
        project = "codex-pgadmin-test-" + uuid.uuid4().hex[:12]
        environment = rendered["services"]["pgadmin"]["environment"].copy()
        environment["PGADMIN_DISABLE_POSTFIX"] = "1"
        configuration = {
            "services": {"pgadmin": {
                "image": rendered["services"]["pgadmin"]["image"],
                "environment": environment,
                "network_mode": "none",
                "configs": rendered["services"]["pgadmin"]["configs"],
                "volumes": ["test-data:/var/lib/pgadmin"],
            }},
            "configs": rendered["configs"],
            "volumes": {"test-data": {}},
        }

        def compose(*arguments, check=True):
            result = subprocess.run(
                ["docker", "compose", "-p", project, "-f", "-", *arguments],
                input=json.dumps(configuration), cwd=INFRA, env=sample_environment(),
                text=True, capture_output=True, timeout=90,
            )
            if check and result.returncode:
                self.fail(result.stderr)
            return result

        def wait_for_server(expected_name):
            # SQLite is read only; no PostgreSQL server is started or contacted.
            probe = (
                "import json,sqlite3; "
                "db=sqlite3.connect('file:/var/lib/pgadmin/pgadmin4.db?mode=ro',uri=True); "
                "print(json.dumps(db.execute('SELECT name FROM server ORDER BY id').fetchall()))"
            )
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                result = compose("exec", "-T", "pgadmin", "/venv/bin/python3", "-c", probe, check=False)
                if result.returncode == 0 and json.loads(result.stdout) == [[expected_name]]:
                    return
                time.sleep(1)
            self.fail("pgAdmin server import timed out: " + compose("logs", "--tail", "40", check=False).stdout)

        try:
            compose("up", "-d")
            wait_for_server("telecare-postgres")
            updated = json.loads(configuration["configs"]["pgadmin-servers"]["content"])
            updated["Servers"]["1"]["Name"] = "telecare-postgres-updated"
            configuration["configs"]["pgadmin-servers"]["content"] = json.dumps(updated)
            compose("up", "-d", "--force-recreate")
            wait_for_server("telecare-postgres-updated")
        finally:
            # Only this newly created, uniquely scoped test project is removed.
            assert project.startswith("codex-pgadmin-test-")
            compose("down", "--volumes", "--remove-orphans", check=False)


if __name__ == "__main__":
    unittest.main()
