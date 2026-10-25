# Non-functional requirements

## Security

SPieGeL publishes open data, but may also serve data at higher access levels. These requirements are written to rule out whole classes of fault, not to patch individual ones.

| ID | Requirement | Priority |
|---|---|---|
| NFR-SEC-01 | **Read-only by construction.** SPieGeL never sends update operations. Read-only access is guaranteed at the data source, through credentials without write rights. Any filtering in the application is defence in depth only. | M |
| NFR-SEC-02 | **Least privilege per access level.** Each access level uses credentials that can only read what that level may see. A fault in the application's authorisation must not widen what the store returns. | M |
| NFR-SEC-03 | **Authorise before cache.** Authorisation is evaluated before any cache lookup. Every cache key includes everything that determines the response, at least: tenant, resource, access level, profile and, where relevant, media type. | M |
| NFR-SEC-04 | **No string-built queries from user input.** Values from requests (IRIs, search terms, filters) enter queries only as bound parameters, or as properly escaped and validated terms (for example through Jena's `ParameterizedSparqlString`). | M |
| NFR-SEC-05 | **Encode output, do not whitelist input.** HTML output is context-encoded by the template engine. Input character whitelists are not a substitute. | M |
| NFR-SEC-06 | **No server-side fetching of caller-supplied URLs.** SPieGeL only contacts data sources defined in its configuration. | M |
| NFR-SEC-07 | **No existence disclosure.** A resource the caller may not see is indistinguishable from one that does not exist. | S |
| NFR-SEC-08 | **Diagnostic and administration endpoints** are absent from production builds, or not exposed publicly. | M |

## Open data

| ID | Requirement | Priority |
|---|---|---|
| NFR-OPEN-01 | Public resources and the public SPARQL endpoint send `Access-Control-Allow-Origin: *`. This is a deliberate open-data policy, not a weakness. Credentialed requests follow normal CORS rules. | M |
| NFR-OPEN-02 | URIs that have been published stay stable. Changes in SPieGeL must not break existing URIs. **A published URI never disappears; only its answer changes:** a description (`200`), possibly with a withdrawn status, a permanent redirect to its new URI (`301`), or a tombstone (`410`). See FR-URI-08 to 11. | M |

## Operations

| ID | Requirement | Priority |
|---|---|---|
| NFR-OPS-01 | **Stateless.** No server-side session. Any node can serve any request, so no sticky sessions are needed. | M |
| NFR-OPS-02 | **Backpressure.** The number of concurrent queries per data source is bounded, with a bounded queue and timeouts. | M |
| NFR-OPS-03 | **Caching** is centralised, configurable per tenant, and respects NFR-SEC-03. Responses stay valid until the tenant's data changes: the loading chain signals a new **data version** per tenant, which also serves as the entity tag (`ETag`), so that clients and caches can revalidate cheaply (`304 Not Modified`). A fixed lifetime is only a fallback, because domains change at very different rates: some every night, others a few times a year (see [Performance and availability](performance.md#the-predecessor-today)). | S |
| NFR-OPS-04 | **Health and readiness** endpoints that really check the configured data sources. | M |
| NFR-OPS-05 | **Structured logging** with a request identifier and timing, and **metrics** per tenant and data source. What is logged follows NFR-PRIV-02. | S |
| NFR-OPS-06 | **Configuration** is external to the build artefact. Per-tenant configuration (URI templates, queries, templates) is version-controlled. | M |
| NFR-OPS-07 | **Deployment** supports a rolling or blue/green switch-over without mixing incompatible versions behind one host name. | S |
| NFR-OPS-08 | **Rate limiting per client.** A client that exceeds its limit gets `429 Too Many Requests` with `Retry-After`, before any query is run. Limits are configurable per tenant and per kind of request, with a stricter limit for `/sparql`. An authenticated client is identified by its identity. For public access, which is unauthenticated, the client is identified by its address as reported by a trusted reverse proxy. Limits hold across all nodes, without sticky sessions (NFR-OPS-01). `429` (this client asks too much) is distinct from `503` (the service or a data source is overloaded, NFR-OPS-02). Whether SPieGeL or the reverse proxy enforces the limit is still to be decided ([open question 13](../06-open-questions.md#architecture-and-operations)). | S |
| NFR-OPS-09 | **Bounded descriptions.** Every description has a maximum number of triples, configurable per tenant, in addition to the query timeout. SPieGeL stops while it reads the result stream, so that one large resource never sits in memory as a whole. A description that reaches the ceiling, because its large properties are not configured for paging (FR-RA-07), is **never cut off silently**: the response says that it is incomplete, in the RDF and in a header, and the event is counted as a metric per tenant and resource type (NFR-OPS-05). Labels for linked IRIs (FR-HTML-10) are fetched in batches, with a cap on their number. | M |

## Performance and availability

Targets are measured on the server, from SPieGeL's own metrics (NFR-OPS-05), under normal load and for public requests. The reasoning and a reference measurement on UniProt are in [Performance and availability](performance.md).

| ID | Requirement | Priority |
|---|---|---|
| NFR-PERF-01 | **Resource descriptions are fast.** A description in RDF or HTML is served from the cache with p95 ≤ 50 ms, and without the cache with a median ≤ 150 ms and p95 ≤ 500 ms. A single description query on the store takes p95 ≤ 100 ms. | M |
| NFR-PERF-02 | **Front pages are always cached.** A domain's front page and the portal (FR-HTML-02, FR-HTML-08) are served from the cache with p95 ≤ 100 ms. Their blocks are refreshed in the background, never while a visitor waits. | S |
| NFR-PERF-03 | **SPARQL is bounded.** A bounded lookup on one resource through `/sparql` takes p95 ≤ 300 ms. Every query stops at a timeout configured per tenant, and results are streamed to the client. | S |
| NFR-PERF-04 | **Throughput with headroom.** The targets of NFR-PERF-01 to 03 still hold at twice the measured peak load per tenant. Beyond that, SPieGeL refuses work (`429`, `503` with `Retry-After`; NFR-OPS-02, NFR-OPS-08) rather than slowing down for everyone. The peak load is still to be measured ([open question 36](../06-open-questions.md#architecture-and-operations)). | S |
| NFR-PERF-05 | **Availability.** Public requests are available 99.5 % of each month (about 3.6 hours of downtime), excluding announced maintenance, as measured by an external probe. | S |

## Privacy

Personal data reaches SPieGeL in the published data and in the requests themselves. See [ADR 0018](../../adr/0018-personal-data-in-requests-and-caches.md).

| ID | Requirement | Priority |
|---|---|---|
| NFR-PRIV-01 | **Personal data in the data is the store's responsibility.** Data owners, the loading chain and the store keep personal data in separate graphs that only the accounts of the levels allowed to see it can read. SPieGeL serves it only through such an account (NFR-SEC-02) and does not decide what is personal. | M |
| NFR-PRIV-02 | **Personal data from requests is minimised.** Logs hold truncated IP addresses (IPv4 /24, IPv6 /48), an authenticated caller as a keyed hash, and URLs without the values of parameters other than `_format`, `_profile`, `_page`, `_property` and `_inverse`. Search terms and SPARQL texts are not logged; a query is logged as a keyed hash with its duration, status and number of results. The key changes at least as often as logs are deleted. Full IP addresses appear only in rate-limit counters, for minutes. Metrics are never labelled by IRI, IP address or caller. The retention of logs is configurable. | M |
| NFR-PRIV-03 | **Caching follows the access level.** A response at a non-public level is `private`, and cached by SPieGeL only under its access level and data version. A public response may be kept by shared caches for minutes only, configurable per tenant, and is then revalidated with its `ETag`. Configuration, example queries and test fixtures contain no real personal data. | M |

## User interface

The HTML must comply with the department's design system. See [Design system: Flux](design-system.md) and [ADR 0005](../../adr/0005-flux-design-system-for-html.md).

| ID | Requirement | Priority |
|---|---|---|
| NFR-UI-01 | **Flux.** HTML pages use the components, styles and page-layout pattern of the Flux design system (v2). They use the *next* variants where both exist and no deprecated components. | M |
| NFR-UI-02 | **Accessibility.** Pages meet WCAG 2.1 AA, as the law requires, and follow Flux's route to *silver [plus]* (WCAG 2.2 AA). End-to-end tests include automated accessibility checks. | M |
| NFR-UI-03 | **Strict Content Security Policy.** Pages send a CSP that is as strict as Flux allows (Flux 2.4.0 or later). There are no inline scripts and no inline style blocks. | S |
| NFR-UI-04 | **Content without JavaScript.** The title, properties, links and alternate formats of a resource are rendered on the server and readable without JavaScript. Scripts only add interaction: trees, lazily loaded relations, maps and the SPARQL editor. | S |

## Quality

| ID | Requirement | Priority |
|---|---|---|
| NFR-Q-01 | The domain core has no dependency on Spring, Jena's HTTP client or any web framework. See [Hexagonal design](../05-architecture/hexagonal-design.md). | M |
| NFR-Q-02 | Every requirement in [Functional requirements](functional.md) has at least one automated test that refers to its identifier. | S |
| NFR-Q-03 | All documentation and code are in British English. | M |
