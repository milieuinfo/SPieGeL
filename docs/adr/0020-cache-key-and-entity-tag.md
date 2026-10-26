# 0020. Key the cache by profile, and tag responses with data and configuration version

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

The [request flow](../analysis/05-architecture/request-flow.md) caches the assembled `ResourceDescription`, an RDF graph, under (tenant, resource IRI, profile, access level) in step 10. The media type is not part of the key: a description cached for a Turtle request serves a later JSON-LD or HTML request with the same profile, and only serialisation is repeated. The chapter asks for this choice to be confirmed in an ADR.

The `ETag` is the tenant's data version, combined with the variant: media type, profile and language (NFR-OPS-03). A cached description stays valid until the loading chain signals a new data version.

A response, however, depends on more than the data:

- **the tenant's configuration** ([ADR 0012](0012-tenant-configuration-as-rdf.md)): which queries run, how deep blank nodes go, which profiles exist, which context shapes the JSON-LD ([ADR 0017](0017-framed-json-ld-with-a-frozen-context.md)), which templates, presentation rules and label sources build the HTML;
- **SPieGeL itself**: its serialisers, its frame, its default templates and its schema.org table ([ADR 0019](0019-findability.md)).

Both change without a new data version. A configuration is rolled out as a new artefact with a rolling restart ([ADR 0013](0013-tenant-configuration-delivery.md)). Today the cache is in memory, so a restart empties it, but a shared cache is planned (`CachePort`, [Hexagonal design](../analysis/05-architecture/hexagonal-design.md)). And whatever SPieGeL's own cache does, a client or proxy that revalidates with an `ETag` of the data version alone gets `304 Not Modified` after a rollout, and keeps the old content. For a domain loaded a few times a year, that lasts months. During a rolling restart, two nodes serve different content under the same tag.

An `ETag` promises that the same tag means the same representation. That holds only if the tag covers everything the representation depends on.

Serialising per response has a cost, largest for framed JSON-LD and HTML. The predecessor's logs show that the same resource is rarely asked for in several formats ([Performance and availability](../analysis/03-requirements/performance.md#the-predecessor-today)), so a cache of serialised bytes would save little, and would itself depend on the context and templates.

## Decision

1. **The cache holds the description, not its serialisation.** The key is (tenant, resource IRI, profile, access level). The media type and the interface language are not part of it; they are applied after the cache, in step 14.
2. **A cached description is immutable.** It is a read-only graph that several requests may serialise at the same time.
3. **The cache is bound to the deployment.** An entry is valid for one data version, one configuration version of the tenant and one SPieGeL version. A shared cache keeps entries of different versions apart, so that nodes on the old and the new version never serve each other's descriptions during a rolling restart.
4. **The `ETag` covers everything a response depends on:** the data version, the configuration version, the SPieGeL version and the variant (media type, profile, language). It is sent as a short hash of these parts, so that it reveals no internal versions.
5. **The `ETag` is weak** (`W/"…"`). Two nodes may write the same graph in a different order of triples or prefixes; the representations are equivalent, not byte for byte identical. Weak tags suffice for `If-None-Match` and `304`.
6. **No cache of serialised bytes**, until the spike of ADR 0017 or measurements against NFR-PERF-01 show that serialisation from the cache is too slow.

## Consequences

- One description serves every format of a profile, so its queries run once, whatever format is asked for first.
- Turtle and HTML share an entry only when they use the same profile. Where a resource type has a lighter default profile for HTML (step 7), HTML has its own entry.
- After a rollout of configuration or of SPieGeL, every description is fetched again once, and every client gets the new content on its next revalidation. Because configuration changes rarely, this costs little.
- Every response from the cache is still serialised. Its cost counts towards the target for cached responses (p95 ≤ 50 ms, NFR-PERF-01), and must be measured, for JSON-LD in particular.
- The configuration version and the SPieGeL version must be known at run time; ADR 0013 already makes the configuration version visible.
- The `304` after step 13 needs no query and no serialisation: the tag is computed from versions and the negotiated variant alone.
