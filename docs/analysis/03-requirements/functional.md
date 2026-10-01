# Functional requirements

Each requirement has a stable identifier, a priority and a phase (see [Scope](../01-context/scope.md#phasing)). The **priority** reflects what is needed to replace the predecessor. **Source** says where the requirement comes from: the predecessor's behaviour (*P*), the analysis of its gaps (*G*), or the model catalogue's needs (*C*).

## URI policy

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-URI-01 | Requests are matched against configurable URI templates (for example `/doc/{concept}/{id}`). | M | 1 | P, C |
| FR-URI-02 | A request for an `id` URI is answered with `303 See Other` to the matching `doc` URI, for every media type. | M | 2 | P, G |
| FR-URI-03 | `ns` URIs serve vocabulary pages for the models of a domain. | M | 2 | P |
| FR-URI-04 | Skolem IRIs under `/.well-known/genid/{id}` are dereferenceable per domain. | M | 2 | G |
| FR-URI-05 | Legislative resources can be published under an ELI-conformant `/eli/…` path structure, alongside the generic scheme. | S | 2 | G |
| FR-URI-06 | An unknown resource returns `404`. A known resource the caller may not see returns `404` as well, so that its existence is not disclosed. | M | 1 | — |

## Resource assembly

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-RA-01 | Each URI template is bound to one or more queries on a named data source. The results are merged into one description. | M | 1 | P, C |
| FR-RA-02 | Queries are data (files under version control), not code. Adding a resource type needs no code change. | M | 1 | C |
| FR-RA-03 | Which queries run can depend on the resource type and the negotiated profile. These conditions are typed and covered by tests. | M | 2 | P, G |
| FR-RA-04 | Blank-node structures attached to a resource are included in its description, to a configurable depth. | M | 1 | P |
| FR-RA-05 | `owl:unionOf` and `owl:intersectionOf` lists are expanded on vocabulary pages. | S | 2 | P |

## Content negotiation

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-CN-01 | `doc` and `ns` resources are available as HTML, Turtle, RDF/XML, N-Triples and JSON-LD, selected with the `Accept` header. | M | 1 | P, C |
| FR-CN-02 | A query parameter can override the `Accept` header, so formats can be linked from HTML. | S | 1 | — |
| FR-CN-03 | Profiles can be negotiated per [W3C Content Negotiation by Profile](https://www.w3.org/TR/dx-prof-conneg/), for example a restricted view of incoming relations in HTML and a full view in machine formats. | M | 2 | P, G, C |
| FR-CN-04 | Responses carry `Link` headers to their alternate representations. | S | 1 | — |
| FR-CN-05 | An unsupported media type returns `406 Not Acceptable`. | M | 1 | — |

## HTML

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-HTML-01 | Simple, generic HTML subject pages for any resource. | M | 1 | C |
| FR-HTML-02 | A configurable front page per domain, with example queries and search terms. | M | 1 | P, C |
| FR-HTML-03 | HTML templates can be chosen per URI template or resource type. | S | 2 | C |
| FR-HTML-04 | HTML embeds the subject IRI on an element with class `ld-subject` and an `about` attribute (compatibility with existing [client components](../02-current-platform/client-components.md)). | S | 2 | P |
| FR-HTML-05 | Incoming relations, SKOS hierarchies, DCAT catalogues and collection tables can be browsed, whether in the browser or rendered on the server. | M | 2 | P |

## SPARQL endpoint

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-SPARQL-01 | A public, read-only SPARQL endpoint per domain at `/sparql` (GET and POST, SPARQL 1.1 Protocol). | M | 2 | P |
| FR-SPARQL-02 | `POST` with a `query` form field returns `application/sparql-results+json` (compatibility with the client components). | M | 2 | P |
| FR-SPARQL-03 | Results can be shown as HTML in a browser. | S | 2 | P |
| FR-SPARQL-04 | A per-domain default query is used when none is given. | C | 2 | P |

## Search

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-SRCH-01 | Keyword search per domain, using the data source's full-text capability behind a port. | S | 2 | P |
| FR-SRCH-02 | Full-text search across all data sources. | W | — | C |

## Multi-tenancy

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-MT-01 | One deployment serves several domains, each selected by host name, with its own data sources, URI templates and HTML. | M | 2 | P, C |
| FR-MT-02 | Adding a domain needs configuration only. | M | 2 | P |

## Data sources

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-DS-01 | One or more SPARQL endpoints can be configured as named data sources. | M | 1 | P, C |
| FR-DS-02 | Relational sources (PostgreSQL, Trino) via R2RML. | C | later | C |
| FR-DS-03 | Elasticsearch as a data source. | W | — | C |

## Access control

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-AC-01 | Callers authenticate with OpenID Connect (people) or client credentials (systems). Anonymous access gives the *public* level. | M | 2 | P, C |
| FR-AC-02 | Access levels form an explicit, ordered type. A caller's level is the highest level granted by their roles. | M | 2 | P |
| FR-AC-03 | Each access level can map to separate, read-only credentials per data source. | M | 2 | P |

## Possibly retired

These features exist in the predecessor but are not routed, or their use is unknown. They are **not** requirements unless an [open question](../06-open-questions.md) settles otherwise.

- OpenRefine-compatible reconciliation service.
- Linked Data Fragments interface.
- Data dump per domain (a placeholder in the predecessor).
