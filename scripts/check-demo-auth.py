import base64
import json

from demo import jwt_subject

payload = base64.urlsafe_b64encode(json.dumps({"sub": "member-id"}).encode()).decode().rstrip("=")
assert jwt_subject("header." + payload + ".signature") == "member-id"
try:
    jwt_subject("invalid")
except ValueError:
    pass
else:
    raise AssertionError("Malformed token must be rejected")
