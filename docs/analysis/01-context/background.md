# Background

## The predecessor

Since the early 2010s, the Flemish Government's Department of Environment and Spatial Development has published environmental and planning data as Linked Open Data. A single platform serves several data domains, each on its own host name (for example `data.imjv.omgeving.vlaanderen.be`). For every resource it offers:

- an identifier (`/id/…`) that redirects with `303 See Other` to a document (`/doc/…`);
- the document itself, as HTML for people and as RDF (Turtle, RDF/XML, N-Triples, JSON-LD) for machines;
- vocabulary pages (`/ns/…`) for the domain ontologies;
- a public SPARQL endpoint, keyword search, and an HTML front end that queries that endpoint from the browser.

The platform runs on [NetKernel](https://en.wikipedia.org/wiki/NetKernel), a resource-oriented computing runtime. NetKernel is no longer actively developed. Much of the platform's behaviour lives in Groovy scripts, FreeMarker templates, XSLT and an XML query catalogue rather than in compiled, tested code. That makes it hard to maintain, and hard to change with confidence.

See [Current platform](../02-current-platform/functional-overview.md) for the full picture.

## Why a new product rather than a port

A one-to-one port to another runtime would carry over the predecessor's weaknesses: business logic hidden in configuration, routes generated at runtime from environment variables, and behaviour that differs from what the configuration suggests. The analysis showed several places where the *intended* behaviour and the *actual* behaviour diverge (see [Request flows](../02-current-platform/request-flows.md)).

SPieGeL therefore starts from the intended behaviour, expressed as [requirements](../03-requirements/functional.md), and implements it as an explicit, tested domain model inside a [hexagonal architecture](../05-architecture/hexagonal-design.md).

## Why open source

Publishing Linked Data with content negotiation, a URI policy and access control is not specific to one department. Other public bodies in the Flemish [OSLO](https://www.vlaanderen.be/digitaal-vlaanderen/onze-oplossingen/oslo) and European [SEMIC](https://interoperable-europe.ec.europa.eu/collection/semic-support-centre) ecosystems face the same problem. Existing open-source tools cover parts of it, but none covers the combination we need (see [Landscape](../04-landscape/comparison.md)). Building SPieGeL in the open lets others reuse it and contribute to it.

## X-Cite

SPieGeL replaces and improves the NetKernel-based platform in line with the requirements of the **X-Cite** project. X-Cite describes simulation models and datasets with metadata and publishes them as DCAT catalogues, for which it needs a Linked Data service. Its requirements for that service overlap strongly with the needs of the predecessor's successor, so SPieGeL serves both. The X-Cite requirements are traced in [Traceability](../03-requirements/traceability.md), and the phasing is described in [Scope](scope.md).
