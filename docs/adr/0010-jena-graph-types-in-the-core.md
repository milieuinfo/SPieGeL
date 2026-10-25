# 0010. Use Jena's graph types in the core, keep SPARQL out of it

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

[Open question 14](../analysis/06-open-questions.md#architecture-and-operations) asks whether the domain core uses Apache Jena's RDF types or an RDF abstraction of its own. The [hexagonal design](../analysis/05-architecture/hexagonal-design.md#rules-that-keep-the-core-clean) left this to an ADR. NFR-Q-01 forbids a dependency on Spring, on Jena's HTTP client and on any web framework, but not on Jena as a library.

Most of the core's work is RDF work:

- merging the results of several queries into one description (FR-RA-01);
- following blank nodes to a configured depth (FR-RA-04);
- unpacking `owl:unionOf` and `owl:intersectionOf` lists (FR-RA-05);
- adding `foaf:primaryTopic` and `foaf:page` (FR-URI-07);
- counting triples against the ceiling (NFR-OPS-09);
- paging large properties (FR-RA-07);
- choosing a label by language (FR-HTML-10).

An abstraction of its own would mean writing and testing all of this again, and converting every result from and to Jena in the adapters. Its usual justification, bridging two RDF libraries, no longer applies: Ontop, which is built on RDF4J, runs as a separate service ([ADR 0009](0009-ontop-for-relational-sources.md)). In RDF systems the graph itself is the domain model, and mapping triples into objects of our own and back adds code without adding anything.

Jena offers two levels. The **graph level** (`org.apache.jena.graph`: `Graph`, `Node`, `Triple`) is the foundation the rest of Jena is built on, and is small and stable. The **model level** (`org.apache.jena.rdf.model`: `Model`, `Resource`, `Statement`) is a richer layer on top of it.

The SPARQL engine, the HTTP client and the RIOT parsers and serialisers live in `jena-arq`, a separate artefact. The graph and model levels live in `jena-core`.

## Decision

1. **The core uses Jena's graph level:** `Graph`, `Node` and `Triple` from `jena-core`. `ResourceDescription` wraps a `Graph`. Inside a method, the core may use the model level as a view on that graph where it makes the code clearer, but its ports and domain types expose the graph level only.
2. **The core does not depend on `jena-arq`.** It neither parses nor runs SPARQL, and does not read or write RDF formats. For the core, a query is text with bindings: a `QuerySpec` holds the query text, and the resource IRI is passed as a `Node`. Binding happens in the SPARQL adapter (NFR-SEC-04). Inspecting a query against a policy is done behind a port ([ADR 0011](0011-query-policy.md)).
3. **`ArchitectureTest` enforces this.** Classes in the core must not depend on `org.apache.jena.sparql..`, `org.apache.jena.query..`, `org.apache.jena.riot..`, `org.apache.jena.http..` or `org.apache.jena.rdflink..`.

## Consequences

- Merging, blank nodes, lists and language tags use Jena's tested code. The adapters pass results to the core without converting them.
- SPieGeL is tied to Jena. Replacing it would touch the core as well as the adapters. We accept this: Jena is the project's RDF library ([ADR 0004](0004-technology-baseline.md)), and the graph level is its most stable API.
- `spiegel-core` gains a dependency on `jena-core`, but not on Jena's HTTP client, so NFR-Q-01 still holds.
- Serialising RDF (Jena RIOT) happens in the adapters that render, never in the core.
- Exposing only the graph level keeps the ports small and avoids leaking the model level's implicit state, such as prefix maps and reification, into the domain.
