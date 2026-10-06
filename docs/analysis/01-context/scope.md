# Scope

## In scope

SPieGeL is a **read-only Linked Data publishing server**. It:

- dereferences URIs according to a URI policy (`id` / `doc` / `ns`, plus Skolem IRIs and ELI paths);
- assembles the description of a resource from one or more queries on one or more data sources;
- serialises that description in the representation the client negotiates (HTML, Turtle, RDF/XML, N-Triples, JSON-LD), optionally per profile;
- exposes a public, read-only SPARQL endpoint;
- serves several data domains (tenants) from one stateless deployment;
- enforces access levels on non-public data.

## Out of scope

- **Writing data.** SPieGeL never modifies a data source. Loading and transforming data (ETL) is a separate concern.
- **Managing vocabularies.** Ontologies, concept schemes, SHACL shapes and catalogues are maintained elsewhere and loaded into the data sources. SPieGeL publishes them.
- **Serving event streams.** Linked Data Event Streams publish the same resources and are served by an LDES server, such as OpenLDES's. SPieGeL makes them discoverable ([ADR 0006](../../adr/0006-resources-and-event-streams.md)).
- **Integrating data across domains.** Consumers combine resources themselves, by dereferencing, by replicating event streams or with SPARQL ([ADR 0006](../../adr/0006-resources-and-event-streams.md)).
- **Other distributions of the data**, such as OGC API Features or WFS layers and downloads. They carry the things' URIs, so that SPieGeL's subject pages serve as their landing pages ([ADR 0007](../../adr/0007-uri-as-shared-identifier.md)).
- **Edge concerns that belong to the reverse proxy**, such as TLS termination and HSTS. Which edge concerns move into SPieGeL is an [open question](../06-open-questions.md).

## Phasing

The requirements are prioritised with MoSCoW (see [Traceability](../03-requirements/traceability.md)). The phases follow from that.

### Phase 1: basic service

Enough to publish a single public catalogue (for example a catalogue of simulation models) from a single SPARQL data source:

- configurable data sources (SPARQL first);
- configurable URI templates, each bound to its own query;
- content negotiation over the common RDF serialisations and HTML;
- simple HTML subject pages that also carry their data as embedded JSON-LD;
- a front page generated from the data it publishes (catalogues, thesauri, classes).

No authentication is needed in this phase, because the catalogue is public.

### Phase 2: successor to the predecessor

Everything needed to switch off the predecessor:

- several domains (tenants) in one deployment, each selected by host name;
- the full URI policy, including `ns` pages, Skolem IRIs and ELI paths;
- a public SPARQL endpoint compatible with the existing browser components;
- content negotiation by profile;
- context-specific HTML templates per URI template;
- authentication (OIDC) and access levels.

### Later

- non-RDF data sources (relational databases such as PostgreSQL and Trino, and CSV, JSON or XML files and Web APIs) via RML mappings;
- full-text search across data sources;
- assisted query building in the web interface.

!!! note
    Phase 1 alone does **not** replace the predecessor. Switching off the predecessor requires phase 2.
