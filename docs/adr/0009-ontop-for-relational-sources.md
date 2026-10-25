# 0009. Translate relational sources with Ontop, materialise other sources with RML

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

FR-DS-02 asks that non-RDF sources can be published through declarative mappings: relational databases (for example PostgreSQL and Trino), and files or Web APIs in CSV, JSON or XML. The analysis so far assumed that SPieGeL would read them through an RML adapter (`spiegel-adapter-rml`, [ADR 0004](0004-technology-baseline.md)) behind its data source port, and drew an "RML data source" in the [hexagonal design](../analysis/05-architecture/hexagonal-design.md).

That adapter does not fit the tools. [Open question 28](../analysis/06-open-questions.md#architecture-and-operations) compared them:

- Every RML processor we looked at (RMLMapper, RMLStreamer, CARML, Morph-KGC; October 2026) **materialises**: it generates RDF from the whole source in a separate step. None rewrites a SPARQL query against the source per request. Calling one from inside SPieGeL while a request is handled would mean materialising on every request, or building a cache that is in effect a store.
- **Translation per request** (a virtual knowledge graph) exists mainly for relational databases with [R2RML](https://www.w3.org/TR/r2rml/). [Ontop](https://ontop-vkg.org/) (Java, Apache 2.0) rewrites each SPARQL query into SQL against the database, so the data is always current. It supports PostgreSQL and Trino, among others, and **exposes its own SPARQL endpoint**.

Because Ontop speaks SPARQL, SPieGeL's existing data source port (FR-DS-01) can reach it without a new adapter. Ontop can also be embedded as a Java library, but it is built on RDF4J, while SPieGeL's core and adapters use Jena ([ADR 0004](0004-technology-baseline.md), [open question 14](../analysis/06-open-questions.md#architecture-and-operations)).

Other forces:

- Access levels rest on separate store accounts per level (FR-AC-03, NFR-SEC-02). A relational source has its own accounts, views and, in PostgreSQL, row-level security.
- `/sparql` is a guarded pass-through to a store, not domain logic. The [request flow](../analysis/05-architecture/request-flow.md#sparql) sends it to "the tenant's data source", which becomes ambiguous once a tenant has several.
- Whether the loading chain is in scope is still [open question 15](../analysis/06-open-questions.md#architecture-and-operations).

## Decision

1. **Relational sources that must be current are translated per request by Ontop**, with R2RML mappings. Ontop runs as a **separate service with its own SPARQL endpoint**. SPieGeL configures it as a named data source (FR-DS-01) and reads it through its SPARQL adapter, like a triple store. Ontop is not embedded in SPieGeL.
2. **RML is used for materialisation only, in the loading chain.** CSV, JSON, XML and Web API sources, and relational sources that may be loaded periodically, are converted by an RML processor into RDF that is loaded into a triple store. SPieGeL never runs an RML processor.
3. **There is no `spiegel-adapter-rml` module.** Non-RDF sources need no adapter of their own in SPieGeL. This reverses the renaming amended into ADR 0004 on 2026-10-05.
4. **Access levels for an Ontop source follow the same rule as for a store:** one named data source per level, each with its own database account. That account can only read what the level may see, for example through views or row-level security.
5. **Each tenant names one data source for `/sparql`**, by default its triple store. A tenant whose data is only relational can name its Ontop source. SPieGeL does not federate queries over several sources.

## Consequences

- The core and the data source port do not change. A relational source is a configuration matter: an Ontop service, a mapping and a named data source.
- SPieGeL keeps one RDF library (Jena). Ontop's RDF4J dependency stays inside the Ontop service.
- Data from a translated source is always current. In exchange, every SPieGeL query becomes SQL against the source database. The bounded pool and timeout per data source (NFR-OPS-02) protect the database, and the cache (NFR-OPS-03) limits repeated queries.
- Running Ontop is an extra service per relational source, or per access level of one. It has to be deployed, monitored and kept up to date.
- SPieGeL's queries (FR-RA-02) must stay within the SPARQL that Ontop supports. A prototype must confirm, before a tenant depends on it, how far Ontop supports named graphs (from `rr:graphMap`), property paths for hierarchies (FR-HTML-05) and `DESCRIBE`. Where it does not, the tenant materialises instead.
- Keyword search (FR-SRCH-01) uses the store's full-text index. An Ontop source has none, so a tenant that needs search on relational data materialises it, or names a store for search.
- A query on `/sparql` sees one source only. Combining a store with an Ontop source in one query needs `SERVICE`, where the store allows it, or materialisation.
- RML mappings, processors and their schedule belong to the loading chain. Their choice depends on open question 15, not on SPieGeL.
- Existing R2RML mappings can be used by Ontop as they are. Moving a source from translation to materialisation needs an RML processor that reads R2RML, which RMLMapper and Morph-KGC do.
