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
9. **Which template engine for HTML?** The design system is decided: Flux ([ADR 0005](../adr/0005-flux-design-system-for-html.md)). The template engine must emit custom elements, slots and attributes cleanly, and encode output by context (NFR-SEC-05).
10. **Where are the domain-specific explorer scripts** for `imjv`, `cbb` and `dba`, and are they still needed?
21. **How are presentation rules expressed?** The predecessor chooses blocks and components with XSLT rules by type and predicate (see [Data-dependent HTML rendering](02-current-platform/html-rendering.md)). FR-HTML-06 makes these rules configuration. Candidates: rules attached to profiles (as in Prez), viewers per URI template (as in ELDA), or a small rule format of our own.
22. **Under which licence is Flux published, and can external contributors build SPieGeL?** The Flux repository has no licence file, and its packages come from the department's Artifactory, not from the public npm registry. SPieGeL is public and MIT-licensed, and its release would bundle Flux code (see [Design system: Flux](03-requirements/design-system.md#consequences-for-the-design)).
23. **Do the new Linked Data components live in SPieGeL or in Flux?** A resource description, a hierarchy tree, incoming relations, a collection table, a geometry map and a SPARQL editor are needed (see [Design system: Flux](03-requirements/design-system.md#components-spiegel-has-to-build)). Contributing them to Flux makes them reusable but ties their release to Flux's.
24. **Do interactive components query `/sparql` from the browser, or call small JSON endpoints?** The predecessor's components build SPARQL in the browser. Server-side endpoints per function keep queries configurable, cacheable and authorised (NFR-SEC-03, NFR-SEC-04).

## URI policy

11. **Where does the list of valid concepts per domain come from?** In the predecessor it is deployment configuration. Options: SPieGeL's tenant configuration, the data itself (`co:Collection` resources), or the vocabulary projects.
12. **Which domains publish legislative resources** that need ELI paths (FR-URI-05)?

## Architecture and operations

13. **Which edge concerns move into SPieGeL?** The `303` redirect, CORS, HSTS and input filtering are currently handled by the reverse proxy.
14. **Does the core use Jena's model types,** or its own RDF abstraction? See [Hexagonal design](05-architecture/hexagonal-design.md#rules-that-keep-the-core-clean).
15. **Is the data loading (ETL) chain in scope** of this project at all, or a separate line of work?
16. **Which configuration mechanism in production?** For example environment variables, mounted files or a configuration server.
17. **Fork pull requests:** the internal CI does not build pull requests from external forks. When external contributors appear, do we add a minimal public CI for them?
18. **Are redirects for moved or retired URIs data or configuration?** Trifid reads them from the store, modelled with the W3C HTTP vocabulary, so data owners can manage them without a deployment (see [Trifid code review](04-landscape/trifid-code-review.md#what-spiegel-can-learn-from-it)). The alternative is SPieGeL's tenant configuration. This affects NFR-OPEN-02.
19. **Hand-written queries, generated queries, or both?** Prez generates each description query from a SHACL shape per profile; SPieGeL's requirements assume query files (FR-RA-02). Generating the simple cases and keeping files for tuned ones is a possible middle way (see [Prez code review](04-landscape/prez-code-review.md#what-spiegel-can-learn-from-it)).
20. **Do we adopt the Linked Data API vocabulary for tenant configuration?** ELDA's model (item and list endpoints, item templates, selectors, viewers, formatters) covers most of FR-URI-01, FR-RA-01 and FR-HTML-03, and comes with a published vocabulary (see [ELDA code review](04-landscape/elda-code-review.md#what-spiegel-can-learn-from-it)). The alternatives are a subset of it, extended with profiles and access levels, or SPieGeL's own format.
