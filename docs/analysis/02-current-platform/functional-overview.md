# Functional overview of the predecessor

This chapter describes what the predecessor does today, from the outside in. It is based on reading the predecessor's source code, its front-end package and its edge configuration. It describes **behaviour**. Implementation details are mentioned only where they explain behaviour that SPieGeL must reproduce, or must deliberately not reproduce.

## Endpoints per domain

Every active domain offers the same set of endpoints on its own host name:

| Endpoint | Behaviour |
|---|---|
| `/id/{concept}/{id}` | `303 See Other` to `/doc/{concept}/{id}`. |
| `/doc/{concept}/{id}` | The resource description. HTML for browsers; RDF/XML, Turtle, N-Triples or JSON-LD by content negotiation. Example: `https://data.omgeving.vlaanderen.be/doc/catalog/codelijst`. |
| `/ns/{model}` | The vocabulary (ontology) page for a model of the domain. |
| `/sparql` | A public SPARQL endpoint (GET and POST). Results as HTML or as standard SPARQL result formats. Read-only. |
| `/keywordsearch` | Full-text search, using the triple store's own full-text index. |
| `/` | A static home page per domain, with documentation, example queries and example search terms. |

Two further services exist in the code but are **not routed**: an OpenRefine-compatible reconciliation service and a [Linked Data Fragments](https://linkeddatafragments.org/) interface. Whether anyone relies on them is an [open question](../06-open-questions.md).

## Domains

See [Data domains](data-domains.md).

## Access levels

Data can be classified in five access levels: *public*, *internal*, *confidential*, *secret* and *top secret*. The user's level is derived from roles in an OpenID Connect token. A legacy single sign-on path still exists alongside it. Each access level maps to its **own read-only database account** on the triple store. That account can only read the graphs that the level may see. See [Non-functional requirements](../03-requirements/non-functional.md#security).

## HTML front end

The HTML pages are rendered on the server with XSLT. They embed web components from a shared front-end package ([`omgeving-ld`](https://github.com/milieuinfo/linked-data)). These components run further SPARQL queries **from the browser** against the public `/sparql` endpoint, for example to show incoming relations, browse a SKOS taxonomy or browse a DCAT catalogue. See [Client components](client-components.md).

## What sits in front of it

A reverse proxy per domain:

- maps the public host name to the domain;
- answers `/id/` requests with the `303` redirect before they reach the application;
- adds an open CORS policy (`Access-Control-Allow-Origin: *`), HSTS and some input filtering.

The open CORS policy is a **deliberate open-data choice**: public, non-credentialed data should be usable from any origin.

## Configuration-driven routing

Which `{concept}` and `{model}` values exist per domain is not defined in the application's code. It comes from per-environment CSV files that are compiled directly into the route patterns at start-up. A concept that is not listed has no endpoint at all (404). The content behind those concepts (ontologies, concept schemes, SHACL shapes, DCAT catalogues and collections) is maintained in a separate family of version-controlled projects, one set per domain, and loaded into the triple store. See [URI patterns](uri-patterns.md).
