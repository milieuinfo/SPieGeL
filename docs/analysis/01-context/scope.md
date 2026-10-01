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
- **Edge concerns that belong to the reverse proxy**, such as TLS termination and HSTS. Which edge concerns move into SPieGeL is an [open question](../06-open-questions.md).

## Phasing

The requirements are prioritised with MoSCoW (see [Traceability](../03-requirements/traceability.md)). The phases follow from that.

### Phase 1: basic service

Enough to publish a single public catalogue (for example a catalogue of simulation models) from a single SPARQL data source:

- configurable data sources (SPARQL first);
- configurable URI templates, each bound to its own query;
- content negotiation over the common RDF serialisations and HTML;
- simple HTML subject pages;
- a configurable front page.

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

- relational data sources (PostgreSQL, Trino) via R2RML;
- full-text search across data sources;
- assisted query building in the web interface.

!!! note
    Phase 1 alone does **not** replace the predecessor. Switching off the predecessor requires phase 2.
