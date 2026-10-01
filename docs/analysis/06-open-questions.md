# Open questions

Questions that the analysis could not settle. When one is answered, record the answer as an [ADR](../adr/README.md) and link it here, rather than deleting the question.

## Product and scope

1. **One multi-tenant service, or one service per domain?** SPieGeL's design assumes one stateless multi-tenant service (FR-MT-01). Newer domains, however, use small purpose-built services. Which model does the organisation want for the long term?
2. **Are the non-public access levels still used?** The model catalogue initiative assumes authentication is "no longer used in practice". If that is true for every domain, phase 2 can drop access control. Usage data per domain should settle this before FR-AC-01 to 03 are built.
3. **Are reconciliation, Linked Data Fragments and the alternative search back ends used by anyone?** They exist in the predecessor but are not routed.
4. **Is keyword search still needed?** The model catalogue initiative grades full-text search as *won't*; the predecessor offers it per domain.
5. **Should the restricted HTML view of incoming relations exist?** The predecessor intended it but never had it in effect (see [Request flows](02-current-platform/request-flows.md)).
6. **Is the `dba` domain still maintained**, or can it be published as a static snapshot?
7. **Which consumers rely on the predecessor's URIs and formats?** For example dataset catalogues and metadata portals that link to it.

## Front end

8. **Do the `omgeving-ld` browser components stay?** If yes, SPieGeL must keep a same-origin `/sparql` endpoint and the HTML conventions in [Client components](02-current-platform/client-components.md). If no, their functions must be rebuilt (FR-HTML-05).
9. **Which design system and template engine for HTML?** FluxUI web components are under consideration.
10. **Where are the domain-specific explorer scripts** for `imjv`, `cbb` and `dba`, and are they still needed?

## URI policy

11. **Where does the list of valid concepts per domain come from?** In the predecessor it is deployment configuration. Options: SPieGeL's tenant configuration, the data itself (`co:Collection` resources), or the vocabulary projects.
12. **Which domains publish legislative resources** that need ELI paths (FR-URI-05)?

## Architecture and operations

13. **Which edge concerns move into SPieGeL?** The `303` redirect, CORS, HSTS and input filtering are currently handled by the reverse proxy.
14. **Does the core use Jena's model types,** or its own RDF abstraction? See [Hexagonal design](05-architecture/hexagonal-design.md#rules-that-keep-the-core-clean).
15. **Is the data loading (ETL) chain in scope** of this project at all, or a separate line of work?
16. **Which configuration mechanism in production?** For example environment variables, mounted files or a configuration server.
17. **Fork pull requests:** the internal CI does not build pull requests from external forks. When external contributors appear, do we add a minimal public CI for them?
