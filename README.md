# SPieGeL

**SPieGeL** is an open-source, configurable Linked Data publishing server. Every resource is served in the representation the client asks for: HTML for people, and Turtle, RDF/XML, N-Triples or JSON-LD for machines.

> *Spiegel* is Dutch for *mirror*. A mirror shows the same thing in a different form, which is what content negotiation does for a resource on `/doc/` and `/ns/`. The capitals honour the founders (**S**tijn, **Pie**ter and **Ge**ert), and the **L** stands for Linked Data.

## Status

Early design phase. Nothing is ready for use yet.

SPieGeL is being built by the Flemish Government's Department of Environment and Spatial Development (Departement Omgeving). It will succeed a NetKernel-based Linked Open Data platform that serves several environmental data domains.

Documentation, including the full analysis and the architecture decisions, is published at <https://milieuinfo.github.io/SPieGeL/> (source in [`docs/`](docs/)).

## Goals

- **Content negotiation as a first-class concern.** One URI, many representations. This includes [content negotiation by profile](https://www.w3.org/TR/dx-prof-conneg/), so a client can ask for a lighter or a richer view of the same resource.
- **Configurable URI templates.** Each URI template is bound to its own SPARQL query on a named data source. Adding a new resource type should not require new code.
- **URI policy out of the box.** SPieGeL follows the `id` / `doc` / `ns` pattern of the Flemish URI standard for data, with `303 See Other` from `/id/` to `/doc/`. Skolem IRIs (`/.well-known/genid/…`) are dereferenceable. A dedicated path structure is planned for [ELI](https://eur-lex.europa.eu/eli-register/about.html) legislative resources.
- **Hexagonal architecture.** A plain domain core, with data sources (SPARQL endpoints first, relational sources via R2RML later), renderers and identity providers behind ports and adapters.
- **Multi-tenant.** One stateless instance can serve several data domains, each selected by host name.
- **Access levels.** Each access level can be mapped to its own read-only credentials on the data source, so authorisation is enforced by the store as well as by the application.
- **Open by default.** CORS is open and a public SPARQL endpoint is available, in line with an open data policy.

## Technology

Java 21, Spring Boot 4 and Apache Jena 6, built with Maven. See [ADR 0004](docs/adr/0004-technology-baseline.md).

```shell
mvn verify                      # build and test all modules
mvn -pl spiegel-app spring-boot:run
```

## Related work

SPieGeL owes a debt to existing Linked Data front ends: [Pubby](https://github.com/cygri/pubby), [LodView](https://github.com/LodLive/LodView), [Trifid](https://github.com/zazuko/trifid), [ELDA](https://github.com/epimorphics/elda), [Prez](https://github.com/RDFLib/prez) and [grlc](https://github.com/CLARIAH/grlc). Its distinguishing features are the multi-tenant set-up, the authorisation per access level, the conformance to the URI policy, and the pluggable, hexagonal core.

## Licence

[MIT](LICENSE) © 2026 Vlaamse Overheid, Departement Omgeving.
