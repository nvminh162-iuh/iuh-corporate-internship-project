"""Block known credential fields containing literal secrets before image publish.

This is a targeted guard, not a general-purpose secret scanner. Never print values.
"""

import json
from pathlib import Path
import re
import sys


SECRET_FIELDS = {
    "password", "secret", "clientsecret", "privatekey", "secretdata",
    "credentialdata", "accesstoken", "refreshtoken",
}


def safe_value(value):
    if value is None or value == "":
        return True
    if isinstance(value, str):
        return bool(re.fullmatch(r"\*+|\$\{[A-Z][A-Z0-9_]*\}", value))
    if isinstance(value, list):
        return all(safe_value(item) for item in value)
    return False


def find_literal_secrets(node, path="$"):
    findings = []
    if isinstance(node, dict):
        for key, value in node.items():
            child_path = f"{path}.{key}"
            normalized = re.sub(r"[-_]", "", key).lower()
            if normalized in SECRET_FIELDS and not safe_value(value):
                findings.append(child_path)
            findings.extend(find_literal_secrets(value, child_path))
    elif isinstance(node, list):
        for index, value in enumerate(node):
            findings.extend(find_literal_secrets(value, f"{path}[{index}]"))
    return findings


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1] / "keycloak/realm-export.json"
    try:
        realm = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, ValueError):
        print("Realm validation failed: unreadable or invalid JSON.", file=sys.stderr)
        return 1
    findings = find_literal_secrets(realm)
    if findings:
        print("Realm contains literal credentials in these fields:", file=sys.stderr)
        for field in findings:
            print(f"- {field}", file=sys.stderr)
        print("Use runtime environment placeholders or remove credentials before publishing.", file=sys.stderr)
        return 1
    print("Realm credential guard passed; masked values are not usable credentials.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
