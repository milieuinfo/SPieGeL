# Functional requirements

Requirements say **what** users and client applications need, not how the predecessor does it. SPieGeL is not a copy of the predecessor: the predecessor and the [existing tools](../04-landscape/comparison.md) are sources of inspiration for something new and better.

Each requirement has a stable identifier, a priority and a phase (see [Scope](../01-context/scope.md#phasing)). The **priority** reflects what is needed to replace the predecessor with a better service and to meet the requirements of the [X-Cite](../01-context/background.md#x-cite) project. **Source** says where a requirement comes from: a function of the predecessor worth keeping (*P*), a gap found in the analysis (*G*), the X-Cite requirements (*C*, for catalogue), or an idea from the landscape reviews (*L*).

## URI policy

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-URI-01 | Each domain defines its own URI structure: which URI patterns identify which kinds of resources (for example `/doc/{concept}/{id}`). The patterns are configuration, not code. | M | 1 | P, C |
| FR-URI-02 | A thing and the document that describes it have different URIs, as the Flemish URI standard requires. Requesting the thing's `id` URI redirects with `303 See Other` to its `doc` URI, whatever format is asked for. | M | 2 | P, G, L |
| FR-URI-03 | Every term of a domain's vocabularies (classes, properties, shapes) can be looked up at its `ns` URI, in HTML and RDF. The HTML page links straight to the term. | M | 2 | P |
| FR-URI-04 | Blank nodes published as Skolem IRIs (`/.well-known/genid/{id}`) can be looked up like any other resource, so that every node in the published data is dereferenceable. | M | 2 | G |
| FR-URI-05 | Legislative resources can be published under an ELI-conformant `/eli/…` path structure, alongside the generic scheme. | S | 2 | G |
| FR-URI-06 | An unknown resource returns `404`. A known resource the caller may not see returns `404` as well, so that its existence is not disclosed. | M | 1 | — |

## Resource assembly

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-RA-01 | What describes a resource is configured per kind of resource: one or more queries, possibly on different data sources, whose results form one description. | M | 1 | P, C |
| FR-RA-02 | Queries are data (files under version control), not code. Adding a resource type needs no code change. | M | 1 | C |
| FR-RA-03 | A resource can have several views (profiles) with different content, for example a summary and a complete description. Which queries make up each view is configuration. The conditions are typed and covered by tests. | M | 2 | P, G, L |
| FR-RA-04 | Nested structures without their own identity (blank nodes, such as an address or a measurement with its unit) are shown as part of the resource that holds them, to a configurable depth. | M | 1 | P |
| FR-RA-05 | Vocabulary pages show class expressions (`owl:unionOf`, `owl:intersectionOf`) as readable lists of classes, not as raw RDF lists. | S | 2 | P |

## Content negotiation

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-CN-01 | Every `doc` and `ns` resource is available as HTML for people and as Turtle, RDF/XML, N-Triples and JSON-LD for machines, from the same URI, selected with the `Accept` header. | M | 1 | P, C |
| FR-CN-02 | Every representation also has its own URL (for example `/doc/x.ttl` or `?_format=ttl`), so that it can be linked, bookmarked and downloaded without setting headers. | S | 1 | L |
| FR-CN-03 | Clients can ask for a view (profile) per [W3C Content Negotiation by Profile](https://www.w3.org/TR/dx-prof-conneg/), for example a light view for a web page and a complete view for a machine. | M | 2 | P, G, C, L |
| FR-CN-04 | Every response tells the client which other formats and profiles exist, and which profile it got (`Link` headers). | S | 1 | L |
| FR-CN-05 | An unsupported media type returns `406 Not Acceptable`. | M | 1 | — |

## HTML

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-HTML-01 | Every resource has a readable HTML page without any configuration: its label as the title, its properties with linked resources shown by their labels, and links to its other formats. | M | 1 | C |
| FR-HTML-02 | A front page per domain, served at `/` on the domain's own host name (for example `https://data.imjv.omgeving.vlaanderen.be/`). It is **generated, not maintained by hand**: it shows the content blocks discovered in the domain's data (FR-HTML-09), and optionally a configured introduction, example queries and search terms. | M | 1 | P, C, G |
| FR-HTML-03 | A domain can give a kind of resource, or a URI pattern, its own page layout. | S | 2 | C |
| FR-HTML-04 | Every HTML page also carries its data in machine-readable form: the subject IRI in the markup, links to the RDF representations (`<link rel="alternate">`), and the description embedded as JSON-LD. Scripts, browser extensions and search engines can then use the data without reading the visible text. | S | 1 | G, L |
| FR-HTML-05 | Users can explore beyond one resource: which resources refer to it (with a count per relation), the hierarchy of a thesaurus, the structure of a catalogue, and the members of a collection. Long lists are paged and searchable. | M | 2 | P |
| FR-HTML-06 | What a page shows adapts to the data: for example a map for a geometry, a tree for a concept scheme, a table for a collection, and measurements with their units. The rules look at the resource's type, the predicate, the predicate's own type and the value's datatype. They are configurable, with shared defaults that tenants can extend. Background: [Data-dependent HTML rendering](../02-current-platform/html-rendering.md). | S | 2 | P, G |
| FR-HTML-07 | Resources with a location or geometry show it on a map, whatever the geometry type (point, line, polygon) and coordinate reference system: WKT literals with their CRS, and WGS84 or Belgian Lambert coordinate pairs. | S | 2 | P, G |
| FR-HTML-08 | A **portal page** on the umbrella domain's host name (`algemeen`, `https://data.omgeving.vlaanderen.be/`) lists every published domain, with its name, a short description and a link to its own front page (FR-HTML-02). The list is **discovered**, not maintained by hand. It comes from the tenant configuration, from the actual publication (for example a description of each domain in the umbrella domain's open data catalogue), or from both. The source is configurable. Domains published by other services (for example the newer record-page domains, see [Data domains](../02-current-platform/data-domains.md#newer-domains)) can be added as configured entries. A new domain appears without editing the portal (FR-MT-02). | S | 2 | G |
| FR-HTML-09 | **Discovered content blocks** on a domain's front page. Each block is a query, declared in configuration, with shared defaults that a tenant can reorder, disable, override or extend. The default blocks are:<br>• **DCAT catalogues** with their datasets (`dcat:Catalog`, `dcat:dataset`);<br>• **thesauri**: the SKOS concept schemes (`skos:ConceptScheme`);<br>• **classes**: the `rdfs:Class` and `owl:Class` resources, linking to their vocabulary pages (FR-URI-03).<br>Every entry links to its subject page and is labelled with its title or label. Empty blocks are not shown. Long blocks are limited and paged. Blocks run at the *public* access level and are cached like resource descriptions (NFR-OPS-03). | S | 1 | G |
| FR-HTML-10 | **Every IRI on a page is a link to that IRI, shown by its label where one exists.** The link points to the IRI itself, so that `id` URIs redirect to their `doc` page (FR-URI-02) and terms in a domain's vocabularies open their `ns` page (FR-URI-03). What is shown as the link text depends on the position:<br>• **properties** (and types): the label from the data or the vocabulary (`rdfs:label`, `skos:prefLabel`), or from the label sources of FR-HTML-11; without one, a prefixed name if a prefix for its namespace is known (from the configuration or the data); otherwise the full IRI;<br>• **objects that are resources** (IRI values): preferably the resource's label (`rdfs:label`, `skos:prefLabel`, `dct:title`), if it is present in the store; without one, the full IRI;<br>• objects that are literals are shown as values, with their language or datatype; blank nodes are shown nested (FR-RA-04).<br>**Language:** when a resource or property has labels in several languages, one label is chosen by a language priority list configured per tenant, for example `nl`, then `en`, then a label without language tag, then any other language. The page title follows the same list. The link text is never empty. | M | 1 | P |
| FR-HTML-11 | **Labels of external vocabularies are available.** Pages also show readable labels for terms of external vocabularies (for example DCTERMS, SKOS, DCAT, schema.org). The labels come from configured label sources, consulted in order: the domain's own data source; a shared vocabulary source for all tenants (a data source, or vocabulary files in the configuration); otherwise the fallback of FR-HTML-10: a prefixed name if the prefix is known, else the full IRI. Vocabularies are never fetched from the web while a request is handled (NFR-SEC-06). Which sources are the default is [open question 26](../06-open-questions.md#architecture-and-operations). | S | 1 | G, L |
| FR-HTML-12 | **Multilingual HTML.** A visitor can read HTML pages in one of the languages a domain offers. The choice applies both to the data (which label is chosen first, FR-HTML-10) and to the interface texts (block titles, buttons, messages, the front page). It is made from the URL (`?lang=en`, so that a page in a given language can be linked and cached), then from the browser's `Accept-Language`, then from the tenant's default. Each domain configures which languages it offers. Interface texts live in translation files, not in templates. If a label is missing in the chosen language, the tenant's priority list applies. RDF representations are not affected: they always contain all languages. | S | 2 | G, L |

## SPARQL endpoint

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-SPARQL-01 | Each domain offers a public, read-only SPARQL endpoint at `/sparql` that implements the SPARQL 1.1 Protocol for queries (GET, form-encoded POST and direct POST). | M | 2 | P |
| FR-SPARQL-02 | The endpoint returns the standard result formats, chosen by content negotiation: SPARQL JSON, XML, CSV and TSV for `SELECT` and `ASK`, and the RDF formats of FR-CN-01 for `CONSTRUCT` and `DESCRIBE`. | M | 2 | P, G |
| FR-SPARQL-03 | People can write, run and share queries in the browser: an editor, results as a table with clickable IRIs, and a URL per query. | S | 2 | P, L |
| FR-SPARQL-04 | Each domain offers example queries to start from, including a default query in the editor. | C | 2 | P |

## Search

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-SRCH-01 | Users can find a domain's resources by keyword in their labels, titles and descriptions, with results linking to the subject pages. The data source's own full-text index is used, behind a port. | S | 2 | P |
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
| FR-AC-01 | Callers authenticate with OpenID Connect (people) or client credentials (systems). An unauthenticated client has the *public* level, the lowest. | M | 2 | P, C |
| FR-AC-02 | Access levels form an explicit, ordered type. A caller's level is the highest level granted by their roles. | M | 2 | P |
| FR-AC-03 | Each access level can map to separate, read-only credentials per data source. | M | 2 | P |

## Possibly retired

These features exist in the predecessor but are not routed, or their use is unknown. They are **not** requirements unless an [open question](../06-open-questions.md) settles otherwise.

- OpenRefine-compatible reconciliation service.
- Linked Data Fragments interface.
- Data dump per domain (a placeholder in the predecessor).
