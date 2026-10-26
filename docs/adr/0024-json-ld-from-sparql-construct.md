# 0024. Compact JSON-LD from CONSTRUCT and DESCRIBE with the tenant's context, unless another form is asked for

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

[ADR 0017](0017-framed-json-ld-with-a-frozen-context.md) serves descriptions as JSON-LD framed by one generic frame and compacted with a frozen context per tenant. It says nothing about `/sparql`. A `CONSTRUCT` or `DESCRIBE` result is an arbitrary graph without a single subject, so the frame does not apply, but the context still could: it gives short keys and stable shapes without a frame. This was [open question 41](../analysis/06-open-questions.md#architecture-and-operations).

On `/sparql` the store returns a fixed format, and SPieGeL writes the negotiated one ([request flow](../analysis/05-architecture/request-flow.md#sparql), steps 5 and 6). JSON-LD is written once the result is complete, within the result limit, so compaction adds processing time but no new limit on size.

[JSON-LD 1.1](https://www.w3.org/TR/json-ld11/#iana-considerations) defines a `profile` parameter on `application/ld+json`, by which a client asks for a form: `http://www.w3.org/ns/json-ld#expanded`, `…#compacted`, `…#flattened` or `…#framed`. A server may ignore it.

Without compaction, a web developer gets full IRIs, unlike what `/doc` returns. With compaction always, a client that wants expanded JSON-LD has no choice.

## Decision

1. **SPieGeL follows the JSON-LD `profile` parameter** for `CONSTRUCT` and `DESCRIBE` results on `/sparql`:

    | Asked for | Result |
    |---|---|
    | no profile, or `…#compacted` | compacted with the tenant's current context, referred to by its URI (ADR 0017) |
    | `…#expanded` | expanded, without context |
    | `…#flattened` | flattened, without context |
    | `…#framed` | not supported: compacted, as without a profile |

2. **The result stays lossless.** A predicate without a term in the context becomes a prefixed name or a full IRI (ADR 0017, decision 5).
3. **The response says which form it has**, with the profile in `Content-Type`, and `Vary: Accept`.
4. **Compaction happens in the SPARQL adapter**, which writes the result. The use case passes it the tenant's context with the handle; the core neither holds nor writes the result ([ADR 0010](0010-jena-graph-types-in-the-core.md)).

## Consequences

- A query from a web application returns the same keys as the descriptions on `/doc`, with no context of its own.
- Clients that want plain RDF in JSON-LD, such as generic RDF libraries, ask for `expanded` or `flattened`, or use another format. Any form is valid JSON-LD, so a client that ignores the profile still gets the same triples.
- Compaction costs time per response, bounded by the result limit. `/sparql` is not cached by default, so the cost is paid on every request; it is to be measured in the spike of ADR 0017.
- The forms are more variants to test, with the same context as the descriptions.
- Descriptions on `/doc` remain framed (ADR 0017). Whether they also honour `expanded` and `flattened` is not decided here.
