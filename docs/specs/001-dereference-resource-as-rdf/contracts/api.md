# API Contracts: Feature 001, Dereference a resource as RDF

## GET /id/{path}

**Purpose:** redirect from a thing to the document that describes it. **Spec requirement:** FR-1, Story 1.

Request: `GET` or `HEAD`, any `Accept`, optional format suffix and query string.

Response `303 See Other`: `Location` = the absolute `doc` URI from the tenant's base, with the suffix and the query string carried over. CORS headers. No body.

| Status | Condition | Body |
|---|---|---|
| 404 | unknown host, no template, value does not match its pattern, trailing slash | problem `not-found` |
| 405 | method other than `GET` or `HEAD` | problem `method-not-allowed`, `Allow: GET, HEAD` |

## GET /doc/{path}[.ttl|.nt|.rdf|.jsonld][?_format=ttl|nt|rdf|jsonld]

**Purpose:** the bounded description of a thing in RDF. **Spec requirement:** FR-2, FR-3, Stories 2 and 3.

Request: `GET` or `HEAD`; the format from the suffix, then `_format`, then `Accept` (preference Turtle, JSON-LD, N-Triples, RDF/XML; no `Accept` gives Turtle).

Response `200 OK`: body in the chosen format; `Content-Type`; `Link` with `rel="alternate"` and `type` for the four suffix URLs; when negotiated by `Accept`, `Vary: Accept` and `Content-Location` = the suffix URL; CORS headers. The graph holds the subject's triples, blank nodes to the tenant's depth, label and type of linked IRIs, and `foaf:primaryTopic` / `foaf:page`.

| Status | Condition | Body |
|---|---|---|
| 400 | suffix and `_format` disagree; unreadable `Accept` | problem `invalid-request` |
| 404 | as for `/id`, or an empty description | problem `not-found` |
| 405 | method other than `GET` or `HEAD` | problem `method-not-allowed` |
| 406 | nothing acceptable; unknown `_format`; RDF/XML asked for explicitly but impossible | problem `not-acceptable` |
| 500 | unexpected failure | problem `internal-error`, `Cache-Control: no-store` |
| 502 | a data source failed or returned an unreadable result | problem `source-error`, `Cache-Control: no-store` |
| 504 | a data source did not answer in time | problem `source-timeout`, `Cache-Control: no-store` |

Every problem body: `{"type": "https://w3id.org/spiegel/problem/…", "title": "…", "status": 404, "detail": "…" (optional), "instance": "urn:uuid:…"}`.
