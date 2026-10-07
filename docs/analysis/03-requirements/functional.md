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
| FR-URI-07 | The RDF of a document links it to the thing it describes, and the thing to its document: `foaf:primaryTopic` from the `doc` URI to the `id` URI, and `foaf:page` back, as [Cool URIs §4.6](https://www.w3.org/TR/cooluris/#linking) recommends. A consumer that merges the triples into a larger graph still knows where they came from. SPieGeL adds these links itself, so no query has to. Vocabulary terms keep their own `rdfs:isDefinedBy`, which points to the ontology that defines them. | S | 1 | G |
| FR-URI-08 | **A moved resource redirects permanently.** When a thing gets a new URI, the old URI answers `301 Moved Permanently` to the new one, for every format. The `id` and `doc` URIs each redirect to their own counterpart, so the old `id` URI leads through `301` to the new `id` URI and then through `303` to the new `doc` URI (FR-URI-02). Two merged duplicates are a move too: the old URI points to the one that remains. The new description links back to the old URI (`dct:replaces`, or `owl:sameAs` where both denote exactly the same thing). | M | 2 | G |
| FR-URI-09 | **A withdrawn or ended resource is still described.** A thing that existed but is no longer valid, such as a dissolved organisation, a deprecated concept or a withdrawn designation, keeps its URI and its description, answered with `200`. The description shows its status (for example `owl:deprecated`, `adms:status`, an end date such as `prov:invalidatedAtTime` or the domain's own validity period) and its successor where there is one (`dct:isReplacedBy`). The HTML page shows the status at the top, with a link to the successor. | M | 2 | G |
| FR-URI-10 | **A deliberately removed description answers `410 Gone`.** When a description may no longer be published, for example because it was published in error or must be erased under the GDPR, its URI answers `410` with a short tombstone: in HTML, what happened and since when, without the old data. The URI is never reused. A non-public resource answers `404` instead, so that its existence is not disclosed (FR-URI-06). | S | 2 | G |
| FR-URI-11 | **Where moves and removals are recorded.** A move or removal of a single URI is **data** in the store, managed by the data owner without a deployment. It describes the fact (`dct:isReplacedBy`, a status, a tombstone), not the HTTP response. A move of a whole URI pattern or host name is **configuration**: a rewrite rule per URI template. SPieGeL only looks for a move or a tombstone when a description comes back empty, so ordinary requests need no extra query. `301` and `410` responses are cacheable, with a lifetime configured per tenant (NFR-OPS-03). A move, withdrawal or removal is also published as an event in the domain's event stream, if it has one (FR-ES-01). | S | 2 | G, L |

## Resource assembly

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-RA-01 | What describes a resource is configured per kind of resource: one or more queries, possibly on different data sources, whose results form one description. | M | 1 | P, C |
| FR-RA-02 | Queries are data (files under version control), not code. Adding a resource type needs no code change. | M | 1 | C |
| FR-RA-03 | A resource can have several views (profiles) with different content, for example a summary and a complete description. Which queries make up each view is configuration. The conditions are typed and covered by tests. | M | 2 | P, G, L |
| FR-RA-04 | Nested structures without their own identity (blank nodes, such as an address or a measurement with its unit) are shown as part of the resource that holds them, to a configurable depth. | M | 1 | P |
| FR-RA-05 | Vocabulary pages show class expressions (`owl:unionOf`, `owl:intersectionOf`) as readable lists of classes, not as raw RDF lists. | S | 2 | P |
| FR-RA-06 | **A description is bounded to its subject.** It holds the subject's own properties. An object that is itself a resource appears by its IRI only, because it is described at its own URI or in its own event stream. Two exceptions are allowed: the label and type of a linked resource, for presentation (FR-HTML-10), and values without an identity of their own, such as a measurement with its unit (FR-RA-04). Incoming relations are not part of a description. See [ADR 0006](../../adr/0006-resources-and-event-streams.md). | M | 1 | G |
| FR-RA-07 | **Large properties are paged.** Per kind of resource, the configuration declares which properties can have many values, for example `skos:member` on a collection, `dcat:dataset` on a catalogue or `org:hasMember` on an organisation. A description holds the first *N* values of such a property and a link to a **paged list** of all its values, with its own URI. Each page says in RDF which list it belongs to and links to the next page, in a paging vocabulary ([open question 35](../06-open-questions.md#architecture-and-operations)). Each property is queried with `LIMIT N+1`, so that SPieGeL knows there are more values without counting them on every request. HTML exploration (FR-HTML-05) uses the same pages, so HTML and RDF follow one rule. A description is not a bulk channel: a client that needs everything uses SPARQL, a dump or an event stream (FR-ES-01). | M | 1 | G |

## Content negotiation

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-CN-01 | Every `doc` and `ns` resource is available as HTML for people and as Turtle, RDF/XML, N-Triples and JSON-LD for machines, from the same URI, selected with the `Accept` header. | M | 1 | P, C |
| FR-CN-02 | Every representation also has its own URL (for example `/doc/x.ttl` or `?_format=ttl`), so that it can be linked, bookmarked and downloaded without setting headers. | S | 1 | L |
| FR-CN-03 | Clients can ask for a view (profile) per [W3C Content Negotiation by Profile](https://www.w3.org/TR/dx-prof-conneg/), for example a light view for a web page and a complete view for a machine. | M | 2 | P, G, C, L |
| FR-CN-04 | Every response tells the client which other formats and profiles exist, and which profile it got (`Link` headers). | S | 1 | L |
| FR-CN-05 | An unsupported media type returns `406 Not Acceptable`. | M | 1 | — |
| FR-CN-06 | A response whose content depends on request headers names those headers in `Vary` (`Accept`, and `Accept-Profile` and `Accept-Language` where they apply). Otherwise a shared cache can serve Turtle to a browser, or HTML to a harvester. A response at a format-specific URL (FR-CN-02) does not vary on `Accept`. | M | 1 | G |
| FR-CN-07 | A negotiated response names the format-specific URL of the variant it returned in `Content-Location`, for example `/doc/x.ttl`, as [Cool URIs §2.1](https://www.w3.org/TR/cooluris/#conneg) recommends. | S | 1 | G |
| FR-CN-08 | Negotiation weighs the client's quality values (`q`) against SPieGeL's own quality per format, as [Cool URIs §4.7](https://www.w3.org/TR/cooluris/#choosing) describes. A request without `Accept`, or with only `*/*`, gets HTML. | M | 1 | G |
| FR-CN-09 | A `HEAD` request returns the same status and headers as `GET` on the same URL, including `Content-Length` if `GET` sends it ([RFC 9110 §9.3.2](https://www.rfc-editor.org/rfc/rfc9110#section-9.3.2)). Link checkers and monitoring tools rely on it. | S | 1 | G |

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
| FR-HTML-13 | **Links to other distributions of the same thing.** A subject page can link to the thing in other forms in which it is published, for example its feature in an OGC API Features collection or WFS layer, or a map viewer centred on it. The links are configured per kind of resource, as templates filled from the URI's variables or from the data. Users who arrive from a map viewer through the thing's URI can go back the same way ([ADR 0007](../../adr/0007-uri-as-shared-identifier.md)). | S | 2 | G |
| FR-HTML-14 | **Images.** When a value is a picture (for example `foaf:depiction`, `schema:image` or `foaf:img`), the page shows the image, not only a link. The image is a resource with its own URI, like any other value (FR-HTML-10). The `<img>` element, its source and its alternative text (from the image's label or description) are written on the server, so that the image shows without JavaScript (NFR-UI-04). Which rendition is shown, for example a thumbnail that links to the full image, is configured per tenant as a URL template on the image's URI. The hosts that serve images are part of the tenant configuration and are allowed in the page's Content Security Policy (NFR-UI-03). Example: the [heritage inventory](design-system.md#a-subject-page-with-images-the-heritage-inventory). | S | 2 | G |
| FR-HTML-15 | **The structure of a domain's data.** A domain's front page (FR-HTML-02) gives a general picture of what its data looks like: which kinds of resources it holds, with which properties, value types and counts. The picture comes from two sources, shown together:<br>• **declared structure**: the SHACL shapes and Data Cube structure definitions (`qb:DataStructureDefinition`, with their dimensions, measures and attributes) that a domain defines in advance;<br>• **derived structure**: a summary queried from the actual data, for example per named graph, in the form of SHACL shapes or VoID class and property partitions, for domains without declared shapes or to show how the data relates to them.<br>The structure is also available as RDF, as part of the dataset description (FR-META-01), so that clients, and applications with a language model ([open question 34](../06-open-questions.md#architecture-and-operations)), can use it as a schema. Deriving it is costly on a large store, so it is computed in the loading chain or cached like a front-page block (NFR-OPS-03), and only from graphs at the caller's access level. | C | 2 | G |

## SPARQL endpoint

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-SPARQL-01 | Each domain offers a public, read-only SPARQL endpoint at `/sparql` that implements the SPARQL 1.1 Protocol for queries (GET, form-encoded POST and direct POST). | M | 2 | P |
| FR-SPARQL-02 | The endpoint returns the standard result formats, chosen by content negotiation: SPARQL JSON, XML, CSV and TSV for `SELECT` and `ASK`, and the RDF formats of FR-CN-01 for `CONSTRUCT` and `DESCRIBE`. | M | 2 | P, G |
| FR-SPARQL-03 | People can write, run and share queries in the browser: an editor, results as a table with clickable IRIs, and a URL per query. The editor can insert the `PREFIX` lines for the prefixes the tenant configures. | S | 2 | P, L |
| FR-SPARQL-04 | **Example queries as data.** Each domain offers example queries to start from, including a default query in the editor. Each example is a SHACL `sh:SPARQLExecutable` (with `sh:select`, `sh:ask`, `sh:construct` or `sh:describe`), with a description in plain language (`rdfs:comment`, per language) and keywords, following the convention of the [SIB SPARQL examples](https://github.com/sib-swiss/sparql-examples). The examples are files under version control in the tenant configuration, like the queries of FR-RA-02. SPieGeL publishes them at `/.well-known/sparql-examples/`, lists them on the domain's front page and the `/sparql` page, and opens each one in the editor through its URL (FR-SPARQL-03). The build runs every example against the domain's store, so that an example that no longer works is noticed. Reference: [UniProt](../04-landscape/uniprot-reference.md). | S | 2 | P, L |

## Search

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-SRCH-01 | Users can find a domain's resources by keyword in their labels, titles and descriptions, with results linking to the subject pages. The data source's own full-text index is used, behind a port. | S | 2 | P |
| FR-SRCH-02 | Full-text search across all data sources. A nice to have for a later phase, kept open although the X-Cite requirements grade it *won't*. Results respect the caller's access level: an index that serves several levels must not reveal values through suggestions, highlighting or counts (see [open question 29](../06-open-questions.md#architecture-and-operations)). | C | later | C |

## Multi-tenancy

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-MT-01 | One deployment serves several domains, each selected by host name, with its own data sources, URI templates and HTML. | M | 2 | P, C |
| FR-MT-02 | Adding a domain needs configuration only. | M | 2 | P |

## Data sources

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-DS-01 | One or more SPARQL endpoints can be configured as named data sources. | M | 1 | P, C |
| FR-DS-02 | Non-RDF sources can be published through declarative mappings: relational databases (for example PostgreSQL and Trino), and files or Web APIs in CSV, JSON or XML. Mappings use [RML](https://rml.io/), which generalises R2RML; existing R2RML mappings remain usable. Whether the data is materialised into a store beforehand or translated per request is [open question 28](../06-open-questions.md#architecture-and-operations). | C | later | C |
| FR-DS-03 | Elasticsearch as a data source. | W | — | C |

## Dataset description and licence

SPieGeL reads DCAT catalogues from the data (FR-HTML-09), but must also describe each domain itself, so that consumers and catalogues can find out what a domain publishes, under which licence, and how to reach it. The [Data on the Web Best Practices](https://www.w3.org/TR/dwbp/) treat metadata and licence information as basic requirements.

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-META-01 | **A dataset description per domain**, in DCAT (DCAT-AP and the OSLO profile) and VoID, available as RDF by content negotiation and linked from the front page (FR-HTML-02). It is served at `/.well-known/void` on the domain's host name, as the VoID specification proposes. It covers title, description, publisher, contact point and licence (FR-META-02); the URI space (`void:uriSpace`) and example resources; the vocabularies used; the access points: the SPARQL endpoint (`void:sparqlEndpoint`, FR-META-03), dumps where they exist, and event streams (FR-ES-01); and the structure of the data as VoID class and property partitions (FR-HTML-15). Fixed facts come from the tenant configuration, the rest is derived from the data and cached. The umbrella catalogue can harvest these descriptions, which also lets the portal discover domains (FR-HTML-08). The description carries the version of the data (`pav:version` and its release date). Its key facts, such as the number of triples, the version, the named graphs, the query timeout and the licence, are also shown in HTML, on the front page and the `/sparql` page. Reference: [UniProt](../04-landscape/uniprot-reference.md). | S | 1 | G, L |
| FR-META-02 | **A licence a consumer can find.** Each domain declares its licence in its configuration, as an IRI (for example a Creative Commons licence or the Flemish open data licence). It appears as `dct:license` in the dataset description (FR-META-01), as a `Link: <…>; rel="license"` header on every response ([RFC 8288](https://www.rfc-editor.org/rfc/rfc8288)), and visibly on every HTML page. Where data at a higher access level comes with other terms of use, the licence can be set per access level. | M | 1 | G |
| FR-META-03 | **SPARQL Service Description.** A request to `/sparql` without a query returns a description of the endpoint in the [SPARQL 1.1 Service Description](https://www.w3.org/TR/sparql11-service-description/) vocabulary, by content negotiation: the endpoint, the supported language and result formats, limits such as the timeout, the features (for example whether the default graph is the union of all graphs), the named graphs, and the dataset description (FR-META-01) as its default dataset, including its VoID statistics per named graph: the number of triples, distinct subjects and objects, and class and property partitions (FR-HTML-15). `/.well-known/void` serves the same statistics. Reference: [UniProt](../04-landscape/uniprot-reference.md). | S | 2 | G, L |

## Event streams

Event streams ([LDES](https://semiceu.github.io/LinkedDataEventStreams/)) publish the same resources as SPieGeL, but as their history, and are served by an LDES server, not by SPieGeL ([ADR 0006](../../adr/0006-resources-and-event-streams.md)).

| ID | Requirement | Priority | Phase | Source |
|---|---|---|---|---|
| FR-ES-01 | A domain's event streams can be discovered from what SPieGeL publishes: from the domain's front page and its dataset description, and from a resource to the stream that publishes it. How a resource points to its stream is [open question 30](../06-open-questions.md#architecture-and-operations). | S | 2 | G |

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
- Data dump per domain (a placeholder in the predecessor). Replicating a domain's event streams covers bulk access ([ADR 0006](../../adr/0006-resources-and-event-streams.md)).
