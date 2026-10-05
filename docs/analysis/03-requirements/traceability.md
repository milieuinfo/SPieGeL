# Traceability

This matrix links every function of the predecessor, and every requirement of the [X-Cite](../01-context/background.md#x-cite) project, to a requirement, a priority and the SPieGeL component expected to implement it. Component names refer to [Module structure](../05-architecture/module-structure.md).

| Function | Predecessor | X-Cite | Requirement(s) | Priority | Phase | Component |
|---|---|---|---|---|---|---|
| Configurable data sources | implicit, one triple store per domain | basic (M) | FR-DS-01 | M | 1 | `spiegel-adapter-sparql` |
| URI template → queries | XML query catalogue | basic (M) | FR-URI-01, FR-RA-01, FR-RA-02 | M | 1 | `spiegel-core` |
| Content negotiation | yes | basic (M) | FR-CN-01, FR-CN-05 | M | 1 | `spiegel-adapter-web` |
| Simple HTML pages | XSLT | basic (M) | FR-HTML-01 | M | 1 | `spiegel-adapter-html` |
| Front page per domain | static, per domain | basic (M) | FR-HTML-02 | M | 1 | `spiegel-adapter-html` |
| Portal page linking all domains | no; static pages, nothing discovered | — | FR-HTML-08 | S | 2 | `spiegel-adapter-html`, `spiegel-adapter-config` |
| Discovered front-page blocks (catalogues, thesauri, classes) | no; maintained by hand | — | FR-HTML-09 | S | 1 | `spiegel-core`, `spiegel-adapter-html` |
| `id` → `doc` 303 | at the reverse proxy | — | FR-URI-02 | M | 2 | `spiegel-adapter-web` |
| `ns` vocabulary pages | yes | — | FR-URI-03, FR-RA-05 | M | 2 | `spiegel-core` |
| Skolem IRIs | **missing** | — | FR-URI-04 | M | 2 | `spiegel-core` |
| ELI paths | **missing** | — | FR-URI-05 | S | 2 | `spiegel-core` |
| Several domains in one deployment | yes (runtime-generated routes) | extended (C) | FR-MT-01, FR-MT-02 | M | 2 | `spiegel-core`, `spiegel-app` |
| Profile-dependent queries | intended, **not in effect** | extended (C) | FR-RA-03, FR-CN-03 | M | 2 | `spiegel-core` |
| Context-specific HTML | partly, per domain | extended (C) | FR-HTML-03 | S | 2 | `spiegel-adapter-html` |
| Authentication and access levels | yes, five levels | extended (C) | FR-AC-01 to 03, NFR-SEC-02 | M | 2 | `spiegel-adapter-security`, `spiegel-core` |
| Public `/sparql` | yes | — | FR-SPARQL-01 to 04 | M | 2 | `spiegel-adapter-web`, `spiegel-adapter-sparql` |
| Properties, values and types as links to their IRIs | yes (XSLT `set-label`, `set-content`) | — | FR-HTML-10 | M | 1 | `spiegel-adapter-html` |
| Labels for terms of external vocabularies | only if the vocabulary is in the domain's store | — | FR-HTML-11 | S | 1 | `spiegel-adapter-html`, `spiegel-adapter-config` |
| Multilingual HTML (data labels and interface texts) | no; fixed label order, Dutch interface texts in the XSLT | — | FR-HTML-12 | S | 2 | `spiegel-adapter-html`, `spiegel-adapter-web` |
| Machine-readable HTML (subject IRI, alternate links, embedded JSON-LD) | partly (`ld-subject` convention) | — | FR-HTML-04 | S | 1 | `spiegel-adapter-html` |
| Standard SPARQL result formats | partly (SPARQL JSON for the old components) | — | FR-SPARQL-02 | M | 2 | `spiegel-adapter-web` |
| Browsing relations, trees, collections | in the browser | — | FR-HTML-05 | M | 2 | to be decided |
| Data-dependent presentation blocks | XSLT rules by type and predicate | — | FR-HTML-06 | S | 2 | `spiegel-adapter-html` |
| Map for resources with a geometry | one point (`ld-map`) | — | FR-HTML-07 | S | 2 | `spiegel-adapter-html` |
| Departmental design system | no (`omgeving-ld`, Vue 2) | — | NFR-UI-01, NFR-UI-04 | M | 1 | `spiegel-adapter-html` |
| Accessibility and CSP | not assessed | — | NFR-UI-02, NFR-UI-03 | M | 1 | `spiegel-adapter-html`, `spiegel-adapter-web` |
| Keyword search | yes (`bif:contains`) | — | FR-SRCH-01 | S | 2 | `spiegel-adapter-sparql` |
| Relational sources (R2RML) | no | extended (C) | FR-DS-02 | C | later | `spiegel-adapter-r2rml` |
| Full-text search across sources | no | won't (W) | FR-SRCH-02 | W | — | — |
| Elasticsearch source | no | won't (W) | FR-DS-03 | W | — | — |
| Assisted (LLM) query building | no | won't (W) | — | W | — | — |
| Reconciliation | present, not routed | — | — | — | — | retired unless needed |
| Linked Data Fragments | present, not routed | — | — | — | — | retired unless needed |
| Data dump | placeholder | — | — | — | — | retired unless needed |
| Caching | two layers, per script | — | NFR-OPS-03, NFR-SEC-03 | S | 2 | `spiegel-app` |
| Concurrency limit per store | yes (5 concurrent, queue of 500) | — | NFR-OPS-02 | M | 1 | `spiegel-adapter-sparql` |
| Rate limiting per client (`429`) | no | — | NFR-OPS-08 | S | 2 | `spiegel-adapter-web` |
| Open CORS | yes | — | NFR-OPEN-01 | M | 1 | `spiegel-adapter-web` |

## Reading the "X-Cite" column

The X-Cite project grades its needs as *basic* (must have) and *extended* (could have). Several items it grades as *could have* (several domains, profiles, authentication) are **must have** for replacing the predecessor. The *Priority* column therefore takes the stricter of the two.
