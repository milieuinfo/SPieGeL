# 0023. Link resources to their event streams from configuration, and serve versions as ordinary resources

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

[ADR 0006](0006-resources-and-event-streams.md) makes resource endpoints and event streams (LDES) two forms of the same data: SPieGeL serves a resource's current state, an LDES server its history. SPieGeL does not serve streams, but makes them discoverable (FR-ES-01). [Open question 30](../analysis/06-open-questions.md#architecture-and-operations) asked two things: how a resource points to the stream that publishes it, and whether SPieGeL serves the version objects that streams often contain.

**Discovery.** A domain's dataset description is in DCAT and VoID (FR-META-01), and every page already links to it with `rel="describedby"` ([ADR 0019](0019-findability.md)). A stream can be listed there as a `dcat:DataService`. That gives a path from a resource to its stream in two steps, but a client must then work out from the DCAT which stream holds which kind of resource.

LDES links only from a stream to its members (`tree:member`); it has no standard property for the other direction. A stream usually publishes one collection, such as all resources of one type, which matches a URI template or resource type in the tenant configuration ([ADR 0012](0012-tenant-configuration-as-rdf.md)). The IANA link relation `collection` ([RFC 6573](https://www.rfc-editor.org/rfc/rfc6573)) says that "the target is a collection of which the context is a member".

A triple per resource in the data would repeat the same statement millions of times, without a standard property to use. SPieGeL adds no triples of its own to a description's data ([ADR 0021](0021-labels-of-external-vocabularies.md)).

**Versions.** LDES members are often version objects with IRIs of their own, related to the resource by `dct:isVersionOf`. If such an IRI is under a host that SPieGeL serves and cannot be dereferenced, it breaks a basic promise of Linked Data. Serving past states by date, for example with Memento ([RFC 7089](https://www.rfc-editor.org/rfc/rfc7089)), would require SPieGeL to understand time and every store to keep the full history of every resource, which is what the stream already offers.

## Decision

1. **A domain lists its streams in its dataset description**, each as a `dcat:DataService` with its endpoint and the dataset it serves (FR-META-01). The front page shows them.
2. **A resource links directly to the stream that publishes it.** The tenant configuration says, per URI template or resource type, which stream publishes those resources. SPieGeL then sends `Link: <stream>; rel="collection"` with every format, and shows the link on the HTML page.
3. **The link is configuration, not data.** SPieGeL adds no triple to the RDF of the description. The data owner maintains nothing per resource.
4. **Version objects are ordinary resources.** If a version IRI matches a URI template and the store holds the version, SPieGeL describes it like any other resource. If the store does not hold it, the answer is `404`, as for any unknown resource. Whether a store keeps versions is decided by the loading chain, for example by an LDES client that materialises the stream.
5. **No mechanism of its own for versions.** A version links to its resource through `dct:isVersionOf` in the data, which SPieGeL shows as any link. The versions of a resource are its incoming relations by `dct:isVersionOf`, available as a paged list ([ADR 0015](0015-paged-lists-as-resources.md)). SPieGeL does not negotiate by date (Memento) and does not reconstruct past states.

## Consequences

- A client finds a resource's stream in one step, from any format, and a person sees it on the page. A client that wants the full picture still finds every stream in the dataset description.
- `rel="collection"` for an event stream is an interpretation of RFC 6573, not part of the LDES specification. SPieGeL documents it; if LDES later defines a relation of its own, SPieGeL can send both.
- The configuration must follow the streams: when a stream is added, moved or split, the tenant configuration changes with it. A wrong link is not detected by SPieGeL.
- Version IRIs on SPieGeL's hosts can be dereferenced wherever the store holds them, with no work in SPieGeL beyond a URI template. A domain that wants this needs a store that keeps versions; a domain that does not, answers `404` for them.
- The current state in SPieGeL and the latest version in the stream must still agree, as ADR 0006 requires. That stays the responsibility of the loading chain.
- Anyone who wants the history as a whole, or the state on a given date, replicates the stream. SPieGeL does not compete with the LDES server.
