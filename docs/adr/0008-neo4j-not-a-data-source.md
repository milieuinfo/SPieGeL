# 0008. Do not use Neo4j as a data source for SPieGeL

- **Status:** Proposed
- **Date:** 2026-10-07

## Context

Neo4j is a property graph database, queried with Cypher. It is strong at traversals and graph algorithms, and has its own full-text and vector indexes. It was raised as a possible store for some of the department's use cases. The question is whether SPieGeL should be able to read from it. The full analysis is [open question 33](../analysis/06-open-questions.md#architecture-and-operations).

SPieGeL is built on SPARQL. Data sources are SPARQL endpoints (FR-DS-01), queries are `.rq` files under version control (FR-RA-02), and every domain offers a public SPARQL 1.1 endpoint (FR-SPARQL-01, FR-SPARQL-02). Neo4j has no SPARQL. A tenant on Neo4j would therefore still need a triple store for `/sparql`, and would maintain its queries in two languages. The data source port also assumes RDF results (open question 14), so a Cypher adapter would have to translate property graph results back to RDF.

RDF reaches Neo4j through the [neosemantics](https://neo4j.com/labs/neosemantics/) plugin (n10s), a Neo4j Labs project that is not a supported product and does not run on the Aura cloud service. By default it keeps triples but not their named graph. Language tags, datatypes, blank nodes and Skolem IRIs, which SPieGeL relies on (FR-HTML-10, FR-URI-04, FR-RA-04), have to be imitated in the property graph.

SPieGeL's access levels rest on named graphs: each level's store account can only read that level's graphs (NFR-SEC-02), and access is restricted per triple ([open question 29](../analysis/06-open-questions.md#architecture-and-operations)). Neo4j's fine-grained permissions apply to labels, relationship types and properties, and as far as we know only in the commercial Enterprise edition. Using them would mean a new security design.

Neo4j's strengths add little to SPieGeL's own task. Keyword search uses the data source's full-text index (FR-SRCH-01), hierarchies and collections work with SPARQL property paths (FR-HTML-05), and a description is bounded to its subject (FR-RA-06), so deep traversals are not needed.

## Decision

1. **Neo4j is not a data source for SPieGeL.** SPieGeL gets no Cypher adapter behind its data source port, and tenants keep their data in SPARQL stores.
2. **Neo4j may be used beside SPieGeL**, for use cases where a property graph is stronger than SPARQL, such as network analysis, graph data science, visual exploration or GraphRAG. In that case:
    - the **loading chain** builds a Neo4j database from an explicit allowlist of public graphs, as a derived artefact like an HDT file or a QLever index (option 4 of open question 29); or
    - a **source system** that already keeps its data in Neo4j exports it to RDF, which is loaded into the triple store like the materialised sources of open question 28.
3. **Nodes keep the things' URIs**, so that an application built on Neo4j links back to SPieGeL's subject pages ([ADR 0007](0007-uri-as-shared-identifier.md)).

## Consequences

- SPieGeL keeps one query language, one result model and one security model. No round trip RDF → Neo4j → RDF has to be proven lossless or kept under test.
- Every tenant needs a SPARQL store. Data that lives only in Neo4j must be exported to RDF before SPieGeL can publish it, so it is not live.
- A Neo4j database built by the loading chain holds public data only. The security boundary stays in the loading chain, and Neo4j's permissions do not have to replicate the access levels.
- Building and running such a database is a matter for the loading chain (open question 15) and for the consumer application ([ADR 0006](0006-resources-and-event-streams.md)), not for SPieGeL.
- Which use cases actually need Neo4j remains open (open question 33). If one ever requires SPieGeL to read live data from Neo4j, a new ADR must supersede this one.
