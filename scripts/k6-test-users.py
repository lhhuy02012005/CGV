#!/usr/bin/env python3
"""Create disposable local Keycloak accounts for authenticated seat contention."""
import argparse
import concurrent.futures
import json
import os
from pathlib import Path
import secrets
import urllib.error
import urllib.parse
import urllib.request
import uuid


BASE = os.environ.get("K6_KEYCLOAK_URL", "http://localhost:8180").rstrip("/")
REALM = os.environ.get("K6_REALM", "cgv-realm")


def call(path, data=None, token=None, method="POST", content_type="application/json"):
    headers = {"Content-Type": content_type}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.status, response.headers, response.read()


def oauth(realm, client, secret, **fields):
    values = {"client_id": client, **fields}
    if secret:
        values["client_secret"] = secret
    _, _, body = call(f"/realms/{realm}/protocol/openid-connect/token",
                      urllib.parse.urlencode(values).encode(),
                      content_type="application/x-www-form-urlencoded")
    return json.loads(body)["access_token"]


def admin_token():
    return oauth("master", "admin-cli", None, grant_type="password",
                 username=os.environ["K6_ADMIN_USER"], password=os.environ["K6_ADMIN_PASSWORD"])


def create_account(index, marker, admin, client_secret):
    email = f"qa-k6-{marker}-{index:04d}@example.invalid"
    password = secrets.token_urlsafe(22)
    body = json.dumps({"username": email, "email": email, "enabled": True,
                       "emailVerified": True, "credentials": [{"type": "password", "value": password,
                                                                   "temporary": False}]}).encode()
    _, headers, _ = call(f"/admin/realms/{REALM}/users", body, admin)
    user_id = headers["Location"].rsplit("/", 1)[-1]
    try:
        token = oauth(REALM, os.environ.get("K6_CLIENT_ID", "CGV_App"), client_secret,
                      grant_type="password", username=email, password=password)
        return user_id, token
    except Exception:
        return user_id, None


def private_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w") as output:
        json.dump(value, output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--count", type=int, default=50)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--cleanup", type=Path, help="Delete only IDs recorded in this manifest")
    args = parser.parse_args()
    admin = admin_token()
    if args.cleanup:
        manifest = json.loads(args.cleanup.read_text())
        if manifest["realm"] != REALM or not manifest["marker"].startswith("qa-k6-"):
            raise ValueError("Not a k6 fixture manifest")
        for user_id in manifest["ids"]:
            call(f"/admin/realms/{REALM}/users/{user_id}", token=admin, method="DELETE")
        print(f"Deleted {len(manifest['ids'])} test accounts")
        return
    if args.count < 2 or args.output is None:
        parser.error("--count >= 2 and --output are required")
    marker = "qa-k6-" + uuid.uuid4().hex[:10]
    manifest_path = args.output.with_suffix(".accounts.json")
    if args.output.exists() or manifest_path.exists():
        raise FileExistsError("Choose unused output path")
    client_secret = os.environ["K6_CLIENT_SECRET"]
    results = []
    failures = 0
    try:
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            futures = [pool.submit(create_account, index, marker[6:], admin, client_secret)
                       for index in range(args.count)]
            for future in concurrent.futures.as_completed(futures):
                try:
                    results.append(future.result())
                except Exception:
                    failures += 1
    finally:
        private_json(manifest_path, {"realm": REALM, "marker": marker,
                                     "ids": [user_id for user_id, _ in results]})
    if failures or any(token is None for _, token in results):
        raise RuntimeError(f"Provision failed for {failures + sum(token is None for _, token in results)} accounts; "
                           f"cleanup recorded IDs with --cleanup {manifest_path}")
    private_json(args.output, [token for _, token in results])
    print(f"Created {len(results)} distinct JWTs; valid for Keycloak token lifespan. "
          f"Run k6 immediately; then --cleanup {manifest_path}")


if __name__ == "__main__":
    main()
