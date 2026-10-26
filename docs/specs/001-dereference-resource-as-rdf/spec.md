# Feature 001: Dereference a resource as RDF

**Status:** Approved
**Created:** 2026-10-26
**Branch:** `001-dereference-resource-as-rdf`

---

## Problem Statement

SPieGeL does not yet answer a single URI. A consumer, harvester or crawler that looks up a URI of a domain must get the description of that thing in a common RDF format, assembled from the domain's configuration, without code per domain. This feature is the walking skeleton of phase 1: the first path from a URI, through configuration and a data source, to a response. Later features add caching, limits, framed JSON-LD, paged lists and HTML on top of it.

The decisions behind this feature are recorded in the ADRs; this spec states what the service must do, and refers to them for how.

## Goals

- A request for an `id` URI is redirected with `303` to its `doc` URI, without consulting any data.
- A request for a `doc` URI returns the description of the thing, bounded to its subject, in Turtle, N-Triples, RDF/XML or JSON-LD.
- A client chooses the format by `Accept`, by a format suffix or by the `_format` parameter, and every response says which other formats exist.
- Every error is answered as `application/problem+json` with a stable type, and never discloses a data source's message, a query text, or whether a hidden resource exists.
- A domain is described by configuration only: a new kind of resource needs a URI template and, optionally, query files, but no code.

## Non-Goals

- **Caching**, entity tags and `304 Not Modified` (NFR-OPS-03, [ADR 0020](../../adr/0020-cache-key-and-entity-tag.md)): feature 002.
- **A ceiling on the size of a description** (NFR-OPS-09), **backpressure** (NFR-OPS-02) and **rate limiting** (NFR-OPS-08, [ADR 0022](../../adr/0022-edge-concerns.md)): feature 002. Until then a description is not bounded in size.
- **Framed JSON-LD with a context per domain** (FR-CN-11, [ADR 0017](../../adr/0017-framed-json-ld-with-a-frozen-context.md)), and `application/json`: feature 003. JSON-LD in this feature is valid but not framed.
- **Paged lists of large properties** (FR-RA-07, [ADR 0015](../../adr/0015-paged-lists-as-resources.md)): feature 004.
- **HTML pages and HTML error pages** (FR-HTML-*, FR-CN-10 for browsers): feature 005, which also makes HTML the default format.
- **Profiles** (FR-CN-03), **several domains** (FR-MT-01), **authentication and access levels** (FR-AC-*): phase 2.
- **`ns` pages, Skolem IRIs, ELI paths, moved and removed URIs** (FR-URI-03 to 05, FR-URI-08 to 11): phase 2.

## Users and Context

**Primary users:** developers and machines that consume Linked Data: applications, harvesters of data catalogues, Linked Data crawlers and search engines that follow links. They send HTTP requests with or without an `Accept` header.

**Secondary users:** the Linked Data team, who write the domain's configuration and query files, and operators who need to trace a failed request.

**Usage context:** a consumer finds a URI in data, in a catalogue or in a link, and looks it up to learn what it identifies. It may look up many URIs in a row, in any of the supported formats.

**Terms:** a *domain* is a body of published data, such as IMJV; SPieGeL serves each domain as a *tenant*, with its own host name and configuration. See the [glossary](../../analysis/01-context/glossary.md).

**User mental model:** the URI is the identifier of a thing; looking it up gives "the data about it". Consumers expect the Cool URIs conventions (`303` from thing to document), standard content negotiation, and that a format-specific URL can be bookmarked and downloaded.

## User Stories

### Story 1: Look up a thing by its identifier

**As a** consumer of Linked Data
**I want** an `id` URI to lead me to the document that describes the thing
**So that** I can tell the thing apart from its description and still get its data

**Acceptance criteria:**

- [ ] **AC-1.1** Given a URI template pair `/id/{concept}/{id}` and `/doc/{concept}/{id}` When a client requests `GET /id/organisatie/42` with any `Accept` header Then the response is `303 See Other` with `Location` set to the absolute `doc` URI `https://data.example.org/doc/organisatie/42`, formed from the domain's configured base
- [ ] **AC-1.2** Given the data source holds nothing about `/id/organisatie/999` When a client requests `GET /id/organisatie/999` Then the response is still `303` to `/doc/organisatie/999`, and no query is sent to the data source
- [ ] **AC-1.3** Given a request `GET /id/organisatie/42?_format=ttl` When the redirect is answered Then `Location` is `/doc/organisatie/42?_format=ttl`
- [ ] **AC-1.4** Given the template variable `{id}` has the pattern `^[0-9]+$` When a client requests `GET /id/organisatie/abc` Then the response is `404` with problem type `not-found`, and no query is sent
- [ ] **AC-1.5** Given the domain's configured base is `https://data.example.org` and the request reaches SPieGeL as `http://internal-node:8080/id/organisatie/42` through the reverse proxy When the redirect is answered Then `Location` is `https://data.example.org/doc/organisatie/42`, and a later request for that `doc` URI queries the subject `<https://data.example.org/id/organisatie/42>`
- [ ] **AC-1.6** Given a path with a trailing slash, such as `/id/organisatie/42/` When it is requested Then the response is `404` with problem type `not-found`

### Story 2: Get the description of a thing in RDF

**As a** consumer of Linked Data
**I want** the `doc` URI to return what the domain publishes about the thing
**So that** I can use its data without knowing how the domain stores it

**Acceptance criteria:**

- [ ] **AC-2.1** Given the data source holds triples with `<…/id/organisatie/42>` as subject When a client requests `GET /doc/organisatie/42` with `Accept: text/turtle` Then the response is `200` and its graph contains every one of those triples
- [ ] **AC-2.2** Given a chain of four blank nodes, where level 1 is the value of a property of the subject and each level is the value of a property of the level before When the description is returned Then the triples of levels 1 to 3 are included, the triple from level 3 to level 4 is included, and the triples whose subject is level 4 are not
- [ ] **AC-2.3** Given the subject links to `<…/id/organisatie/7>`, which has an `rdfs:label`, an `rdf:type` and other properties When the description is returned Then it contains the label and type of `<…/id/organisatie/7>` and none of its other properties; the same holds for an IRI that is the value of a property of a nested blank node
- [ ] **AC-2.4** Given other resources in the data source link to the subject When the description is returned Then it contains none of their triples: the only triples whose object is the subject are those of the subject's own blank nodes and those of AC-2.5
- [ ] **AC-2.5** Given a description of `<…/id/organisatie/42>` at `<…/doc/organisatie/42>` When it is returned in any RDF format Then it contains `<…/doc/organisatie/42> foaf:primaryTopic <…/id/organisatie/42>` and `<…/id/organisatie/42> foaf:page <…/doc/organisatie/42>`
- [ ] **AC-2.6** Given the data source holds no triples with `<…/id/organisatie/999>` as subject When a client requests `GET /doc/organisatie/999` Then the response is `404` with problem type `not-found`
- [ ] **AC-2.7** Given a resource type is configured with two queries on two different data sources When its description is returned Then it contains the triples of both results, merged into one graph
- [ ] **AC-2.8** Given a request for a host name that is not configured When any path is requested Then the response is `404` with problem type `not-found`

### Story 3: Choose the format

**As a** consumer of Linked Data
**I want** to choose the RDF format by header or by URL
**So that** both HTTP clients and people with only a link can get the format they need

**Acceptance criteria:**

- [ ] **AC-3.1** Given a description exists When a client requests it with `Accept` set to `text/turtle`, `application/n-triples`, `application/rdf+xml` or `application/ld+json` Then the response has status `200`, that `Content-Type`, and a body that parses as that format into the same graph
- [ ] **AC-3.2** Given a description exists When a client requests `/doc/organisatie/42.ttl`, `.nt`, `.rdf` or `.jsonld` Then the response is in Turtle, N-Triples, RDF/XML or JSON-LD respectively, whatever `Accept` says
- [ ] **AC-3.3** Given a description exists When a client requests `/doc/organisatie/42?_format=nt` with `Accept: text/turtle` Then the response is in N-Triples
- [ ] **AC-3.4** Given a request with no `Accept` header, or with `Accept: */*` When the description is returned Then it is in Turtle
- [ ] **AC-3.5** Given `Accept: application/ld+json;q=0.5, text/turtle;q=0.9` When the description is returned Then it is in Turtle
- [ ] **AC-3.6** Given `Accept: application/rdf+xml, application/ld+json` with equal quality When the description is returned Then it is in JSON-LD, following SPieGeL's order of preference: Turtle, JSON-LD, N-Triples, RDF/XML
- [ ] **AC-3.7** Given a request negotiated by `Accept` When the response is `200` Then it has `Vary: Accept` and `Content-Location` set to the format-specific URL, such as `/doc/organisatie/42.ttl`
- [ ] **AC-3.8** Given a request for a format-specific URL (suffix or `_format`) When the response is `200` Then it has no `Vary: Accept` and no `Content-Location`
- [ ] **AC-3.9** Given any `200` response for a description When its headers are read Then it has a `Link` header with `rel="alternate"` and the matching `type` for each of the four formats, pointing to their suffix URLs
- [ ] **AC-3.10** Given any URL of this feature When a client sends `HEAD` instead of `GET` Then the status and headers are the same as for `GET`, and the body is empty
- [ ] **AC-3.11** Given `Accept: text/html` only, or `Accept: application/json` only When a description is requested Then the response is `406` with problem type `not-acceptable`
- [ ] **AC-3.12** Given `_format=xyz` When a description is requested Then the response is `406` with problem type `not-acceptable`
- [ ] **AC-3.13** Given a suffix and a `_format` that disagree, such as `/doc/organisatie/42.ttl?_format=nt` When the request is answered Then the response is `400` with problem type `invalid-request`
- [ ] **AC-3.14** Given a description with a predicate that cannot be written as RDF/XML, such as `<https://example.org/ns/eigenschap/123>` When a client requests it with the suffix `.rdf`, with `_format=rdf`, or with `Accept: application/rdf+xml` only Then the response is `406` with problem type `not-acceptable` and a `detail` saying that the description cannot be expressed in RDF/XML, and no part of an RDF/XML body is sent
- [ ] **AC-3.15** Given the same description When a client requests it with `Accept: application/rdf+xml, text/turtle;q=0.5` Then the response is `200` in Turtle
- [ ] **AC-3.16** Given `Accept: application/ld+json; profile="http://www.w3.org/ns/json-ld#compacted"` When a description is requested Then the response is `200` in JSON-LD; the profile parameter is ignored in this feature

### Story 4: Understand what went wrong

**As a** consumer or operator
**I want** errors in one predictable, safe format
**So that** my client can handle them, and a failure can be traced without leaking anything

**Acceptance criteria:**

- [ ] **AC-4.1** Given any error of this feature When it is answered Then the body is `application/problem+json` with `type` under `https://w3id.org/spiegel/problem/`, `title`, `status`, and `instance` as a `urn:uuid:` request identifier, whatever format the client asked for
- [ ] **AC-4.2** Given the data source answers with an error message When a description is requested Then the response is `502` with problem type `source-error`, and the body contains neither the data source's message nor any query text
- [ ] **AC-4.3** Given the data source does not answer within the configured time-out When a description is requested Then the response is `504` with problem type `source-timeout`
- [ ] **AC-4.4** Given a resource type with two queries, of which one fails When a description is requested Then the response is `502`, and no partial description is returned
- [ ] **AC-4.5** Given any `502` or `504` response When its headers are read Then it has `Cache-Control: no-store`
- [ ] **AC-4.6** Given a request with method `POST`, `PUT`, `PATCH` or `DELETE` When it is answered Then the response is `405` with `Allow: GET, HEAD` and problem type `method-not-allowed`
- [ ] **AC-4.7** Given an error response When its request identifier is looked up in the service's log Then exactly one log entry has that identifier, with the status and the duration
- [ ] **AC-4.8** Given an unexpected failure inside SPieGeL itself When a request is answered Then the response is `500` with problem type `internal-error`, a generic title, no stack trace, and `Cache-Control: no-store`

### Story 5: Describe a domain with configuration only

**As a** member of the Linked Data team
**I want** to publish a new kind of resource by changing configuration
**So that** no release of SPieGeL is needed for a domain's data

**Acceptance criteria:**

- [ ] **AC-5.1** Given a domain configured with only the generic template pair `/id/{concept}/{id}` and `/doc/{concept}/{id}` and no query files When any concept in the data is requested Then its description is returned with SPieGeL's default description query
- [ ] **AC-5.2** Given a more specific template `/doc/emissie/{id}` with its own query file When `/doc/emissie/5` is requested Then its own query is used, and `/doc/organisatie/42` still uses the default query
- [ ] **AC-5.3** Given a description query file that contains an update operation, a `SERVICE` clause, a syntax error, or a query form other than `CONSTRUCT` or `DESCRIBE` When the service starts Then it refuses the configuration, names the file and the reason, and does not serve requests
- [ ] **AC-5.4** Given a configuration that does not conform to SPieGeL's configuration shapes When the service starts Then it refuses the configuration and names each violation
- [ ] **AC-5.5** Given a configuration without a blank-node depth and without a time-out When the service starts Then the depth is 3 and the time-out per data source is 10 seconds

## Functional Requirements

### FR-1: URI templates and the `303`

URI templates come from the domain's configuration (FR-URI-01, [ADR 0012](../../adr/0012-tenant-configuration-as-rdf.md)). Template variables are typed by a pattern. When several templates match, the most specific wins: more literal characters, then more variables.

**Must:**
- answer an `id` URI with `303 See Other` to the matching `doc` URI, for every media type, carrying over the query string (FR-URI-02);
- answer a value that does not match its variable's pattern with `404`, without a query;
- answer a host name that is not configured, and a path that matches no template, with `404`;
- form every IRI and URL it uses or sends (the subject IRI, `Location`, `Link` and `Content-Location`) as absolute URIs from the domain's configured base, never from the scheme, host or port of the request;
- match patterns against the percent-decoded value of each path segment, and treat a trailing slash as a different path.

**Must not:**
- consult the data to answer an `id` URI ([request flow, step 5](../../analysis/05-architecture/request-flow.md#steps));
- check a matching value against a list of known resources: the data decides whether it exists.

### FR-2: The description of a resource

**Must:**
- accept only `CONSTRUCT` and `DESCRIBE` as description queries, and refuse any other form when the configuration is loaded;
- run the queries configured for the resource type, or SPieGeL's default description query when none are configured, with the resource IRI bound as a term (FR-RA-01, FR-RA-02);
- merge the results of all queries into one graph;
- include the subject's own triples and blank nodes nested to the configured depth (3 by default): level 1 is a blank node that is the value of the subject's property, and the triple pointing to the first level beyond the depth is kept, but that level's own triples are not (FR-RA-04);
- include the label (`rdfs:label`, `skos:prefLabel`, `dct:title`) and type of every IRI that is an object in the description, including inside nested blank nodes (FR-RA-06, [ADR 0006](../../adr/0006-resources-and-event-streams.md));
- add `foaf:primaryTopic` from the `doc` URI to the `id` URI and `foaf:page` back (FR-URI-07);
- answer `404` when the merged description holds no triple about the subject (FR-URI-06).

**Must not:**
- include incoming relations, or properties of linked resources other than their label and type;
- return a partial description when one of its queries failed or timed out.

### FR-3: Content negotiation over RDF formats

**Must:**
- offer Turtle (`text/turtle`, `.ttl`), N-Triples (`application/n-triples`, `.nt`), RDF/XML (`application/rdf+xml`, `.rdf`) and JSON-LD (`application/ld+json`, `.jsonld`) for every description (FR-CN-01);
- recognise a format suffix before matching the URI template, so that a template value cannot end in a format suffix;
- choose the format, in this order: a format suffix, then `_format`, then `Accept` weighed against SPieGeL's preference (Turtle, JSON-LD, N-Triples, RDF/XML) (FR-CN-02, FR-CN-08);
- serve Turtle when there is no `Accept` header or only `*/*`, until HTML exists (feature 005);
- send `Vary: Accept` and `Content-Location` on a response negotiated by `Accept`, and neither on a format-specific URL (FR-CN-06, FR-CN-07);
- send a `Link` header with `rel="alternate"` and `type` for each format (FR-CN-04);
- answer `HEAD` with the same status and headers as `GET` (FR-CN-09);
- accept media type parameters in `Accept`, such as a JSON-LD `profile`, and ignore them in this feature;
- check that a description can be written in the chosen format before sending any part of it. A description whose predicates cannot be written as RDF/XML gives `406` when RDF/XML was asked for explicitly, by suffix, by `_format` or as the only acceptable format; when it was negotiated, the next acceptable format is chosen instead.

**Must not:**
- answer with a format the client did not accept: no acceptable format gives `406` (FR-CN-05);
- send a `200` status and then fail while writing the body;
- offer `text/html` or `application/json` in this feature.

### FR-4: Errors

**Must:**
- answer every error as `application/problem+json`, with the types `not-found` (`404`), `not-acceptable` (`406`), `invalid-request` (`400`), `method-not-allowed` (`405`), `source-error` (`502`), `source-timeout` (`504`) and `internal-error` (`500`) under `https://w3id.org/spiegel/problem/` (FR-CN-10, [ADR 0016](../../adr/0016-problem-details-for-errors.md));
- give each error a `urn:uuid:` request identifier in `instance`, which is also the identifier in the log;
- send `Cache-Control: no-store` with every `5xx`.

**Must not:**
- copy a data source's message, a stack trace or a query text into an error;
- answer a resource differently depending on whether it does not exist or may not be seen (NFR-SEC-07).

## Non-Functional Requirements

### Performance

- With a data source that answers each query within 50 ms, the time SPieGeL adds to a description request is at most 50 ms at the 95th percentile, measured over at least 1,000 requests for different resources. (The end-to-end targets of NFR-PERF-01 depend on the store and on the cache of feature 002.)
- A `303` for an `id` URI is answered within 20 ms at the 95th percentile, since it needs no query.

### Security

- No request causes an update operation on a data source; the configured credentials are read-only (NFR-SEC-01).
- Values from a request enter a query only as bound terms, never as text (NFR-SEC-04).
- SPieGeL contacts only the data sources in its configuration, never a URL taken from a request (NFR-SEC-06).
- Every response, including `303` and errors, carries `Access-Control-Allow-Origin: *` and exposes `Link`, `Location` and `Content-Location` to scripts (NFR-OPEN-01).
- Logs hold no full IP address, no query values other than `_format`, and no personal data from the request (NFR-PRIV-02, [ADR 0018](../../adr/0018-personal-data-in-requests-and-caches.md)).

### Reliability

- A data source that fails or times out affects only the requests that need it; the service keeps answering `id` redirects and other requests.
- The service does not start with a configuration that fails validation; it never serves with a partially loaded configuration ([ADR 0013](../../adr/0013-tenant-configuration-delivery.md)).
- Every request is logged once, in a structured format, with its request identifier, status and duration (NFR-OPS-05).

## Error Scenarios

| Scenario | Expected Behavior |
|----------|-------------------|
| Unknown host name | `404`, `not-found` |
| No template matches the path | `404`, `not-found` |
| Template variable does not match its pattern | `404`, `not-found`, no query |
| Empty description | `404`, `not-found` |
| No acceptable format, unknown `_format`, or only `text/html` or `application/json` accepted | `406`, `not-acceptable` |
| Suffix and `_format` disagree | `400`, `invalid-request` |
| Description cannot be written as RDF/XML, which was asked for explicitly | `406`, `not-acceptable`, with a `detail` saying so |
| Same, but RDF/XML was negotiated among other acceptable formats | `200` in the next acceptable format |
| Path with a trailing slash | `404`, `not-found` |
| Method other than `GET` or `HEAD` | `405`, `method-not-allowed`, `Allow: GET, HEAD` |
| Data source returns an error, or a result that cannot be parsed | `502`, `source-error`, `Cache-Control: no-store`, no store message or query text |
| Data source does not answer within the time-out | `504`, `source-timeout`, `Cache-Control: no-store` |
| One of several queries fails | `502`, no partial description |
| Unexpected failure inside SPieGeL | `500`, `internal-error`, generic title, no stack trace, `Cache-Control: no-store` |
| Configuration invalid at start-up, including a description query that is not `CONSTRUCT` or `DESCRIBE` | Service refuses to start and names each violation |

## Open Questions

None block this feature. The defaults for the blank-node depth (3) and the time-out per data source (10 seconds) were agreed when this spec was drafted, and are configurable. Questions for later features remain in the [open questions](../../analysis/06-open-questions.md), in particular the predecessor's load (question 36) and the kind of store per access level (question 29).

## Out of Scope (Future Considerations)

- Feature 002: caching tied to the data version, entity tags and `304`; a ceiling on the size of a description; bounded pools per data source with `503`; rate limiting with `429`.
- Feature 003: JSON-LD framed with a frozen context per domain, and `application/json`.
- Feature 004: paged lists of large properties and incoming relations.
- Feature 005: HTML pages and HTML error pages; HTML becomes the default format.
- Phase 2: profiles, several domains, `ns` pages, Skolem IRIs, ELI paths, moved and removed URIs, authentication and access levels.
