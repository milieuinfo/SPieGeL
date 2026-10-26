# 0016. Answer errors to machines with problem details (RFC 9457)

- **Status:** Accepted
- **Date:** 2026-10-25
- **Amended:** 2026-10-26: added `method-not-allowed`, `internal-error`, and the `406` for a description that cannot be written in the format asked for ([spec 001](../specs/001-dereference-resource-as-rdf/spec.md))

## Context

The [request flow](../analysis/05-architecture/request-flow.md#errors) lists SPieGeL's errors (E1 to E7) and how HTML shows them (`vl-http-error-message`), but not what a client gets that asked for Turtle, JSON-LD or SPARQL results. Some rules already hold:

- an unknown resource and one the caller may not see give the same `404` (FR-URI-06, NFR-SEC-07);
- a `502` or `504` never includes the store's error message or the query text (E2, E3);
- `429` and `503` carry `Retry-After` (NFR-OPS-02, NFR-OPS-08);
- a `410` is not an error but a tombstone, described in RDF (FR-URI-10).

On `/sparql`, a `400` is most useful when it says where the query went wrong, or which rule of the query policy it broke ([ADR 0011](0011-query-policy.md)).

[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) (*Problem Details for HTTP APIs*) defines `application/problem+json`, with a `type` IRI per kind of problem, a `title`, the `status`, a `detail` and an `instance`. HTTP libraries in most languages understand it. An error could also be negotiated into the requested RDF format, with Hydra's error terms, but an error is not data about the resource, and a client checks the status before it reads the body. For a `406`, SPieGeL cannot answer in a requested format by definition.

## Decision

1. **A browser gets an HTML error page; every other client gets `application/problem+json`**, whatever RDF format or result format it asked for, including on `406`. HTML is chosen by the same negotiation as for descriptions (FR-CN-08).
2. **Every problem has a stable `type` IRI**, under `https://w3id.org/spiegel/problem/`, documented with SPieGeL. For example:

    | Status | `type` | When |
    |---|---|---|
    | `400` | `…/problem/invalid-query` | `/sparql`: a syntax error, an update, or a query that breaks the query policy |
    | `400` | `…/problem/invalid-request` | an invalid parameter, such as `_page` or `_property` |
    | `404` | `…/problem/not-found` | no template, no resource, or a resource the caller may not see |
    | `405` | `…/problem/method-not-allowed` | a method other than those the resource allows, with `Allow` |
    | `406` | `…/problem/not-acceptable` | no acceptable media type, or the description cannot be written in the format asked for |
    | `429` | `…/problem/rate-limited` | this client is over its limit |
    | `500` | `…/problem/internal-error` | an unexpected failure inside SPieGeL itself |
    | `502` | `…/problem/source-error` | a data source failed |
    | `503` | `…/problem/overloaded` | the pool or queue of a data source is full |
    | `504` | `…/problem/source-timeout` | a data source did not answer in time |

3. **The body is safe by construction.** `title` and `detail` are written by SPieGeL, in English, and never copy a store's message, a stack trace or a query text. An `internal-error` has a generic title and no `detail`; the cause is in the logs, under the request identifier. The `404` body is the same for a resource that does not exist and for one the caller may not see.
4. **`instance` carries the request identifier** as a `urn:uuid:` URI, the same identifier that the logs use (NFR-OPS-05), so that a report can be traced without exposing anything else.
5. **A query error on `/sparql` says where.** An `invalid-query` problem adds `line` and `column` from the parser, or the policy rule that was broken, such as "`SERVICE` is not allowed". It does not echo the query.
6. **Error responses keep the response rules:** CORS as for any public response (NFR-OPEN-01), so that scripts in a browser can read the error; `Retry-After` on `429` and `503`; `Vary` where the format was negotiated; and `Cache-Control: no-store` on `429` and every `5xx`.

## Consequences

- Clients get one predictable error format, which HTTP libraries already parse, for descriptions, lists, `/sparql` and the dataset description alike.
- The `type` IRIs become part of SPieGeL's public contract: new ones may be added, existing ones keep their meaning. The namespace must be registered at w3id.org, like the configuration vocabulary ([ADR 0012](0012-tenant-configuration-as-rdf.md)).
- An RDF client gets JSON on an error, not RDF. We accept this: an error is not a description of the resource.
- A tombstone (`410`) and a moved resource (`301`) stay outside this scheme, because they are answers about the resource, not errors.
- The error texts for machines are English only. HTML error pages follow the visitor's language (FR-HTML-12).
