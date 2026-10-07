#!/usr/bin/env python3
"""Measure the response times of a Linked Data service from the client's side.

Runs a set of typical requests (resource descriptions and SPARQL queries)
several times and reports, per request, the time to first byte and the total
time as minimum, median, 95th percentile and maximum.

Only the Python standard library is used. Example:

    python3 tools/latency/measure_latency.py --runs 20
    python3 tools/latency/measure_latency.py --runs 20 --no-cache-busting
    python3 tools/latency/measure_latency.py --cases my-cases.json --csv out.csv

Cache busting (the default) appends a unique, harmless VALUES clause to every
SPARQL query. The results do not change, but neither an HTTP cache nor the
store's own query cache can recognise the query, so the measurement shows the
work of the store. (A unique comment is not enough: a store that parses the
query before looking it up in its cache ignores comments.) Resource
descriptions cannot be cache-busted this way, so their figures may include a
cache.

Every request opens a new connection, as a one-off client would. The time
for TCP and TLS set-up is therefore part of every figure; measure it once with
for example `curl -w '%{time_appconnect}'` to know the network floor.

A cases file is a JSON list of objects with the keys "name", "url", "accept"
and, for SPARQL, "query" (sent as a GET parameter to "url").
"""

import argparse
import csv
import json
import statistics
import sys
import time
import urllib.parse
import urllib.request
import uuid

# Default cases: UniProt, a public SPARQL service over a very large dataset.
UNIPROT_SPARQL = "https://sparql.uniprot.org/sparql"
DEFAULT_CASES = [
    {
        "name": "resource description (Turtle)",
        "url": "https://purl.uniprot.org/uniprot/P05067",
        "accept": "text/turtle",
    },
    {
        "name": "SPARQL: ASK on one resource",
        "url": UNIPROT_SPARQL,
        "accept": "application/sparql-results+json",
        "query": "PREFIX up: <http://purl.uniprot.org/core/>\n"
                 "ASK { <http://purl.uniprot.org/uniprot/P05067> a up:Protein }",
    },
    {
        "name": "SPARQL: properties of one resource",
        "url": UNIPROT_SPARQL,
        "accept": "application/sparql-results+json",
        "query": "SELECT ?p ?o WHERE { <http://purl.uniprot.org/uniprot/P05067> ?p ?o } LIMIT 500",
    },
    {
        "name": "SPARQL: label lookup for 50 resources",
        "url": UNIPROT_SPARQL,
        "accept": "application/sparql-results+json",
        "query": "PREFIX up: <http://purl.uniprot.org/core/>\n"
                 "SELECT ?protein ?name WHERE {\n"
                 "  ?protein a up:Protein ; up:reviewed true ; up:mnemonic ?name .\n"
                 "} LIMIT 50",
    },
    {
        "name": "SPARQL: count over a large set",
        "url": UNIPROT_SPARQL,
        "accept": "application/sparql-results+json",
        "query": "PREFIX up: <http://purl.uniprot.org/core/>\n"
                 "PREFIX taxon: <http://purl.uniprot.org/taxonomy/>\n"
                 "SELECT (COUNT(?protein) AS ?n) WHERE {\n"
                 "  ?protein a up:Protein ; up:reviewed true ; up:organism taxon:9606 .\n"
                 "}",
    },
]


def build_request(case, bust_cache):
    url = case["url"]
    if "query" in case:
        query = case["query"]
        if bust_cache:
            query = f'{query}\nVALUES ?cache_buster {{ "{uuid.uuid4()}" }}'
        url = f"{url}?{urllib.parse.urlencode({'query': query})}"
    return urllib.request.Request(url, headers={
        "Accept": case["accept"],
        "User-Agent": "spiegel-latency-measurement/1.0",
    })


def measure_once(case, bust_cache, timeout):
    request = build_request(case, bust_cache)
    start = time.perf_counter()
    with urllib.request.urlopen(request, timeout=timeout) as response:
        first = response.read(1)
        ttfb = time.perf_counter() - start
        size = len(first) + len(response.read())
        status = response.status
    total = time.perf_counter() - start
    return status, ttfb, total, size


def percentile(values, fraction):
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, round(fraction * (len(ordered) - 1))))
    return ordered[index]


def summarise(values):
    return {
        "min": min(values),
        "median": statistics.median(values),
        "p95": percentile(values, 0.95),
        "max": max(values),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--cases", help="JSON file with the cases (default: UniProt)")
    parser.add_argument("--runs", type=int, default=10, help="measured runs per case (default 10)")
    parser.add_argument("--warm-up", type=int, default=1, help="unmeasured runs per case first (default 1)")
    parser.add_argument("--timeout", type=float, default=60, help="timeout per request in seconds")
    parser.add_argument("--no-cache-busting", action="store_true",
                        help="send identical SPARQL queries, so HTTP caches may answer")
    parser.add_argument("--csv", help="also write every measurement to this CSV file")
    args = parser.parse_args()

    cases = DEFAULT_CASES
    if args.cases:
        with open(args.cases, encoding="utf-8") as handle:
            cases = json.load(handle)

    rows = []
    print(f"{'case':42} {'n':>3} {'size':>9}   {'TTFB min/median/p95/max (ms)':32} {'total median/p95 (ms)':>22}")
    for case in cases:
        for _ in range(args.warm_up):
            try:
                measure_once(case, not args.no_cache_busting, args.timeout)
            except Exception:  # the measured runs report errors
                pass
        ttfbs, totals, size, errors = [], [], 0, 0
        for run in range(args.runs):
            try:
                status, ttfb, total, size = measure_once(case, not args.no_cache_busting, args.timeout)
            except Exception as error:
                errors += 1
                rows.append([case["name"], run, "error", "", "", str(error)])
                continue
            ttfbs.append(ttfb)
            totals.append(total)
            rows.append([case["name"], run, status, f"{ttfb:.4f}", f"{total:.4f}", size])
        if not ttfbs:
            print(f"{case['name'][:42]:42} all {args.runs} runs failed")
            continue
        t, a = summarise(ttfbs), summarise(totals)
        ms = lambda s: f"{s * 1000:.0f}"
        print(f"{case['name'][:42]:42} {len(ttfbs):>3} {size:>9}   "
              f"{ms(t['min']):>6} {ms(t['median']):>6} {ms(t['p95']):>6} {ms(t['max']):>6}       "
              f"{ms(a['median']):>8} {ms(a['p95']):>8}"
              + (f"   ({errors} errors)" if errors else ""))

    if args.csv:
        with open(args.csv, "w", newline="", encoding="utf-8") as handle:
            writer = csv.writer(handle)
            writer.writerow(["case", "run", "status", "ttfb_s", "total_s", "size_or_error"])
            writer.writerows(rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
