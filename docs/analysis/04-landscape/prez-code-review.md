# Prez code review

[Prez](https://github.com/RDFLib/prez) (RDFLib, developed mainly by KurrawongAI) is the only project in the [comparison](comparison.md) built around **W3C Content Negotiation by Profile**, which is what the predecessor intended with its format-dependent queries. It is written in Python, so adopting it was never likely. We read its source code to find out what it does, how much of the predecessor's behaviour it covers, and which of its ideas SPieGeL should adopt.

**Version reviewed:** upstream commit `2130b38` (2 October 2026), version 4.7.5. About 17,700 lines of Python under `prez/`, plus 68 test modules. Built on FastAPI, RDFLib, pyoxigraph, httpx, aiocache and Kurrawong's `sparql-grammar` (a typed SPARQL syntax tree). BSD-3-Clause. Shipped as a container image and as an Azure Functions variant.

Prez delivers **data only**: RDF, GeoJSON and JSON. HTML comes from a separate Vue application, [Prez UI](https://github.com/RDFLib/prez-ui), which was not part of this review.

## How it works

### Configuration: environment variables plus RDF

Runtime settings are environment variables (`prez/config.py`, Pydantic Settings): the single `SPARQL_ENDPOINT` and its credentials, a timeout, label and description predicates, count limits, and so on. Everything that shapes the API is **RDF** (`prez/reference_data/`), loaded at start-up into an in-memory *system store* (pyoxigraph):

- **Profiles** (`profiles/*.ttl`) are `prof:Profile` resources described with the Content Negotiation by Profile vocabulary (`altr-ext:`). Each profile says which classes it applies to (`altr-ext:constrainsClass`), which media types it offers (`altr-ext:hasResourceFormat`) and its default media type. Node shapes name the default profile per class (`altr-ext:hasDefaultProfile`). **What a profile returns is a SHACL property shape**: a union of property paths (sequences, inverse paths, alternatives), with extensions such as `shext:bNodeDepth "2"` (follow blank nodes two levels deep) and `shext:allPredicateValues`.
- **Endpoints** (`endpoints/**/*.ttl`) are `ont:ListingEndpoint` or `ont:ObjectEndpoint` resources with an `ont:apiPath` (for example `/catalogs/{catalogId}/collections`). Each points to SHACL node shapes that describe the class hierarchy along the path, for example *a `skos:Concept` is in a `skos:ConceptScheme` that is part of a `dcat:Catalog`*. Custom endpoint definitions can be read from files or from a named graph in the store, and there is a form for writing them (`CONFIGURATION_MODE`).
- **Annotations** (`annotations/*.ttl`): labels and descriptions of about 35 common vocabularies (DCAT, SKOS, PROV, GeoSPARQL, QUDT, schema.org and others). They are bundled so that responses can be labelled without querying the data store.
- **Prefixes**, used to turn IRIs into CURIEs in URL paths.

### Request flow for an object

For `GET /catalogs/{catalogId}/collections/{collectionId}` or `GET /object?iri=…` (`routers/base_router.py`, `services/objects.py`):

1. **Resolve the identifiers.** Path segments are CURIEs (`ex:my-catalog`), expanded with the known prefixes.
2. **Negotiate profile and media type** (`services/connegp_service.py`). Requested profiles come from `?_profile=` or the `Accept-Profile` header, and media types from `?_mediatype=` or `Accept`, both with *q*-weights. One SPARQL `SELECT` on the system store ranks the available combinations: requested profile, then how closely the profile's class matches (exact or one `rdfs:subClassOf` step), then the class's default profile, requested media type and default media type.
3. **Generate the query.** The selected profile's SHACL shape becomes a `CONSTRUCT` (`services/query_generation/shacl.py`, `umbrella.py`). It is built as a syntax tree through `sparql-grammar` and serialised at the end. With `?_mediatype=application/sparql-query`, Prez returns the generated query instead of running it.
4. **Run it** on the data store: a remote SPARQL endpoint over httpx, or an embedded pyoxigraph store.
5. **Annotate** (only for the `anot+` media types, such as `text/anot+turtle`, the default). Prez adds `prez:label`, `prez:description` and similar triples for *every* IRI in the response, from the data and from the bundled vocabularies. It also adds `prez:link` triples with the API path of each referenced resource. Annotations are cached in memory per term.
6. **Respond** with the serialised result and `Link` headers: `rel="profile"` for the selected profile, and `rel="self"` and `rel="alternate"` links for each available profile and media type combination. The special `alt` profile lists every representation as data.

If `PREZ_UI_URL` is set, a browser request (`text/html` or `*/*`) is redirected to the matching Prez UI page.

### Other functions

| Function | Where |
|---|---|
| Listings with paging, ordering and bounded counts: above `LISTING_COUNT_LIMIT` (default 100) the count is reported as `>100` | `services/listings.py`, `query_generation/count.py` |
| Search: default `REGEX` or `LCASE` matching on configurable predicates, or Fuseki full-text search and Jena Lucene with escaping rules | `query_generation/search_*.py` |
| OGC CQL2 (JSON) filtering, including spatial functions | `routers/cql_router.py`, `query_generation/cql.py` |
| Facets on listings and search results | `query_generation/facet.py` |
| SKOS hierarchy browsing: top concepts and narrowers, with child counts | `query_generation/concept_hierarchy.py` |
| OGC API – Features (GeoJSON), mounted under each collection; tested with the OGC conformance tests | `routers/ogc_features_router.py` |
| SPARQL pass-through at `/sparql`, off by default (`ENABLE_SPARQL_ENDPOINT`) | `routers/sparql.py` |
| Identifier services: `foaf:homepage` redirect, IRI ↔ CURIE | `routers/identifier.py` |
| Management: `/health`, `/prefixes`, `/tbox-cache`, `/purge-tbox-cache` | `routers/management.py` |
| Correlation IDs (`X-Request-ID`), structured JSON logs with typed attributes, timing of downstream calls | `middleware.py`, `services/prez_logging.py` |

## Coverage of the predecessor's functions

| Predecessor function | Prez | Where in the code |
|---|---|---|
| `/doc/{concept}/{id}` as HTML and as RDF/XML, Turtle, N-Triples and JSON-LD | **RDF yes**, plus annotated variants and GeoJSON. **HTML no**: it redirects to the separate Prez UI | `connegp_service.py`, `renderers/renderer.py` |
| Own composed query per concept and media type | **yes, in a different form**: profiles per class, each with its own SHACL-defined content and its own media types. This is the closest match to what the predecessor intended | `reference_data/profiles`, `query_generation/shacl.py` |
| HTML view per concept | **outside Prez** (Prez UI) | — |
| `303` from `/id/` to `/doc/` | **no**. Resources live under CURIE paths (`/catalogs/ex:abc/…`) or at `/object?iri=`, not at their own IRI | `routers/base_router.py` |
| `/ns/{model}` | **no** dedicated support; vocabularies are shown as catalogues or concept schemes | — |
| Several domains in one service | **no**: one data store and one endpoint structure per instance (a documented limitation) | `config.py`, `docs/custom_endpoints.md` |
| `/sparql` as a filtered proxy with HTML results | **proxy yes, filter no**, and off by default. No HTML results | `routers/sparql.py`, `repositories/remote_sparql.py` |
| Access levels through OIDC | **no**. Only an optional fixed request header that must be present (`required_header`), meant for an API gateway | `middleware.py` |
| Keyword search | **yes**, including full-text search on Fuseki and Lucene | `query_generation/search_*.py` |
| Reconciliation, Linked Data Fragments, dumps | **no** | — |
| Incoming relations | **by profile**: an `sh:inversePath` in the profile's shape | `query_generation/shacl.py` |
| Blank nodes in outgoing relations | **yes**, to a configurable depth (`shext:bNodeDepth`) | `query_generation/shacl.py` |
| Front-end components that need a same-origin `/sparql` | **yes**, when the SPARQL endpoint is enabled | `routers/sparql.py` |
| Skolem IRIs and ELI paths | **no**: identifiers are CURIEs in a fixed path structure | — |
| Caching | **partly**: labels, CURIEs and class lookups are cached in process. Responses are not cached, and no `Cache-Control`, `ETag` or `Vary` headers are set | `cache.py`, `services/annotations.py` |

## Points to note before using it in production

As with the other reviews, these are listed so that SPieGeL avoids them, not as criticism of a project that is actively maintained.

1. **Not every query goes through the syntax tree.** Most queries are built with `sparql-grammar`, but a few are still assembled from text with values from the request. → SPieGeL: NFR-SEC-04, and use one query-building mechanism throughout.
2. **The SPARQL pass-through forwards the caller's headers.** When enabled, `/sparql` passes the query and almost all incoming request headers (only `Host` and `X-Request-ID` are removed) to the store. As the README says, whether updates are refused is up to the store. → SPieGeL: NFR-SEC-01.
3. **Permissive CORS.** `Access-Control-Allow-Origin: *` is combined with `Access-Control-Allow-Credentials: true`, once in a custom middleware and again through Starlette's `CORSMiddleware` (which then reflects the caller's origin). Prez has no logins, so the impact is small, but this must not be copied into a service that has them. → SPieGeL: NFR-OPEN-01.
4. **Management endpoints are public.** `GET /purge-tbox-cache` empties the label cache for anyone, and `/tbox-cache` returns its whole content. → SPieGeL: NFR-SEC-08.
5. **Unbounded, per-process label cache.** The annotation cache has no expiry and no size limit. It grows with every new IRI seen, is not invalidated when the data changes, and differs between replicas. → SPieGeL: NFR-OPS-03.
6. **No HTTP caching support.** Representations are negotiated on `Accept` and `Accept-Profile` without a `Vary` header, and there is no `ETag` or `Cache-Control`. An unsupported media type falls back to the default instead of `406`. → SPieGeL: NFR-SEC-03, FR-CN-05.
7. **Generated queries are hard to tune.** Profiles are concise, but the `CONSTRUCT` built from a SHACL shape (unions of property paths, blank-node depth, annotation and link look-ups) is not written by anyone. Prez makes it visible with `_mediatype=application/sparql-query`, which helps, but a slow page needs a profile change rather than a query fix. The README also notes that the endpoint structure is limited to two or three levels because "the SHACL parsing is not completely recursive".
8. **`/health` does not check the store.** It always answers `{"status": "ok"}`. → SPieGeL: NFR-OPS-04.

## What SPieGeL can learn from it

- **The profile model, as data.** Use `prof:Profile` and the `altr-ext:` vocabulary to declare which profiles exist, which classes they apply to, which media types they offer and which is the default per class. Read `_profile` and `_mediatype` as well as `Accept-Profile` and `Accept`. Answer with `Link` headers (`rel="profile"`, `rel="alternate"`) and offer an `alt` representation that lists them all. This is the most directly reusable result of the review, for FR-CN-02, FR-CN-03, FR-CN-04 and FR-RA-03.
- **Deterministic ranking.** Rank the available representations explicitly: requested profile, class match, the class's default profile, requested media type, then the default media type. That makes negotiation testable (NFR-Q-02).
- **A typed query builder.** Building queries as a syntax tree rather than as text is the structural answer to NFR-SEC-04. In Jena that means the ARQ syntax classes (`Query`, `ElementGroup`, `ElementPathBlock`) or `ParameterizedSparqlString`. Prez also shows the counter-lesson: one builder must be used everywhere, with no exceptions for "simple" queries.
- **Declarative descriptions, at least as an option.** A SHACL shape with property paths and blank-node depth (FR-RA-04) is a compact way to say "what this profile contains". SPieGeL could offer it next to hand-written query files (FR-RA-02), generating queries for the simple cases and keeping files for the tuned ones. See [open question 19](../06-open-questions.md#architecture-and-operations).
- **Show the generated query.** Returning the query for a resource on request (`_mediatype=application/sparql-query`) is a cheap, powerful diagnostic. In SPieGeL it belongs behind NFR-SEC-08.
- **Labelled responses.** Adding labels and descriptions for every referenced IRI, partly from bundled copies of common vocabularies, lets a generic page show readable names without extra round trips (FR-HTML-01). SPieGeL can do this inside its HTML rendering, with a bounded cache.
- **Bounded counts.** Reporting "more than 100" instead of an exact total keeps listings cheap on large datasets (NFR-OPS-02).
- **SKOS hierarchy endpoints.** Top concepts and narrowers with child counts are exactly what an incremental tree view needs (FR-HTML-05).
- **Timeouts at both ends.** Prez sets an HTTP client timeout and also passes a timeout parameter to the store (`timeout=` by default, configurable through `SPARQL_TIMEOUT_PARAM_NAME`), so the store stops working as well (NFR-OPS-02).
- **Observability without a full tracing stack.** A correlation ID on every request (returned in `X-Request-ID`, with a separate client-supplied ID), JSON logs with typed attributes and units in the names (`duration_ms`), and the time spent waiting on downstream calls (NFR-OPS-05).
- **Testing set-up.** An embedded store for unit tests, Testcontainers for tests against a real triple store, and the OGC conformance test suite for the Features API. All three have direct equivalents on the JVM.

## Conclusion

- **As a replacement for the predecessor: no.** Prez does not dereference IRIs at their own paths, does not serve HTML, serves one store per instance and has no access levels. Its URI structure (CURIEs in fixed path levels) conflicts with the Flemish URI standard.
- **For phase 1 (a single public catalogue): possible, but awkward.** DCAT catalogues and SKOS schemes are what Prez is best at, and search and profiles come for free. However, it needs Prez UI for HTML and edge rewrites from `/id/` and `/doc/` paths to `/object?iri=`. That makes it a poorer stopgap than Trifid.
- **As a reference for SPieGeL: the most valuable of the three.** Its implementation of Content Negotiation by Profile is complete and data-driven, and SPieGeL should follow it closely. The typed query builder, bounded counts, SKOS hierarchy endpoints and observability practices are also worth adopting. The SHACL-generated queries are an idea to weigh, not to copy wholesale.
