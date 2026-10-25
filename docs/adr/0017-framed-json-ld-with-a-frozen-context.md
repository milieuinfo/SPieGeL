# 0017. Serve framed JSON-LD with a generated, frozen context

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

FR-CN-01 makes every description available as JSON-LD, but says nothing about its shape. Written without a frame and without a context, JSON-LD is a flat `@graph` of nodes with full IRIs as keys and every value in an array. A web developer must then find the subject among the nodes, look up blank nodes by their identifier and join labels of linked resources by hand. ELDA and Prez offer short names instead (see the [landscape](../analysis/04-landscape/comparison.md)).

Two different things are needed:

- a **frame**, which decides the **shape**: a tree with the subject at its root, with nested values;
- a **context**, which decides the **keys and values**: `prefLabel` instead of an IRI, an IRI value as `"@id"`, a language map instead of a list of tagged strings, always an array or a single value.

The frame can be the same for every description, because a description is bounded to its subject ([ADR 0006](0006-resources-and-event-streams.md)): its own properties, nested blank nodes, and the label and type of linked resources.

A context can be derived by an algorithm. Per predicate it chooses a short name, and finds whether values are IRIs, which datatype literals have, whether there are languages and whether there can be several values. SHACL shapes say this directly (`sh:path`, `sh:nodeKind`, `sh:class`, `sh:datatype`, `sh:maxCount`). Where a domain has no shapes, the derived structure of its data says it (FR-HTML-15).

Deriving a context at run time, per response or per data version, makes keys and shapes depend on the data. A second value turns a string into an array, and a new predicate in another namespace can take the name `title`. Client code then breaks silently, although the RDF is correct. A key that a client relies on must keep its meaning and its shape.

The JSON-LD 1.1 processor in Jena 6 is Titanium, which supports compaction and framing. Whether Jena's writer passes a frame through, or SPieGeL calls Titanium directly, is still to be checked.

## Decision

1. **Every JSON-LD description is framed by one generic frame:**
    - the subject, the `id` URI of the request, is the root object;
    - blank nodes are nested in place (FR-RA-04);
    - linked resources appear as an object with only what the description holds about them: `id`, label and type;
    - `@embed` is `@always`, so that a node has the same shape wherever it appears.

    The frame needs no configuration.
2. **Each tenant has a context, generated and then frozen.** A generator, shipped with SPieGeL as a tool, proposes the context in the CI of the configuration repository ([ADR 0013](0013-tenant-configuration-delivery.md)):
    - from the domain's SHACL shapes first, and otherwise from the derived structure of its data;
    - **names:** the local name of each predicate. On a collision, the predicate in the namespace that comes first in the tenant's prefix order keeps the short name, and the other one gets a prefixed name;
    - **values:** `"@type": "@id"` for IRIs, the datatype for typed literals, `"@container": "@language"` for language-tagged text, and `"@container": "@set"` unless the shape says there is at most one value;
    - `id` and `type` as aliases of `@id` and `@type`.

    The Linked Data team reviews the proposal. The result is a file in the configuration, like any other.
3. **A published context only grows.** A term, once published, keeps its IRI, its type and its container. New terms may be added. A change to an existing term needs a new context version with its own URI, and the earlier versions remain served.
4. **SPieGeL serves the contexts itself**, from the tenant's own host, under a versioned URI such as `/context/v3.jsonld`, with long-lived caching. A description refers to it by URI. No context is fetched from another host at run time (NFR-SEC-06).
5. **What the context does not know stays lossless.** A predicate without a term becomes a prefixed name, or a full IRI, so that an RDF client always gets the same triples.
6. **Plain JSON is offered as well.** A request for `application/json` gets the same framed document, with the context in a `Link` header (`rel="http://www.w3.org/ns/json-ld#context"`), as JSON-LD 1.1 allows.
7. **The paged lists of [ADR 0015](0015-paged-lists-as-resources.md) use the same mechanism**, with the Hydra terms they need added to the context.

## Consequences

- A web developer gets a document such as `data.prefLabel.nl` or `data.broader[0].prefLabel.nl`, and it is still valid JSON-LD.
- The context is a public contract per tenant. Reviewing additions is a small, recurring task for the Linked Data team, and it is where a client-breaking change would be noticed.
- New predicates in the data appear as prefixed names until the context is extended. Nothing breaks, but the JSON is less tidy until then.
- Domains with SHACL shapes get the best contexts. Without shapes, the generator guesses from the data, for example "at most one value" where every resource happens to have one, so its proposals need more care in review.
- A short spike must confirm how framing and compaction are done with Jena 6 and Titanium, and what they cost per response against NFR-PERF-01.
- The context path, such as `/context/`, is reserved on every host, like `/sparql`.
