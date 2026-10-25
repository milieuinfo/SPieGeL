# Architecture decision records

An architecture decision record (ADR) captures one significant decision: its context, the decision itself and its consequences. ADRs are numbered and never deleted. A decision that is reversed gets a new ADR that supersedes the old one.

Use [`template.md`](template.md) for new records.

| No. | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-documentation-as-markdown-with-mkdocs.md) | Documentation as Markdown, published with MkDocs | Accepted |
| [0003](0003-build-with-jenkins.md) | Build with the departmental Jenkins | Accepted |
| [0004](0004-technology-baseline.md) | Technology baseline | Accepted |
| [0005](0005-flux-design-system-for-html.md) | Use the Flux design system for HTML | Accepted |
| [0006](0006-resources-and-event-streams.md) | Publish resources and event streams as complementary forms | Accepted |
| [0007](0007-uri-as-shared-identifier.md) | Use the URI as the shared identifier in every distribution | Accepted |
| [0008](0008-neo4j-not-a-data-source.md) | Do not use Neo4j as a data source for SPieGeL | Accepted |
| [0009](0009-ontop-for-relational-sources.md) | Translate relational sources with Ontop, materialise other sources with RML | Accepted |
| [0010](0010-jena-graph-types-in-the-core.md) | Use Jena's graph types in the core, keep SPARQL out of it | Accepted |
| [0011](0011-query-policy.md) | Check every query against a query policy before it runs | Accepted |
| [0012](0012-tenant-configuration-as-rdf.md) | Describe tenants in RDF, with one generic template and exceptions | Accepted |
| [0013](0013-tenant-configuration-delivery.md) | Deliver tenant configuration as a versioned artefact | Accepted |
| [0014](0014-placement-of-linked-data-components.md) | Keep Linked-Data-specific components in SPieGeL, contribute generic ones to Flux | Accepted |
| [0015](0015-paged-lists-as-resources.md) | Serve large and follow-up data as paged Hydra lists | Accepted |
| [0016](0016-problem-details-for-errors.md) | Answer errors to machines with problem details (RFC 9457) | Accepted |
| [0017](0017-framed-json-ld-with-a-frozen-context.md) | Serve framed JSON-LD with a generated, frozen context | Accepted |
| [0018](0018-personal-data-in-requests-and-caches.md) | Minimise personal data from requests; leave personal data in the data to the store | Accepted |
| [0019](0019-findability.md) | Advertise entry points to search engines, with schema.org for datasets and terms | Accepted |
