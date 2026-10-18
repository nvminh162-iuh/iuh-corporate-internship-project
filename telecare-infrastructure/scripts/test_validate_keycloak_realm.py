import unittest

from validate_keycloak_realm import find_literal_secrets


class RealmCredentialGuardTest(unittest.TestCase):
    def test_accepts_masked_values(self):
        self.assertEqual([], find_literal_secrets({"smtpServer": {"password": "********"}}))

    def test_accepts_required_runtime_placeholders(self):
        self.assertEqual([], find_literal_secrets({"clientSecret": "${GOOGLE_CLIENT_SECRET}"}))

    def test_accepts_empty_credentials_and_public_configuration(self):
        self.assertEqual([], find_literal_secrets({"secret": "", "password": None, "clientId": "public-id", "accessTokenLifespan": 300}))

    def test_rejects_literal_smtp_password_without_echoing_value(self):
        self.assertEqual(["$.smtpServer.password"], find_literal_secrets({"smtpServer": {"password": "synthetic-test-secret"}}))

    def test_rejects_nested_identity_provider_secret(self):
        realm = {"identityProviders": [{"config": {"clientSecret": "synthetic-test-secret"}}]}
        self.assertEqual(["$.identityProviders[0].config.clientSecret"], find_literal_secrets(realm))

    def test_rejects_private_key_arrays(self):
        self.assertEqual(["$.config.private_key"], find_literal_secrets({"config": {"private_key": ["synthetic-private-key"]}}))

    def test_rejects_placeholders_with_secret_defaults(self):
        self.assertEqual(["$.password"], find_literal_secrets({"password": "${SMTP_PASSWORD:synthetic-secret}"}))


if __name__ == "__main__":
    unittest.main()
