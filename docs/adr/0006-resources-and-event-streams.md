# 0006. Publish resources and event streams as complementary forms

- **Status:** Proposed
- **Date:** 2026-10-06

## Context

The department publishes Linked Data in two ways. **Resource endpoints** answer a request for one URI with the current description of that resource: that is SPieGeL's job. **[Linked Data Event Streams](https://semiceu.github.io/LinkedDataEventStreams/)** (LDES) publish every change to a collection of resources as an append-only stream that clients can replicate and keep in sync. The department leads the [OpenLDES](https://github.com/OpenLDES) project, which builds an LDES server and client components (LDIO), and intends to make LDES a real part of its publication.

Both forms need one information architecture. Without it, each domain decides for itself how much of the surrounding graph a description contains. That makes descriptions unpredictable in size, duplicates data across endpoints and streams, and leaves consumers unsure where the authoritative description of a resource lives.

## Decision

1. **Two forms of the same data.** Resource endpoints and event streams publish the same resources, with the same URIs and the same application profiles (OSLO, SHACL shapes). A resource endpoint serves a resource's current state; an event stream serves its history and lets a consumer replicate it.
2. **A description is bounded to its subject.** It holds the subject's own properties. An object that is itself a resource appears by its IRI only: it is described at its own endpoint or in its own stream. There are two exceptions:
    - the **label** (and, where needed, the type) of a linked resource, so that it can be presented without a further request (FR-HTML-10);
    - **values without an identity of their own**, such as a measurement with its unit, published as a blank node or a Skolem IRI (FR-RA-04, FR-URI-04).
3. **Integration happens on the consumer's side.** A consumer that needs more than one resource combines the data itself: by dereferencing the IRIs it finds, by replicating several streams with an LDES client and joining them, or by querying a domain's SPARQL endpoint. SPieGeL does not join data across domains on the consumer's behalf.
4. **SPieGeL does not serve event streams.** Streams are served by an LDES server, such as OpenLDES's. SPieGeL makes them discoverable from the domains it publishes.
5. **Streams may feed SPieGeL's stores.** An LDES client that materialises a stream into a store (for example LDIO) is a valid part of the loading chain (open question 15). It also fits the derived public stores of open question 29.

## Consequences

- Descriptions have a predictable size, and their queries become simpler: they select the subject's own triples plus the agreed exceptions. This limits, but does not remove, the risk of very large resources: a subject can still have many values for one property.
- **Incoming relations are not part of a description.** The HTML view shows them through follow-up requests (FR-HTML-05). An RDF client finds them through SPARQL or by replicating the stream that holds them.
- Consumers make more requests, or run an LDES client, to assemble a larger dataset. That is the intended division of work, and it is cacheable on both sides.
- The state that SPieGeL serves must equal the latest state in the stream. Both forms must therefore come from the same source and the same shapes. Whether SPieGeL also serves version objects and past states is open question 30.
- Discovery between both forms needs a convention: from a domain to its streams, and from a resource to the stream that publishes it (FR-ES-01, open question 30).
- Integration across stores and domains on the consumer's side, for example with a federating query engine such as Comunica, is left for later (open question 31).
