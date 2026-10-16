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

## The predecessor today

Aggregated figures from the predecessor's own logs, to set the targets against. They are a first baseline, not a measurement of peak load: the application log covers only one afternoon, because it is reset on every deployment.

**Resource descriptions (`/doc`)**, from the duration the predecessor logs per request, in two samples: 48 minutes on the afternoon of 7 October 2026 (13:55 to 14:42 UTC) and 85 minutes on the morning of 8 October 2026 (07:31 to 08:56 UTC), all domains together:

| Domain | 7 Oct: requests | median | p95 | 8 Oct: requests | median | p95 |
|---|---|---|---|---|---|---|
| `algemeen` | 1,205 | 4.9 s | 9.4 s | 466 | 4.2 s | 18.3 s |
| `cbb` | 1,090 | 0.6 s | 22.8 s | 803 | 4.8 s | 10.2 s |
| `dba` | 691 | 1.3 s | 28.6 s | 45 | 3.5 s | 8.4 s |
| `imjv` | 530 | 0.9 s | 29.0 s | 9 | 7.6 s | 9.2 s |
| `dsi` | 419 | 16.6 s | 33.1 s | 641 | 4.7 s | 22.4 s |
| All domains | 3,977 | 4.0 s | 27 s | 1,984 | 4.6 s | 17.0 s |

- **Load:** on the afternoon of 7 October about one `/doc` request per second (median 62, maximum 214 per minute); on the morning of 8 October a median of 14 per minute, with a burst of up to 274 per minute in the first five minutes. The load comes in bursts, each mostly from one domain.
- **Slow requests:** 16 % and 10 % took longer than 10 seconds; the longest took 52 seconds.
- **Every request is slow, not only under load.** In quiet minutes (20 requests or fewer) the median is still 4.9 s; bursts mainly lengthen the tail (p95 from 9.6 s to 22 s). Rendering HTML with XSLT takes a median of 2 to 4 s on its own. The stores themselves cancel few queries: the store of `algemeen` aborted or cancelled 125 queries in two years. Most of the time is therefore spent **above the store**, in the predecessor's own work per request: several queries per page and the XSLT rendering. The queue (5 concurrent queries per store, with a queue of 500) adds to the tail during bursts. A faster store alone would not fix this (question 29).
- **Little repetition.** In the 85 minutes of 8 October, only 22 % of the requests asked for a resource that had already been asked for (1,531 distinct resources in 1,964 requests), and asking for the same resource in another format was rare. The requests are spread almost evenly over Turtle, HTML, RDF/XML, plain text, JSON-LD and XML, which suggests crawlers, but crawlers that each fetch a different resource. A response cache therefore helps less than the slow data suggests: **the uncached response time is what matters** (NFR-PERF-01). Caching the assembled description per resource, and serialising it per format, still saves work when the same resource is asked for in several formats.

**Loading patterns differ per domain** (from the store logs):

- `zendantennes` is loaded **incrementally every night**: at 22:00, about 50 update requests. Its data changes every day.
- `algemeen` is **mostly static**. One graph is cleared every night at 22:00. The rest changes when the store is set up again, which happened on 14 days since January 2025, during office hours, for example when code lists are released.

A fixed cache lifetime therefore fits neither domain well. Responses can stay cached until the tenant's data changes, if the loading chain signals a new data version (NFR-OPS-03).

**Availability of the stores:** the store of `zendantennes` restarted 65 times in seven years, each time online again after about 1.5 minutes; that of `algemeen` 17 times in two years, after about 10 seconds. The logs do not show outages of the proxy, the network or the predecessor itself, so they are not enough to measure NFR-PERF-05.

**External traffic on the umbrella domain** (`data.omgeving.vlaanderen.be`, the domain `algemeen`), from the reverse proxy's access log. The export held two complete days; the health check, which runs once a minute, shows that no minutes are missing:

| | Friday 2 October 2026 | Sunday 4 October 2026 |
|---|---|---|
| Requests, without health checks | 3,732 | 1,202 |
| of which `/doc` and `/id` | 1,846 (1,177 distinct resources) | 406 |
| Busiest minute / busiest hour | 139 / 542 | 101 / 188 |
| Client addresses | 1,071 | 311 |
| Answered `4xx` | 31 % | 49 % |
| Answered `5xx` | 27 | 24 |
| Health checks that failed | 43 of 1,441 (**97.0 %** available) | 35 of 1,440 (**97.6 %** available) |

- **External load is low:** a few thousand requests a day, on average fewer than three per minute, with short bursts of up to about two per second.
- **Many requests are for paths that do not exist:** a third to half of the requests get a `4xx`, mostly from automated clients.
- **Availability falls short of NFR-PERF-05:** the failed health checks cluster in periods of one to one and a half hours (Friday morning, Sunday evening), during which the proxy answers `502`. At 97 % per day, a month would have about 20 hours of downtime, against the 3.6 hours that 99.5 % allows.
- **The predecessor handles far more `algemeen` descriptions than the proxy passes on:** its own log counted 1,205 `/doc` requests for `algemeen` in 48 minutes on 7 October and 466 in 85 minutes on 8 October (about 36,000 and 7,900 a day at those rates), while the proxy passes on about 1,850 `/doc` and `/id` requests on a whole weekday. The most repeated requests are for the code lists of `algemeen` (`conceptscheme`). If these figures are typical, most of the predecessor's work for `algemeen` does not come from outside through this host name, but from inside: probably pages of other domains that fetch descriptions of shared resources, such as concepts from code lists, to show their labels. The samples are from different days, so this still has to be confirmed. If it holds, the throughput SPieGeL needs is set less by external visitors than by how labels of other domains' resources are resolved, which SPieGeL does from configured label sources and a cache rather than a request per label (FR-HTML-11, NFR-OPS-09).

**Still needed** ([open question 36](../06-open-questions.md#architecture-and-operations)): request counts over several weeks, including busy days, from the reverse proxy's access log in the central logging. That log has no request duration today. Recording it (for example Apache's `%D`) would give response times from the outside from now on.

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
| Throughput | the targets above hold at twice the measured peak load; the 48-minute sample above gives at least 8 `/doc` requests per second over all domains, including the predecessor's internal requests; external traffic on the umbrella domain is far lower | NFR-PERF-04 |
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
