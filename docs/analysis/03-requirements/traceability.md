# Traceability

This matrix links every function of the predecessor, and every need of the model catalogue, to a requirement, a priority and the SPieGeL component expected to implement it. Component names refer to [Module structure](../05-architecture/module-structure.md).

| Function | Predecessor | Model catalogue | Requirement(s) | Priority | Phase | Component |
|---|---|---|---|---|---|---|
| Configurable data sources | implicit, one triple store per domain | basic (M) | FR-DS-01 | M | 1 | `spiegel-adapter-sparql` |
| URI template → queries | XML query catalogue | basic (M) | FR-URI-01, FR-RA-01, FR-RA-02 | M | 1 | `spiegel-core` |
| Content negotiation | yes | basic (M) | FR-CN-01, FR-CN-05 | M | 1 | `spiegel-adapter-web` |
| Simple HTML pages | XSLT | basic (M) | FR-HTML-01 | M | 1 | `spiegel-adapter-html` |
| Front page | static, per domain | basic (M) | FR-HTML-02 | M | 1 | `spiegel-adapter-html` |
| `id` → `doc` 303 | at the reverse proxy | — | FR-URI-02 | M | 2 | `spiegel-adapter-web` |
| `ns` vocabulary pages | yes | — | FR-URI-03, FR-RA-05 | M | 2 | `spiegel-core` |
| Skolem IRIs | **missing** | — | FR-URI-04 | M | 2 | `spiegel-core` |
| ELI paths | **missing** | — | FR-URI-05 | S | 2 | `spiegel-core` |
| Several domains in one deployment | yes (runtime-generated routes) | extended (C) | FR-MT-01, FR-MT-02 | M | 2 | `spiegel-core`, `spiegel-app` |
| Profile-dependent queries | intended, **not in effect** | extended (C) | FR-RA-03, FR-CN-03 | M | 2 | `spiegel-core` |
| Context-specific HTML | partly, per domain | extended (C) | FR-HTML-03 | S | 2 | `spiegel-adapter-html` |
| Authentication and access levels | yes, five levels | extended (C) | FR-AC-01 to 03, NFR-SEC-02 | M | 2 | `spiegel-adapter-security`, `spiegel-core` |
| Public `/sparql` | yes | — | FR-SPARQL-01 to 04 | M | 2 | `spiegel-adapter-web`, `spiegel-adapter-sparql` |
| Client component compatibility | yes | — | FR-HTML-04, FR-SPARQL-02 | S | 2 | `spiegel-adapter-html` |
| Browsing relations, trees, collections | in the browser | — | FR-HTML-05 | M | 2 | to be decided |
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
| Open CORS | yes | — | NFR-OPEN-01 | M | 1 | `spiegel-adapter-web` |

## Reading the "model catalogue" column

The model catalogue initiative grades its needs as *basic* (must have) and *extended* (could have). Several items it grades as *could have* (several domains, profiles, authentication) are **must have** for replacing the predecessor. The *Priority* column therefore takes the stricter of the two.
