# Standards

SPieGeL implements or follows these standards. Where the predecessor deviates from one, that is noted in [Current platform](../02-current-platform/uri-patterns.md#known-gaps).

| Standard | Used for | Requirements |
|---|---|---|
| Flemish URI standard for data (Informatie Vlaanderen, v1.0, 2017) | `id` / `doc` / `ns` URI shape, `303` redirect | FR-URI-01 to 03 |
| [European Legislation Identifier (ELI)](https://eur-lex.europa.eu/eli-register/about.html), including the *ELI Technical Implementation Guide* | `/eli/…` paths for legislative resources | FR-URI-05 |
| [RDF 1.1 Concepts §3.5: Skolemisation](https://www.w3.org/TR/rdf11-concepts/#section-skolemization) | `/.well-known/genid/` IRIs | FR-URI-04 |
| [Cool URIs for the Semantic Web](https://www.w3.org/TR/cooluris/) | `303` pattern, content negotiation, `Content-Location`, links between a thing and its document | FR-URI-02, FR-URI-07, FR-CN-01, FR-CN-07, FR-CN-08 |
| [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110) | Content negotiation, `301`, `303`, `404`, `406`, `410`, `Vary`, `HEAD` | FR-CN-01, FR-CN-05, FR-CN-06, FR-CN-09, FR-URI-08, FR-URI-10 |
| [Content Negotiation by Profile](https://www.w3.org/TR/dx-prof-conneg/) (W3C draft) | Profile negotiation | FR-CN-03 |
| [RFC 8288: Web Linking](https://www.rfc-editor.org/rfc/rfc8288) | `Link` headers to alternates | FR-CN-04 |
| [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/) and [Query Results JSON](https://www.w3.org/TR/sparql11-results-json/) | `/sparql` endpoint | FR-SPARQL-01, 02 |
| [RML](https://kg-construct.github.io/rml-resources/portal/) (W3C Knowledge Graph Construction Community Group) and [R2RML](https://www.w3.org/TR/r2rml/) | Mappings from non-RDF sources (relational, CSV, JSON, XML, Web APIs) to RDF | FR-DS-02 |
| [Linked Data Event Streams](https://semiceu.github.io/LinkedDataEventStreams/) (SEMIC) | Event streams that publish the same resources; discovery from SPieGeL | FR-ES-01 |
| [Spatial Data on the Web Best Practices](https://www.w3.org/TR/sdw-bp/) (W3C and OGC) | HTTP URIs for spatial things in every distribution, links between distributions | FR-URI-02, FR-HTML-13 |
| [DCAT-AP](https://interoperable-europe.ec.europa.eu/collection/semic-support-centre/dcat-ap) and the Flemish [OSLO](https://data.vlaanderen.be/) application profiles | Catalogues published by the domains, and each domain's own dataset description | FR-HTML-05, FR-META-01 |
| [Data on the Web Best Practices](https://www.w3.org/TR/dwbp/) (W3C) | Metadata and licence information for each published dataset | FR-META-01, FR-META-02 |
| [DCAT 3](https://www.w3.org/TR/vocab-dcat-3/) and [VoID](https://www.w3.org/TR/void/) | Dataset description per domain, including `/.well-known/void` and class and property partitions | FR-META-01, FR-HTML-15 |
| [SPARQL 1.1 Service Description](https://www.w3.org/TR/sparql11-service-description/) | Description of the `/sparql` endpoint | FR-META-03 |
| [RFC 8288: Web Linking](https://www.rfc-editor.org/rfc/rfc8288), relation type `license` | Licence link on every response | FR-META-02 |
| [Hydra Core Vocabulary](https://www.hydra-cg.com/spec/latest/core/) (W3C Community Group draft), or [TREE](https://w3id.org/tree/specification) | Paged lists of the values of a large property | FR-RA-07 |
| [SHACL](https://www.w3.org/TR/shacl/) `sh:SPARQLExecutable`, following the [SIB SPARQL examples](https://github.com/sib-swiss/sparql-examples) convention | Example queries as data, at `/.well-known/sparql-examples/` | FR-SPARQL-04 |
| [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html) | Authentication | FR-AC-01 |

!!! note
    The Flemish URI standard and the ELI implementation guide are published as PDF documents by their owners. This repository links to them rather than copying them.
