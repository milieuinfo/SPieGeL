# 0012. Describe tenants in RDF, with one generic template and exceptions

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

Almost every functional requirement depends on the tenant configuration: host names and data sources (FR-MT-01, FR-DS-01), URI templates (FR-URI-01), the queries that describe a resource (FR-RA-01 to 03), paging (FR-RA-07), layout (FR-HTML-03), labels and languages (FR-HTML-10 to 12), the licence (FR-META-02) and example queries (FR-SPARQL-04). Three open questions were still undecided:

- [question 20](../analysis/06-open-questions.md#architecture-and-operations): the format, and whether to adopt the Linked Data API vocabulary;
- [question 19](../analysis/06-open-questions.md#architecture-and-operations): hand-written queries, generated queries, or both;
- [question 11](../analysis/06-open-questions.md#uri-policy): where the list of valid concepts per domain comes from.

The configuration is written and maintained by the **Linked Data team**, who use Turtle, SHACL and SPARQL every day and also maintain each domain's vocabulary projects. Part of the configuration is RDF by nature: example queries are `sh:SPARQLExecutable`s (FR-SPARQL-04), profiles are `prof:Profile`s (FR-CN-03), and the dataset description is DCAT and VoID (FR-META-01).

The [Linked Data API](../analysis/04-landscape/elda-code-review.md) vocabulary, as used by ELDA, covers URI templates, item templates and views, but has no profiles in the sense of Content Negotiation by Profile, no access levels, no tenants selected by host name, and no query files. Its specification has not changed since 2013. Adopting it would mean extending it in most places.

Some queries cannot be left to the tenant. Every resource needs a readable page without configuration (FR-HTML-01), so a generic description query must exist. Each large property is queried with `LIMIT N+1` (FR-RA-07), and front-page blocks and labels have defaults (FR-HTML-09, FR-HTML-10). The core cannot build SPARQL syntax trees ([ADR 0010](0010-jena-graph-types-in-the-core.md)).

The predecessor compiles a list of valid concepts per domain into its routes. Every new concept therefore needs a change to the configuration, and the lists differ per environment.

## Decision

1. **The tenant configuration is Turtle**, in a small vocabulary of SPieGeL's own. It reuses existing terms wherever they fit: `prof:` for profiles, `sh:` for example queries, prefixes and patterns, `dcat:`, `dct:` and `void:` for the dataset description, and the Linked Data API's `api:uriTemplate` and `api:itemTemplate`. The proposed namespace is `https://w3id.org/spiegel/config#`, a neutral, persistent IRI, so that organisations other than the department can use it.
2. **SPieGeL validates the configuration when it loads it**, with SHACL shapes that it ships, and checks every query file against the query policy ([ADR 0011](0011-query-policy.md)). One violation refuses the whole configuration.
3. **The core never sees Turtle.** The configuration adapter turns it into the core's types: `Tenant`, `DataSource`, `UriTemplate`, `ResourceType`, `QuerySpec` and `Profile`.
4. **No secrets in the configuration.** A data source names its credentials by a key; their values come from the platform's secret store ([ADR 0013](0013-tenant-configuration-delivery.md)).
5. **One generic template covers every concept.** A tenant normally declares one `ResourceType` with `/id/{concept}/{id}` and `/doc/{concept}/{id}`. Without anything else, it uses SPieGeL's default queries. A new concept in the data works at once, with no change to the configuration.
6. **Exceptions are optional and describe only what differs.** A more specific template, such as `/doc/emissie/{id}`, adds its own queries, profiles or layout and inherits everything else. The most specific template wins (step 4 of the [request flow](../analysis/05-architecture/request-flow.md#steps)). An exception is chosen by **path**, which is known before any query runs. Choosing by `rdf:type` may be added later, first for the HTML layout (FR-HTML-06), where the type is known after the queries anyway.
7. **Queries are default templates plus hand-written files.** SPieGeL ships `.rq` templates for:
    - the generic description, bounded to its subject (FR-RA-06);
    - labels (FR-HTML-10);
    - one page of a large property (FR-RA-07);
    - the default front-page blocks (FR-HTML-09).

    The templates take bindings such as `?resource`, `?property` and `?lang`. A limit is an attribute of the `QuerySpec`, applied by the SPARQL adapter. A tenant replaces a default or adds to it with its own `.rq` files. Generating queries from SHACL shapes (as Prez does) may come later, as an adapter that supplies `QuerySpec`s; it is not built now.
8. **Large properties are declared once**, for all tenants in the shared defaults or per tenant, such as `dct:hasPart` or `skos:member` with a page size. A specific template may override this.
9. **Template variables are typed by a pattern, and the data decides whether a resource exists.** A variable has an `sh:pattern`; a value that does not match gives `404` without a query. A value that matches is looked up, and an empty description gives `404` (or `301` or `410`, FR-URI-08 to 11). A negative result is cached per data version (NFR-OPS-03). A tenant that wants a closed list writes it as a pattern, such as `^(dossier|besluit)$`.

A sketch of a tenant:

```turtle
<#imjv> a spiegel:Tenant ;
    spiegel:hostName "data.imjv.omgeving.vlaanderen.be" ;
    dct:license <https://data.vlaanderen.be/id/licentie/modellicentie-gratis-hergebruik/v1.0> ;
    spiegel:languagePriority ( "nl" "en" "" ) ;
    spiegel:dataSource <#store-public> ;
    spiegel:sparqlDataSource <#store-public> ;
    spiegel:resourceType <#generic>, <#emissie> .

<#store-public> a spiegel:DataSource ;
    spiegel:accessLevel spiegel:Public ;
    spiegel:credentialKey "imjv-public" ;
    spiegel:enforcesGraphPermissions true .

<#generic> a spiegel:ResourceType ;
    api:itemTemplate "/id/{concept}/{id}" ;
    api:uriTemplate  "/doc/{concept}/{id}" .

# An exception: only what differs from the generic template.
<#emissie> a spiegel:ResourceType ;
    api:uriTemplate "/doc/emissie/{id}" ;
    spiegel:query [ spiegel:file "queries/emissie-full.rq" ; spiegel:profile <#full> ] .
```

## Consequences

- The configuration is written in the language its authors already use, and validated with SHACL, which they know. Profiles, example queries and the dataset description can be published as they are.
- A typical tenant is small: a host name, data sources, a licence, a language order, one generic template and a few exceptions. Adding a concept to the data needs no configuration.
- SPieGeL owns a vocabulary and its SHACL shapes, which must be documented, versioned and kept backwards compatible for existing tenants.
- Every request for a path that matches a pattern but does not exist costs one bounded query, until its negative result is cached. The rate limit (NFR-OPS-08) and the bounded pool (NFR-OPS-02) limit the cost.
- People outside the Linked Data team, for example operations, do not edit this configuration. What they manage (credentials, pool sizes, timeouts per environment) stays in the application's Spring configuration.
- The namespace must be registered at w3id.org before the first release that reads it. Until then the vocabulary may still change.
- Question 18 stays as decided in FR-URI-11: a move or removal of a single URI is data in the store, a move of a whole pattern or host name is a rewrite rule in this configuration.
