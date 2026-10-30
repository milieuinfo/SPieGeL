# Traceability

This matrix links every function of the predecessor, and every requirement of the [X-Cite](../01-context/background.md#x-cite) project, to a requirement, a priority and the SPieGeL component expected to implement it. Component names refer to [Module structure](../05-architecture/module-structure.md).

| Function | Predecessor | X-Cite | Requirement(s) | Priority | Phase | Component |
|---|---|---|---|---|---|---|
| Configurable data sources | implicit, one triple store per domain | basic (M) | FR-DS-01 | M | 1 | `spiegel-adapter-out-virtuoso` |
| URI template → queries | XML query catalogue | basic (M) | FR-URI-01, FR-RA-01, FR-RA-02 | M | 1 | `spiegel-application` |
| Content negotiation | yes | basic (M) | FR-CN-01, FR-CN-05 | M | 1 | `spiegel-adapter-in-rest` |
| Negotiation that caches and HTTP tools understand (`Vary`, `Content-Location`, quality values, `HEAD`) | not checked | — | FR-CN-06 to 09 | M | 1 | `spiegel-adapter-in-rest` |
| Errors that machines can read (`application/problem+json`) | not checked | — | FR-CN-10 | S | 1 | `spiegel-adapter-in-rest` |
| JSON-LD for web developers (frame, context per domain) | not checked | — | FR-CN-11 | S | 1 | `spiegel-adapter-in-rest`, tenant configuration, context generator |
| Links between a thing and its document in the RDF | only where the data has them | — | FR-URI-07 | S | 1 | `spiegel-application` |
| Moved, withdrawn and removed URIs (`301`, status, `410`) | no | — | FR-URI-08 to 11, NFR-OPEN-02 | M | 2 | `spiegel-application`, `spiegel-adapter-in-rest`, tenant configuration |
| Simple HTML pages | XSLT | basic (M) | FR-HTML-01 | M | 1 | `spiegel-adapter-in-rest` |
| Front page per domain | static, per domain | basic (M) | FR-HTML-02 | M | 1 | `spiegel-adapter-in-rest` |
| Portal page linking all domains | no; static pages, nothing discovered | — | FR-HTML-08 | S | 2 | `spiegel-adapter-in-rest`, `spiegel-adapter-out-config` |
| Discovered front-page blocks (catalogues, thesauri, classes) | no; maintained by hand | — | FR-HTML-09 | S | 1 | `spiegel-application`, `spiegel-adapter-in-rest` |
| Structure of a domain's data (declared shapes and cube structures, derived summary) | no | — | FR-HTML-15 | C | 2 | `spiegel-application`, `spiegel-adapter-in-rest`, tenant queries |
| Dataset description per domain (DCAT, VoID) | no | — | FR-META-01 | S | 1 | `spiegel-application`, `spiegel-adapter-in-rest`, `spiegel-adapter-out-config` |
| Licence findable in data, headers and pages | no | — | FR-META-02 | M | 1 | `spiegel-adapter-out-config`, `spiegel-adapter-in-rest` |
| SPARQL Service Description | no | — | FR-META-03 | S | 2 | `spiegel-adapter-in-rest` |
| Example queries as data (SHACL), tested against the store | example queries in the editor component | — | FR-SPARQL-04 | S | 2 | `spiegel-adapter-in-rest`, tenant configuration |
| `id` → `doc` 303 | at the reverse proxy | — | FR-URI-02 | M | 1 | `spiegel-adapter-in-rest` (moved from the proxy, [ADR 0022](../../adr/0022-edge-concerns.md)) |
| `ns` vocabulary pages | yes | — | FR-URI-03, FR-RA-05 | M | 2 | `spiegel-application` |
| Skolem IRIs | **missing** | — | FR-URI-04 | M | 2 | `spiegel-application` |
| ELI paths | **missing** | — | FR-URI-05 | S | 2 | `spiegel-application` |
| Several domains in one deployment | yes (runtime-generated routes) | extended (C) | FR-MT-01, FR-MT-02 | M | 2 | `spiegel-application`, `spiegel-bootstrap` |
| Profile-dependent queries | intended, **not in effect** | extended (C) | FR-RA-03, FR-CN-03 | M | 2 | `spiegel-application` |
| Context-specific HTML | partly, per domain | extended (C) | FR-HTML-03 | S | 2 | `spiegel-adapter-in-rest` |
| Authentication and access levels | yes, five levels | extended (C) | FR-AC-01 to 03, NFR-SEC-02 | M | 2 | `spiegel-adapter-in-rest`, `spiegel-application` |
| Public `/sparql` | yes | — | FR-SPARQL-01 to 04 | M | 2 | `spiegel-adapter-in-rest`, `spiegel-adapter-out-virtuoso` |
| Properties, values and types as links to their IRIs | yes (XSLT `set-label`, `set-content`) | — | FR-HTML-10 | M | 1 | `spiegel-adapter-in-rest` |
| Labels for terms of external vocabularies | only if the vocabulary is in the domain's store | — | FR-HTML-11 | S | 1 | `spiegel-adapter-in-rest`, `spiegel-adapter-out-config` (extracts in the configuration, [ADR 0021](../../adr/0021-labels-of-external-vocabularies.md)) |
| Multilingual HTML (data labels and interface texts) | no; fixed label order, Dutch interface texts in the XSLT | — | FR-HTML-12 | S | 2 | `spiegel-adapter-in-rest` |
| Machine-readable HTML (subject IRI, alternate links, embedded JSON-LD) | partly (`ld-subject` convention) | — | FR-HTML-04 | S | 1 | `spiegel-adapter-in-rest` |
| Standard SPARQL result formats | partly (SPARQL JSON for the old components) | — | FR-SPARQL-02 | M | 2 | `spiegel-adapter-in-rest` |
| Browsing relations, trees, collections | in the browser | — | FR-HTML-05 | M | 2 | to be decided |
| Data-dependent presentation blocks | XSLT rules by type and predicate | — | FR-HTML-06 | S | 2 | `spiegel-adapter-in-rest` |
| Map for resources with a geometry | one point (`ld-map`) | — | FR-HTML-07 | S | 2 | `spiegel-adapter-in-rest` |
| Departmental design system | no (`omgeving-ld`, Vue 2) | — | NFR-UI-01, NFR-UI-04 | M | 1 | `spiegel-adapter-in-rest` |
| Accessibility and CSP | not assessed | — | NFR-UI-02, NFR-UI-03 | M | 1 | `spiegel-adapter-in-rest` |
| Keyword search | yes (`bif:contains`) | — | FR-SRCH-01 | S | 2 | `spiegel-adapter-out-virtuoso` |
| Non-RDF sources through mappings (RML, R2RML) | no | extended (C) | FR-DS-02 | C | later | `spiegel-adapter-out-virtuoso` (Ontop endpoint); none if materialised ([ADR 0009](../../adr/0009-ontop-for-relational-sources.md)) |
| Full-text search across sources | no | won't (W) | FR-SRCH-02 | C | later | `spiegel-application`, search adapter |
| Elasticsearch source | no | won't (W) | FR-DS-03 | W | — | — |
| Descriptions bounded to their subject | no, queries decide | — | FR-RA-06 | M | 1 | `spiegel-application`, tenant queries |
| Paging of large properties, in HTML and RDF alike | no | — | FR-RA-07 | M | 1 | `spiegel-application`, `spiegel-adapter-in-rest`, tenant configuration |
| Links to other distributions (OGC API, WFS, map viewers) | no | — | FR-HTML-13 | S | 2 | `spiegel-application`, `spiegel-adapter-in-rest` |
| Sitemap, `robots.txt`, canonical and alternate links | not checked | — | FR-HTML-16 | S | 1 | `spiegel-adapter-in-rest`, tenant configuration |
| schema.org and Open Graph for search engines and link previews | not checked | — | FR-HTML-17 | S | 1 | `spiegel-adapter-in-rest` |
| Images of a resource shown on its page | no | — | FR-HTML-14 | S | 2 | `spiegel-adapter-in-rest`, `spiegel-adapter-out-config` |
| Discovery of event streams (LDES) | no | — | FR-ES-01 | S | 2 | `spiegel-adapter-in-rest`, `spiegel-application` ([ADR 0023](../../adr/0023-event-stream-discovery-and-versions.md)) |
| Assisted (LLM) query building | no | won't (W) | — | W | — | — |
| Reconciliation | present, not routed | — | — | — | — | retired unless needed |
| Linked Data Fragments | present, not routed | — | — | — | — | retired unless needed |
| Data dump | placeholder | — | — | — | — | covered by event streams (ADR 0006) |
| Caching | two layers, per script | — | NFR-OPS-03, NFR-SEC-03 | S | 1 | `spiegel-bootstrap` |
| Concurrency limit per store | yes (5 concurrent, queue of 500) | — | NFR-OPS-02 | M | 1 | `spiegel-adapter-out-virtuoso` |
| Rate limiting per client (`429`) | no | — | NFR-OPS-08 | S | 1 | `spiegel-adapter-in-rest`, `RateLimitPort` ([ADR 0022](../../adr/0022-edge-concerns.md)) |
| Personal data: store's responsibility for the data; minimised in logs, counters and metrics; caching per access level | not assessed | — | NFR-PRIV-01 to 03 | M | 1 | `spiegel-bootstrap`, `spiegel-adapter-in-rest`, loading chain |
| Response time, throughput and availability targets | not defined | — | NFR-PERF-01 to 05 | M | 1 | `spiegel-bootstrap`, all adapters |
| Ceiling on the size of a description | no | — | NFR-OPS-09 | M | 1 | `spiegel-application`, `spiegel-adapter-out-virtuoso` |
| Open CORS | yes | — | NFR-OPEN-01 | M | 1 | `spiegel-adapter-in-rest`, not at the proxy ([ADR 0022](../../adr/0022-edge-concerns.md)) |

## Reading the "X-Cite" column

The X-Cite project grades its needs as *basic* (must have) and *extended* (could have). Several items it grades as *could have* (several domains, profiles, authentication) are **must have** for replacing the predecessor. The *Priority* column therefore takes the stricter of the two.
