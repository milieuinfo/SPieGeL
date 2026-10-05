# Glossary

**Access level**
:   A classification of data by sensitivity. The predecessor uses five levels: *public*, *internal*, *confidential*, *secret* and *top secret*. A user's level is derived from roles in an identity token. *Public*, the lowest level, is the level of an unauthenticated client.

**Content negotiation**
:   The HTTP mechanism by which one URI returns different representations, chosen by the client's `Accept` header. See [RFC 9110 §12](https://www.rfc-editor.org/rfc/rfc9110#section-12).

**Content negotiation by profile**
:   Negotiating not only the *format* but also the *profile* of a representation, such as a lighter or richer description, or the same data in another model. See the W3C draft [Content Negotiation by Profile](https://www.w3.org/TR/dx-prof-conneg/).

**Data source**
:   A store that SPieGeL reads from. Initially a SPARQL endpoint; later possibly a relational database exposed through R2RML.

**Dereference**
:   To look up a URI over HTTP and get something useful back.

**Domain** (also *tenant*)
:   A coherent body of published data with its own host name, data source and URI space, for example `imjv` (the Integrated Environmental Annual Report).

**ELI**
:   [European Legislation Identifier](https://eur-lex.europa.eu/eli-register/about.html). A URI scheme and ontology for legislation and legal decisions, with its own hierarchical path structure.

**Hexagonal architecture** (also *ports and adapters*)
:   An architecture in which the domain core depends only on interfaces (*ports*). Technology-specific code (*adapters*) implements or calls those interfaces. See [Hexagonal design](../05-architecture/hexagonal-design.md).

**`id` / `doc` / `ns`**
:   The three URI types of the Flemish URI standard for data. `id` identifies a thing, `doc` is the web document about it, and `ns` is a vocabulary namespace. A request for an `id` URI is answered with `303 See Other` to the matching `doc` URI.

**Predecessor**
:   The NetKernel-based Linked Open Data platform that SPieGeL replaces.

**Profile**
:   A named specification that a representation conforms to, for example "summary" or "full", or "Data Cube" versus "SOSA observation".

**R2RML**
:   [RDB to RDF Mapping Language](https://www.w3.org/TR/r2rml/). A W3C language for exposing relational data as RDF.

**Skolem IRI**
:   An IRI that replaces a blank node, conventionally under `/.well-known/genid/`. See [RDF 1.1 Concepts §3.5](https://www.w3.org/TR/rdf11-concepts/#section-skolemization).

**URI template**
:   A pattern such as `/doc/{concept}/{id}` that SPieGeL matches against incoming requests. Each template is bound to the queries that describe resources matching it.
