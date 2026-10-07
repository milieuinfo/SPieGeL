# Performance and availability

The requirements did not yet say how fast SPieGeL must be or how often it must be available. Without targets, neither the choice of store ([open question 29](../06-open-questions.md#architecture-and-operations)) nor the cache settings can be judged. This page gives a reference measurement on a very large public dataset, and the targets that follow from it (NFR-PERF-01 to 05 in [Non-functional requirements](non-functional.md#performance-and-availability)).

## Reference: UniProt on QLever

[UniProt](https://www.uniprot.org/) publishes one of the largest public RDF datasets: 244 billion triples in release 2026_03, according to its endpoint's home page. Its official SPARQL endpoint, `https://sparql.uniprot.org/sparql`, runs on [QLever](https://github.com/ad-freiburg/qlever), the store that is a candidate to replace Virtuoso (question 29). It is therefore a good indication of what a store of that kind can do under real, public load. What else SPieGeL can learn from the service is in [Reference: the UniProt SPARQL service](../04-landscape/uniprot-reference.md).

We measured it on 7 October 2026 from a workstation in Flanders, with the [script below](#measurement-script): 20 runs per request after one warm-up run, each on a new connection. SPARQL queries were cache-busted, so that neither an HTTP cache nor QLever's own query cache could answer them.

| Request | Size | Time to first byte, median | p95 | Total, median |
|---|---|---|---|---|
| Resource description, Turtle (`purl.uniprot.org/uniprot/P05067`) | 870 kB | 191 ms | 197 ms | 525 ms |
| SPARQL `ASK` on one resource | 41 B | 90 ms | 133 ms | 91 ms |
| SPARQL: all properties of one resource (`LIMIT 500`) | 137 kB | 98 ms | 107 ms | 166 ms |
| SPARQL: labels of 50 resources | 12 kB | 262 ms | 467 ms | 276 ms |
| SPARQL: count of the reviewed human proteins | 260 B | 90 ms | 113 ms | 90 ms |

How to read these figures:

- **The network floor is about 65 ms.** Setting up TCP (about 32 ms) and TLS (about 33 ms) to Switzerland is part of every figure. What remains for the server is **about 25 to 40 ms** for a lookup on one resource, and **about 200 ms** for the label query, uncached.
- **Caches matter, but the store is fast without them.** Without cache busting, the label query took 84 ms instead of 262 ms (median). The other queries hardly changed.
- **The resource description is not served from the store.** `purl.uniprot.org` redirects (`301`) to UniProt's REST service, which serves the 870 kB Turtle file. Its time to first byte (191 ms) includes the redirect.
- **UniProt ties caching to its data release.** Responses carry `ETag: W/"2026_03"` (the release), `Cache-Control: public` and an `Expires` one day later. A cache can keep a response until the data changes. This fits SPieGeL, whose domains are loaded in batches: an entity tag per data version makes validation (`304 Not Modified`) cheap (NFR-OPS-03).

What this means for SPieGeL: a store like QLever answers bounded lookups, which is what SPieGeL's description queries are (FR-RA-06, FR-RA-07), in a few tens of milliseconds, even on a dataset far larger than any of the department's domains. SPieGeL's own share, running several queries, fetching labels and rendering HTML, must stay in proportion to that.

## Targets

The targets are measured **on the server**, from SPieGeL's own metrics (NFR-OPS-05), so that they do not depend on the client's network. They apply under normal load, for public requests, with the store on the same network as SPieGeL.

| Kind of request | Target | Requirement |
|---|---|---|
| Resource description (RDF or HTML) from the cache | p95 ≤ 50 ms | NFR-PERF-01 |
| Resource description (RDF or HTML), not cached | median ≤ 150 ms, p95 ≤ 500 ms | NFR-PERF-01 |
| One description query on the store | p95 ≤ 100 ms | NFR-PERF-01 |
| Front page and portal | p95 ≤ 100 ms, always from the cache | NFR-PERF-02 |
| `/sparql`: bounded lookup on one resource | p95 ≤ 300 ms | NFR-PERF-03 |
| `/sparql`: any query | stopped at the configured timeout | NFR-PERF-03 |
| Throughput | the targets above hold at twice the measured peak load | NFR-PERF-04 |
| Availability | 99.5 % per month for public requests | NFR-PERF-05 |

The throughput target needs the predecessor's actual load and response times as a baseline ([open question 36](../06-open-questions.md#architecture-and-operations)).

## Measurement script

The script measures the response times of any Linked Data service from the client's side: the minimum, median, 95th percentile and maximum of the time to first byte and of the total time. It uses only the Python standard library. Without arguments it runs the UniProt requests above. With `--cases` it reads other requests from a JSON file, so the same queries can be run against the predecessor and against candidate stores (question 29).

```bash
python3 tools/latency/measure_latency.py --runs 20
python3 tools/latency/measure_latency.py --runs 20 --cases my-cases.json --csv results.csv
```

A client-side measurement includes the network. Use it to compare services under the same conditions, not to check the server-side targets above.

```python
--8<-- "tools/latency/measure_latency.py"
```
