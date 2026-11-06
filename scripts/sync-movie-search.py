#!/usr/bin/env python3
"""Backfill local Elasticsearch from Catalog PostgreSQL without changing bookings."""

import json
import subprocess
import urllib.request


ES_URL = "http://localhost:9200"
QUERY = """
SELECT json_build_object(
    'id', id::text, 'title', title, 'originalTitle', original_title,
    'synopsis', synopsis, 'director', director, 'ageRating', age_rating,
    'showingStatus', showing_status, 'releaseDate', release_date
)
FROM movies ORDER BY id
"""


def get_json(path):
    with urllib.request.urlopen(ES_URL + path, timeout=15) as response:
        return json.load(response)


def main():
    mapping = get_json("/movies/_mapping")["movies"]["mappings"]["properties"]
    if mapping["title"].get("analyzer") != "autocomplete_analyzer":
        raise RuntimeError("Start Catalog first: movies index needs its configured analyzers")

    result = subprocess.run(
        ["docker", "exec", "cgv-postgres", "psql", "-U", "postgres", "-d", "cgv", "-Atq", "-c", QUERY],
        check=True, capture_output=True, text=True,
    )
    movies = [json.loads(line) for line in result.stdout.splitlines()]
    assert movies, "No Catalog movies in PostgreSQL"
    payload = "".join(
        json.dumps({"index": {"_index": "movies", "_id": movie["id"]}}) + "\n"
        + json.dumps(movie, ensure_ascii=False) + "\n"
        for movie in movies
    ).encode("utf-8")
    request = urllib.request.Request(
        ES_URL + "/_bulk?refresh=wait_for", payload,
        headers={"Content-Type": "application/x-ndjson"}, method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        bulk = json.load(response)
    failures = [item for item in bulk["items"] if item["index"].get("error")]
    if failures:
        raise RuntimeError(f"Elasticsearch rejected {len(failures)} documents: {failures[:1]}")
    indexed = get_json("/movies/_count")["count"]
    assert indexed >= len(movies), (indexed, len(movies))
    print(f"PostgreSQL: {len(movies)} movies; Elasticsearch: {indexed} indexed; bulk errors: 0")


if __name__ == "__main__":
    main()
