# Standards

SPieGeL implements or follows these standards. Where the predecessor deviates from one, that is noted in [Current platform](../02-current-platform/uri-patterns.md#known-gaps).

| Standard | Used for | Requirements |
|---|---|---|
| Flemish URI standard for data (Informatie Vlaanderen, v1.0, 2017) | `id` / `doc` / `ns` URI shape, `303` redirect | FR-URI-01 to 03 |
| [European Legislation Identifier (ELI)](https://eur-lex.europa.eu/eli-register/about.html), including the *ELI Technical Implementation Guide* | `/eli/…` paths for legislative resources | FR-URI-05 |
| [RDF 1.1 Concepts §3.5: Skolemisation](https://www.w3.org/TR/rdf11-concepts/#section-skolemization) | `/.well-known/genid/` IRIs | FR-URI-04 |
| [Cool URIs for the Semantic Web](https://www.w3.org/TR/cooluris/) | `303` pattern, content negotiation, `Content-Location`, links between a thing and its document | FR-URI-02, FR-URI-07, FR-CN-01, FR-CN-07, FR-CN-08 |
| [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110) | Content negotiation, `301`, `303`, `404`, `406`, `410`, `Vary`, `HEAD` | FR-CN-01, FR-CN-05, FR-CN-06, FR-CN-09, FR-URI-08, FR-URI-10 |
| [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457) | Error responses for machines | FR-CN-10 |
| [JSON-LD 1.1](https://www.w3.org/TR/json-ld11/) and [JSON-LD 1.1 Framing](https://www.w3.org/TR/json-ld11-framing/) | Framed JSON-LD with a context per domain, also as `application/json` | FR-CN-01, FR-CN-11 |
| [Content Negotiation by Profile](https://www.w3.org/TR/dx-prof-conneg/) (W3C draft) | Profile negotiation | FR-CN-03 |
| [RFC 8288: Web Linking](https://www.rfc-editor.org/rfc/rfc8288) | `Link` headers to alternates | FR-CN-04 |
| [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/) and [Query Results JSON](https://www.w3.org/TR/sparql11-results-json/) | `/sparql` endpoint | FR-SPARQL-01, 02 |
| [RML](https://kg-construct.github.io/rml-resources/portal/) (W3C Knowledge Graph Construction Community Group) and [R2RML](https://www.w3.org/TR/r2rml/) | R2RML: relational sources translated per request by Ontop. RML: other sources (CSV, JSON, XML, Web APIs) materialised in the loading chain ([ADR 0009](../../adr/0009-ontop-for-relational-sources.md)) | FR-DS-02 |
| [Linked Data Event Streams](https://semiceu.github.io/LinkedDataEventStreams/) (SEMIC) | Event streams that publish the same resources; discovery from SPieGeL | FR-ES-01 |
| [Spatial Data on the Web Best Practices](https://www.w3.org/TR/sdw-bp/) (W3C and OGC) | HTTP URIs for spatial things in every distribution, links between distributions | FR-URI-02, FR-HTML-13 |
| [DCAT-AP](https://interoperable-europe.ec.europa.eu/collection/semic-support-centre/dcat-ap) and the Flemish [OSLO](https://data.vlaanderen.be/) application profiles | Catalogues published by the domains, and each domain's own dataset description | FR-HTML-09, FR-META-01 |
| [Data on the Web Best Practices](https://www.w3.org/TR/dwbp/) (W3C) | Metadata and licence information for each published dataset | FR-META-01, FR-META-02 |
| [DCAT 3](https://www.w3.org/TR/vocab-dcat-3/) and [VoID](https://www.w3.org/TR/void/) | Dataset description per domain, including `/.well-known/void` and class and property partitions | FR-META-01, FR-HTML-15 |
| [SPARQL 1.1 Service Description](https://www.w3.org/TR/sparql11-service-description/) | Description of the `/sparql` endpoint | FR-META-03 |
| [RFC 8288: Web Linking](https://www.rfc-editor.org/rfc/rfc8288), relation type `license` | Licence link on every response | FR-META-02 |
| [Hydra Core Vocabulary](https://www.hydra-cg.com/spec/latest/core/) (W3C Community Group draft) | Paged lists: the values of a large property, incoming relations and the levels of a hierarchy ([ADR 0015](../../adr/0015-paged-lists-as-resources.md)) | FR-RA-07 |
| [SHACL](https://www.w3.org/TR/shacl/) `sh:SPARQLExecutable`, following the [SIB SPARQL examples](https://github.com/sib-swiss/sparql-examples) convention | Example queries as data, at `/.well-known/sparql-examples/` | FR-SPARQL-04 |
| [GeoSPARQL](https://docs.ogc.org/is/22-047r1/22-047r1.html) (OGC) | WKT literals with their CRS, shown on a map | FR-HTML-07 |
| [WCAG 2.1](https://www.w3.org/TR/WCAG21/) AA, and [WCAG 2.2](https://www.w3.org/TR/WCAG22/) AA as the target | Accessibility of the HTML | NFR-UI-02 |
| [Sitemaps protocol](https://www.sitemaps.org/protocol.html) and [RFC 9309: Robots Exclusion Protocol](https://www.rfc-editor.org/rfc/rfc9309) | Sitemaps of entry points, `robots.txt` per domain | FR-HTML-16 |
| [schema.org](https://schema.org/), with the schema.org mapping of [DCAT 3](https://www.w3.org/TR/vocab-dcat-3/) | Markup for search engines and Google Dataset Search | FR-HTML-17 |
| [Open Graph protocol](https://ogp.me/) | Link previews | FR-HTML-17 |
| [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html) | Authentication | FR-AC-01 |

!!! note
    The Flemish URI standard and the ELI implementation guide are published as PDF documents by their owners. This repository links to them rather than copying them.
