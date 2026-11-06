#!/usr/bin/env python3
"""Local seat-hold demo using the application's HTTP, PostgreSQL and Redis."""
import argparse
import base64
import binascii
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "target"
CURRENT = TARGET / "demo-current.json"
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8000").rstrip("/")
SERVICES = ["postgres", "redis", "kafka-1", "kafka-2", "kafka-3", "keycloak",
            "elasticsearch", "catalog-service", "booking-service", "kong"]


def command(args, *, capture=True, check=True, input=None):
    return subprocess.run(args, cwd=ROOT, input=input, text=True, check=check,
                          stdout=subprocess.PIPE if capture else None,
                          stderr=subprocess.PIPE if capture else None)


def compose(*args):
    return ["docker", "compose", "--env-file", str(ROOT / ".env.demo"),
            "-f", str(ROOT / "docker-compose-dev.yml"), *args]


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")


def pg(sql):
    return command(["docker", "exec", "-i", "cgv-postgres", "psql", "-X", "-U",
                    "postgres", "-d", "cgv", "-v", "ON_ERROR_STOP=1", "-At"],
                   input=sql).stdout.strip()


def redis(*args):
    return command(["docker", "exec", "cgv-redis", "redis-cli", "--raw", *args]).stdout.strip()

def jwt_subject(token):
    try:
        payload = token.split(".")[1]
        subject = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))["sub"]
        if not isinstance(subject, str) or not subject:
            raise ValueError("Missing JWT subject")
        return subject
    except (IndexError, KeyError, ValueError, TypeError, binascii.Error) as exc:
        raise ValueError("Test token has no readable JWT subject") from exc

def test_accounts():
    path = Path(os.environ.get("SMOKE_TOKENS_FILE", os.environ.get("TOKENS_FILE", ""))).expanduser()
    if not path.is_file():
        raise RuntimeError("Set SMOKE_TOKENS_FILE to a JSON array with two valid account JWTs.")
    tokens = json.loads(path.read_text())
    if not isinstance(tokens, list) or len(tokens) < 2 or jwt_subject(tokens[0]) == jwt_subject(tokens[1]):
        raise RuntimeError("Smoke/integrity need JWTs from two distinct accounts.")
    return tokens[:2]


def request(method, path, payload=None, owner=None):
    headers = {"Content-Type": "application/json"}
    if owner:
        headers["Authorization"] = "Bearer " + owner
    body = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(BASE_URL + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            return response.status, json.loads(response.read())
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read())


def init():
    path = ROOT / ".env.demo"
    if path.exists():
        print(".env.demo already exists; retained.")
        return
    realm = json.loads((ROOT / "keycloak/cgv-realm-export.json").read_text())
    client = next(c for c in realm["clients"] if c["clientId"] == "CGV_App")
    value = (ROOT / ".env.demo.example").read_text().replace(
        "REPLACED_FROM_LOCAL_REALM_BY_DEMO_INIT", client["secret"])
    path.write_text(value)
    path.chmod(0o600)
    print("Created .env.demo for local demo; existing .env was not changed.")


def doctor(build=False):
    required = ["docker", "python3"] + (["java", "mvn"] if build else [])
    for name in required:
        if not shutil.which(name):
            raise RuntimeError(f"Missing {name} in PATH; see README.md prerequisites.")
    print("Python:", platform.python_version())
    print("Compose:", command(["docker", "compose", "version", "--short"]).stdout.strip())
    print("Docker server:", command(["docker", "version", "--format", "{{.Server.Version}}"]
                                    ).stdout.strip())
    print("Docker CPU/RAM:", command(["docker", "info", "--format",
          "{{.NCPU}} CPUs / {{.MemTotal}} bytes"]).stdout.strip())
    if build:
        version = command(["java", "-version"])
        info = version.stderr or version.stdout
        match = re.search(r'version "(\d+)', info)
        if not match or int(match[1]) != 17:
            raise RuntimeError("This handover uses JDK 17. Set JAVA_HOME/PATH to JDK 17.")
        print(info.splitlines()[0])
        print(command(["mvn", "-v"]).stdout.splitlines()[0])
    if not (ROOT / ".env.demo").exists():
        print("Next: python3 scripts/demo.py init")


def wait_ready(seconds=300):
    deadline = time.monotonic() + seconds
    last = ""
    while time.monotonic() < deadline:
        try:
            if redis("PING") != "PONG":
                raise RuntimeError("Redis not ready")
            if not pg("SELECT to_regclass('public.showtimes'), to_regclass('public.seats'), "
                      "to_regclass('public.booking_seat_claims');").startswith("showtimes|seats|booking_seat_claims"):
                raise RuntimeError("Waiting for Catalog/Booking Flyway")
            status, body = request("GET", "/api/v1/bookings/seat-locks/booked/" + str(uuid.UUID(int=0)))
            if status != 200 or not isinstance(body.get("data"), list):
                raise RuntimeError(f"Gateway/Booking HTTP {status}")
            print("READY: Gateway -> Booking -> PostgreSQL; Redis PONG; migration tables present.")
            return
        except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as exc:
            last = type(exc).__name__ + ": " + str(exc)
            time.sleep(2)
    raise RuntimeError("Readiness timeout. Read logs of booking-service, catalog-service and kong. " + last)


def start():
    doctor(build=True)
    init()
    command(["mvn", "-B", "-DskipTests", "package"], capture=False)
    command(compose("up", "-d", "--build", *SERVICES), capture=False)
    wait_ready()


def session():
    if not CURRENT.exists():
        raise RuntimeError("Run python3 scripts/demo.py prepare first.")
    data = json.loads(CURRENT.read_text())
    data["directory"] = ROOT / data["directory"]
    return data


def prepare():
    wait_ready()
    run_id = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex[:8]
    directory = TARGET / "demo-runs" / run_id
    directory.mkdir(parents=True)
    showtime = pg("SELECT id FROM showtimes WHERE status='SCHEDULED' "
                  "AND start_time > NOW() + INTERVAL '5 minutes' ORDER BY start_time LIMIT 1;")
    created_fixture = False
    if not showtime:
        showtime = str(uuid.uuid4())
        result = pg(f"INSERT INTO showtimes(id,movie_id,room_id,show_date,start_time,end_time,"
                    "language,subtitle_language,format,viewing_mode,base_price,available_seats,status,created_at,updated_at) "
                    f"SELECT '{showtime}'::uuid,t.movie_id,t.room_id,CURRENT_DATE,NOW()+INTERVAL '1 hour',"
                    "NOW()+INTERVAL '1 hour'+m.duration_minutes*INTERVAL '1 minute',t.language,"
                    "t.subtitle_language,t.format,t.viewing_mode,t.base_price,r.total_seats,'SCHEDULED',NOW(),NOW() "
                    "FROM showtimes t JOIN movies m ON m.id=t.movie_id JOIN rooms r ON r.id=t.room_id "
                    "WHERE r.status='ACTIVE' ORDER BY t.start_time DESC LIMIT 1 RETURNING id;")
        if showtime not in result:
            raise RuntimeError("No Catalog seed available. Check Catalog Flyway V1/V2/V3.")
        created_fixture = True
    candidates = pg(f"SELECT s.id,s.row_char||s.seat_number FROM seats s JOIN showtimes t ON t.room_id=s.room_id "
                    f"WHERE t.id='{showtime}'::uuid AND s.is_active "
                    "AND NOT EXISTS(SELECT 1 FROM booking_seat_claims c WHERE c.showtime_id=t.id AND c.seat_id=s.id) "
                    "AND NOT EXISTS(SELECT 1 FROM booking_seats bs JOIN bookings b ON b.id=bs.booking_id "
                    "WHERE bs.showtime_id=t.id AND bs.seat_id=s.id "
                    "AND b.status IN ('CONFIRMED','PAYMENT_PENDING','SEAT_RESERVED','USED')) "
                    "ORDER BY s.row_char,s.seat_number;")
    for line in candidates.splitlines():
        seat, label = line.split("|", 1)
        key = f"showtime:lock:{showtime}:{seat}"
        if not redis("GET", key):
            data = dict(run_id=run_id, showtime_id=showtime, seat_id=seat, seat_label=label,
                        redis_key=key, base_url=BASE_URL, created_showtime_fixture=created_fixture,
                        directory=str(directory.relative_to(ROOT)),
                        prepared_at=dt.datetime.now(dt.timezone.utc).isoformat())
            write_json(CURRENT, data)
            write_json(directory / "session.json", data)
            print(json.dumps(data, ensure_ascii=False, indent=2))
            return
    raise RuntimeError("No free seat; no existing locks were removed.")


def smoke():
    data = session()
    payload = {"showtimeId": data["showtime_id"], "seatIds": [data["seat_id"]]}
    key = data["redis_key"]
    if redis("GET", key):
        raise RuntimeError("Seat is already held. Run prepare to choose a free seat.")
    events = []
    a, b = test_accounts()
    a_owner, b_owner = jwt_subject(a), jwt_subject(b)
    def step(name, method, owner, expected_status, expected_owner):
        status, body = request(method, "/api/v1/bookings/seat-locks", payload, owner)
        holder, ttl = redis("GET", key), int(redis("TTL", key))
        ok = status == expected_status and holder == expected_owner
        if method == "POST" and status == 200:
            dto = body.get("data", {})
            ok = (ok and dto.get("showtimeId") == data["showtime_id"]
                  and data["seat_id"] in dto.get("seatIds", []) and bool(dto.get("expiresAt")))
        if holder:
            ok = ok and 0 < ttl <= 240
        events.append(dict(step=name, http_status=status, response=body, redis_owner=holder,
                           redis_ttl_seconds=ttl, passed=ok))
        write_json(data["directory"] / "smoke.json", events)
        print(f"{name}: HTTP {status}; owner={holder or '(nil)'}; TTL={ttl}; passed={ok}")
        if not ok:
            raise RuntimeError("Demo step failed; smoke.json retains the response.")
    try:
        step("A holds", "POST", a, 200, a_owner)
        step("B conflicts", "POST", b, 409, a_owner)
        step("B cannot release A", "DELETE", b, 200, a_owner)
        step("A releases", "DELETE", a, 200, "")
        step("B now holds", "POST", b, 200, b_owner)
        step("B releases", "DELETE", b, 200, "")
    finally:
        holder = redis("GET", key)
        if holder in (a_owner, b_owner):
            request("DELETE", "/api/v1/bookings/seat-locks", payload, a if holder == a_owner else b)
    print("DEMO PASS. No booking/payment was created. Seat ready for k6.")

def integrity():
    data = session()
    directory = data["directory"]
    path = directory / "integrity.json"
    if path.exists():
        raise RuntimeError("Integrity output already exists; run prepare for a new RUN_ID.")
    showtime, seat, key = data["showtime_id"], data["seat_id"], data["redis_key"]
    owner, contender = test_accounts()
    payload = {"showtimeId": showtime, "seatIds": [seat]}
    result = {"run_id": data["run_id"], "scope": "Kong/Booking/PostgreSQL/Redis with JWT", "steps": []}
    booking_id = None
    contender_booking_id = None

    def record(name, passed, **details):
        result["steps"].append(dict(name=name, passed=passed, **details))
        write_json(path, result)
        if not passed:
            raise RuntimeError(f"Integrity step {name} failed; see {path}")

    if redis("GET", key) or seat in request("GET", "/api/v1/bookings/seat-locks/booked/" + showtime)[1].get("data", []):
        raise RuntimeError("Seat not free; run prepare again.")
    try:
        status, body = request("POST", "/api/v1/bookings/seat-locks", payload, owner)
        record("A holds", status == 200 and redis("GET", key) == jwt_subject(owner), http_status=status)
        status, body = request("POST", "/api/v1/bookings", payload, owner)
        booking_id = body.get("data", {}).get("bookingId") if status == 201 else None
        record("A creates pending booking", status == 201 and bool(booking_id), http_status=status,
               booking_id=booking_id, response=body)
        claim = pg(f"SELECT count(*) FROM booking_seat_claims WHERE booking_id='{booking_id}'::uuid "
                   f"AND showtime_id='{showtime}'::uuid AND seat_id='{seat}'::uuid;")
        state = pg(f"SELECT status FROM bookings WHERE id='{booking_id}'::uuid;")
        record("PostgreSQL claim", claim == "1" and state == "PAYMENT_PENDING", claim_count=claim, booking_status=state)
        redis("EXPIRE", key, "1")
        for _ in range(30):
            if redis("TTL", key) == "-2":
                break
            time.sleep(0.1)
        record("Redis lock expires", redis("GET", key) == "", ttl_seconds=int(redis("TTL", key)))
        status, body = request("POST", "/api/v1/bookings/seat-locks", payload, contender)
        record("B cannot hold sold seat", status == 409 and redis("GET", key) == "", http_status=status, response=body)
        status, body = request("POST", "/api/v1/bookings", payload, contender)
        if status == 201:
            contender_booking_id = body.get("data", {}).get("bookingId")
        record("B cannot book sold seat", status == 409, http_status=status, response=body)
    finally:
        if contender_booking_id:
            request("PUT", f"/api/v1/bookings/{contender_booking_id}/cancel", owner=contender)
        if redis("GET", key) == jwt_subject(contender):
            request("DELETE", "/api/v1/bookings/seat-locks", payload, contender)
        if booking_id:
            status, body = request("PUT", f"/api/v1/bookings/{booking_id}/cancel", owner=owner)
            record("A cancels pending booking", status == 200, http_status=status, response=body)
            claim = pg(f"SELECT count(*) FROM booking_seat_claims WHERE booking_id='{booking_id}'::uuid;")
            state = pg(f"SELECT status FROM bookings WHERE id='{booking_id}'::uuid;")
            record("Claim released", claim == "0" and state == "CANCELLED", claim_count=claim, booking_status=state)
        elif redis("GET", key) == jwt_subject(owner):
            request("DELETE", "/api/v1/bookings/seat-locks", payload, owner)
    status, body = request("POST", "/api/v1/bookings/seat-locks", payload, contender)
    try:
        record("B holds after cancellation", status == 200 and redis("GET", key) == jwt_subject(contender),
               http_status=status, response=body)
    finally:
        if redis("GET", key) == jwt_subject(contender):
            request("DELETE", "/api/v1/bookings/seat-locks", payload, contender)
    result["passed"] = all(step["passed"] for step in result["steps"])
    write_json(path, result)
    print("Integrity evidence:", path)


def environment(directory):
    names = ["cgv-booking-service", "cgv-catalog-service", "cgv-postgres", "cgv-redis", "cgv-kong"]
    containers = []
    for name in names:
        obj = json.loads(command(["docker", "inspect", name]).stdout)[0]
        containers.append(dict(name=name, image=obj["Config"]["Image"], image_id=obj["Image"],
                               started_at=obj["State"]["StartedAt"],
                               memory_limit_bytes=obj["HostConfig"]["Memory"],
                               nano_cpus=obj["HostConfig"]["NanoCpus"]))
    info = dict(time_utc=dt.datetime.now(dt.timezone.utc).isoformat(), host_os=platform.platform(),
                host_arch=platform.machine(), host_logical_cpus=os.cpu_count(),
                docker=command(["docker", "version", "--format", "{{.Server.Version}}"]
                               ).stdout.strip(),
                docker_resources=command(["docker", "info", "--format", "{{.NCPU}} CPUs / {{.MemTotal}} bytes"]
                                         ).stdout.strip(), containers=containers,
                scope="One BookingService, one Redis; HTTP hold contention through Kong; no payment.")
    if sys.platform == "darwin":
        info["host_memory_bytes"] = int(command(["sysctl", "-n", "hw.memsize"]).stdout.strip())
    else:
        info["host_memory_bytes"] = os.sysconf("SC_PHYS_PAGES") * os.sysconf("SC_PAGE_SIZE")
    info["host_java"] = command(["java", "-version"]).stderr.splitlines()[0]
    info["resource_snapshot"] = [json.loads(line) for line in command(
        ["docker", "stats", "--no-stream", "--format", "{{json .}}", *names]).stdout.splitlines()]
    for item, name in zip(containers, names):
        obj = json.loads(command(["docker", "inspect", name]).stdout)[0]
        item["java_opts"] = next((v.split("=", 1)[1] for v in obj["Config"]["Env"]
                                  if v.startswith("JAVA_OPTS=")), None)
    write_json(directory / "environment.json", info)
    paths = [ROOT / "pom.xml", ROOT / "docker-compose-dev.yml", ROOT / "k6/seat-lock-1000.js",
             ROOT / "scripts/demo.py"]
    for module in ["bookingservice", "catalogservice", "commondto"]:
        paths.extend((ROOT / module / "src/main").rglob("*"))
        paths.append(ROOT / module / "pom.xml")
    hashes = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in sorted(set(paths)) if p.is_file() and "prod" not in p.name}
    write_json(directory / "source-manifest.json", hashes)
    jar = next((ROOT / "bookingservice/target").glob("bookingservice-*.jar"))
    expected = hashlib.sha256(jar.read_bytes()).hexdigest()
    actual = command(["docker", "exec", "cgv-booking-service", "sha256sum", "/app/app.jar"]).stdout.split()[0]
    write_json(directory / "runtime-jar.json", dict(local_sha256=expected, container_sha256=actual,
                                                    matches=expected == actual))
    if expected != actual:
        raise RuntimeError("Container JAR differs from local build. Run demo.py start before measuring.")


def load():
    count = int(os.environ.get("SEAT_REQUESTS", "1000"))
    if count < 2:
        raise RuntimeError("SEAT_REQUESTS must be >= 2.")
    token_file = Path(os.environ.get("TOKENS_FILE", "")).expanduser()
    if not token_file.is_file():
        raise RuntimeError(f"Set TOKENS_FILE to JSON array of {count} distinct users' valid JWTs.")
    token_file = token_file.resolve()
    tokens = json.loads(token_file.read_text())
    if not isinstance(tokens, list) or len(tokens) < count or len({jwt_subject(token) for token in tokens[:count]}) != count:
        raise RuntimeError(f"TOKENS_FILE needs {count} JWTs from distinct accounts.")
    data = session()
    directory = data["directory"]
    if (directory / "k6-console.log").exists():
        raise RuntimeError("This run already has output. Run prepare for a new run directory.")
    if redis("GET", data["redis_key"]):
        raise RuntimeError("Selected seat has a lock; run prepare or wait for its TTL.")
    if data["seat_id"] in request("GET", "/api/v1/bookings/seat-locks/booked/" + data["showtime_id"])[1].get("data", []):
        raise RuntimeError("Selected seat is already allocated in Booking.")
    environment(directory)
    write_json(directory / "redis-before.json", dict(owner=redis("GET", data["redis_key"]),
               ttl_seconds=int(redis("TTL", data["redis_key"]))))
    params = dict(SHOWTIME_ID=data["showtime_id"], SEAT_ID=data["seat_id"], RUN_ID=data["run_id"], SEAT_REQUESTS=str(count))
    if shutil.which("k6"):
        env = dict(os.environ, **params, BASE_URL=BASE_URL, TOKENS_FILE=str(token_file), K6_RESULT_PATH=str(directory / "result.json"))
        args = ["k6", "run", "--out", f"json={directory / 'k6-raw.json'}", "--summary-export",
                str(directory / "k6-summary.json"), "k6/seat-lock-1000.js"]
        version = command(["k6", "version"]).stdout.strip()
    else:
        env = dict(os.environ, **params)
        args = compose("run", "--rm", "--no-deps", "--user", f"{os.getuid()}:{os.getgid()}",
                       "-v", f"{directory}:/results", "-v", f"{token_file}:/tokens.json:ro", "-e", "TOKENS_FILE=/tokens.json", "-e", "BASE_URL=http://kong:8000",
                       "-e", "K6_RESULT_PATH=/results/result.json")
        for k, v in params.items():
            args.extend(["-e", f"{k}={v}"])
        args.extend(["k6", "run", "--out", "json=/results/k6-raw.json", "--summary-export",
                     "/results/k6-summary.json", "/k6/seat-lock-1000.js"])
        version = command(compose("run", "--rm", "--no-deps", "k6", "version")).stdout.strip()
    write_json(directory / "load-generator.json", dict(version=version, arguments=args,
                                                      started_at=dt.datetime.now(dt.timezone.utc).isoformat()))
    with (directory / "k6-console.log").open("w") as log:
        proc = subprocess.Popen(args, cwd=ROOT, env=env, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        for line in proc.stdout:
            print(line, end="", flush=True)
            log.write(line)
        code = proc.wait()
    owner = redis("GET", data["redis_key"])
    ttl = int(redis("TTL", data["redis_key"]))
    write_json(directory / "redis-after.json", dict(owner=owner, ttl_seconds=ttl))
    console = (directory / "k6-console.log").read_text()
    winners = sorted(set(re.findall(r"WINNER_VU=(\d+)", console)))
    result_path = directory / "result.json"
    result = json.loads(result_path.read_text()) if result_path.exists() else {}
    passed = (code == 0 and result.get("requests") == count and result.get("success") == 1
              and result.get("conflicts") == count - 1 and result.get("unexpected") == 0
              and result.get("thresholds_passed") is True and len(winners) == 1
              and owner == jwt_subject(tokens[int(winners[0]) - 1]) and 0 < ttl <= 240)
    verification = dict(passed=passed, k6_exit_code=code, winner_ids=winners,
                        redis_owner=owner, ttl_seconds=ttl,
                        completed_at=dt.datetime.now(dt.timezone.utc).isoformat())
    write_json(directory / "verification.json", verification)
    print(json.dumps(verification, indent=2))
    print("Output:", directory)
    if not passed:
        raise RuntimeError("Run did not satisfy all criteria; unmodified output has been retained.")


def status():
    command(compose("ps", *SERVICES), capture=False)
    if CURRENT.exists():
        data = session()
        print("RUN_ID:", data["run_id"])
        print("REDIS_KEY:", data["redis_key"])
        print("OWNER:", redis("GET", data["redis_key"]) or "(nil)")
        print("TTL:", redis("TTL", data["redis_key"]))
        print("OUTPUT:", data["directory"])


def results():
    data = session()
    print("Saved run:", data["run_id"])
    print("Output directory:", data["directory"])
    for filename in ["result.json", "redis-before.json", "redis-after.json", "verification.json"]:
        path = data["directory"] / filename
        print("\n" + filename)
        print(path.read_text() if path.exists() else "Not produced yet.")
    print("Redis values above were captured immediately after that run; live TTL may now have expired.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["init", "doctor", "start", "wait", "prepare", "smoke", "integrity", "load", "status", "results", "stop"])
    args = parser.parse_args()
    if args.action == "doctor":
        doctor(build=True)
    elif args.action == "wait":
        wait_ready()
    elif args.action == "stop":
        command(compose("stop", *SERVICES), capture=False)
    else:
        globals()[args.action]()


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, subprocess.CalledProcessError) as exc:
        print("ERROR:", str(exc), file=sys.stderr)
        sys.exit(1)
