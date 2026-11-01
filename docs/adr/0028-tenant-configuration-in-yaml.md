# 0028. Configure tenants in YAML, bound by Spring in spiegel-bootstrap

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

[ADR 0012](0012-tenant-configuration-as-rdf.md) described tenants in Turtle, in a vocabulary of SPieGeL's own, validated with SHACL shapes that SPieGeL ships. A configuration adapter, `spiegel-adapter-out-config`, would turn the Turtle into the core's types behind a `TenantConfigurationPort`.

In the [hexagonal design](../analysis/05-architecture/hexagonal-design.md), an adapter connects the core to something outside it that the core drives or that drives the core: a data source, a cache, a client over HTTP. The tenant configuration is neither. It is read once, at start-up, and fixes how the application is put together: which tenants exist, which data sources they use, which templates and queries apply. That is the job of `spiegel-bootstrap`, which configures and wires the application. It depends on every module, so it can create every bean it needs.

A configuration in RDF also has a cost of its own: a vocabulary with a persistent namespace, SHACL shapes to document, version and keep compatible, and a parser and validator to maintain. Spring Boot already binds and validates YAML.

## Decision

1. **The tenant configuration is YAML**, under one prefix, `spiegel.tenants`, with shared defaults under `spiegel.defaults`. `spiegel-bootstrap` binds it with `@ConfigurationProperties`.
2. **The YAML can be in the jar or outside it.** It is either part of `application.yml` in `spiegel-bootstrap`, or an external file that Spring loads through its standard configuration locations (`spring.config.import`, `spring.config.additional-location`), with Spring's usual precedence. Both are supported.
3. **SPieGeL validates the configuration at start-up.** The properties classes are checked with Bean Validation (`@Validated`), the mapping to the adapters' configuration types checks what Bean Validation cannot (references between data sources, templates and profiles; patterns that compile), and the SPARQL adapter checks every query against the query policy through `QueryInspector` ([ADR 0011](0011-query-policy.md)). One violation stops start-up.
4. **The core never sees YAML, nor the configuration itself.** `spiegel-bootstrap` maps the properties to the configuration types of the adapters that use them, and passes them as beans when it wires the application. The web adapter gets the tenants, their URI templates and profiles; the SPARQL adapter gets the data sources and the queries per resource type and profile. Each adapter defines its own configuration types, because adapters do not depend on `spiegel-bootstrap`. The web adapter matches the tenant of a request itself: per tenant and URI template, the configuration says whether a request that matches the template also matches the tenant by host name or by a path variable. It then passes the core a `Resource` that holds the tenant id and the resource type ([Hexagonal design](../analysis/05-architecture/hexagonal-design.md#domain-model)). There is no `TenantConfigurationPort`, no configuration adapter and no use case for resolving a tenant.
5. **The tenant's queries are in the YAML for now**, as multi-line strings. Whether they move to files of their own is decided later. What is RDF or JSON by nature stays in files of its own, next to the YAML, as decided before: the example queries as `sh:SPARQLExecutable`s (FR-SPARQL-04), the extracts of external vocabularies ([ADR 0021](0021-labels-of-external-vocabularies.md)) and the tenant's JSON-LD context ([ADR 0017](0017-framed-json-ld-with-a-frozen-context.md)).
6. **No secrets in the configuration.** A data source names its credentials by a key; their values come from the platform's secret store as Spring properties ([ADR 0013](0013-tenant-configuration-delivery.md)).
7. **The decisions of ADR 0012 that do not depend on the format remain:**
    1. **One generic template covers every concept.** A tenant normally declares one resource type with `/id/{concept}/{id}` and `/doc/{concept}/{id}`. Without anything else, it uses SPieGeL's default queries. A new concept in the data works at once, with no change to the configuration.
    2. **Exceptions are optional and describe only what differs.** A more specific template, such as `/doc/emissie/{id}`, adds its own queries, profiles or layout and inherits everything else. The most specific template wins (step 4 of the [request flow](../analysis/05-architecture/request-flow.md#steps)). An exception is chosen by path.
    3. **Queries are defaults plus the tenant's own.** SPieGeL ships default queries for the generic description, labels, one page of a large property and the default front-page blocks. They take bindings such as `?resource`, `?property` and `?lang`. A tenant replaces a default or adds to it. A limit is an attribute of the query, applied by the SPARQL adapter.
    4. **Large properties are declared once**, for all tenants in the shared defaults or per tenant, with a page size. A specific template may override this.
    5. **Template variables are typed by a pattern, and the data decides whether a resource exists.** A value that does not match gives `404` without a query. A value that matches is looked up, and a negative result is cached per data version (NFR-OPS-03).

A sketch of a tenant:

```yaml
spiegel:
  tenants:
    imjv:
      base: https://data.imjv.omgeving.vlaanderen.be
      licence: https://data.vlaanderen.be/id/licentie/modellicentie-gratis-hergebruik/v1.0
      language-priority: [nl, en, ""]
      data-sources:
        store-public:
          access-level: public
          credential-key: imjv-public
          enforces-graph-permissions: true
      default-data-source: store-public
      sparql-data-source: store-public
      resource-types:
        generic:
          item-template: /id/{concept}/{id}
          uri-template: /doc/{concept}/{id}
        # An exception: only what differs from the generic template.
        emissie:
          uri-template: /doc/emissie/{id}
          queries:
            - profile: full
              query: |
                CONSTRUCT { ?resource ?p ?o }
                WHERE { ?resource ?p ?o }
```

## Consequences

- SPieGeL owns no configuration vocabulary, no namespace at w3id.org and no SHACL shapes for its configuration. The properties classes in `spiegel-bootstrap` are the schema, and Spring Boot's configuration metadata documents them for editors.
- Profiles and the fixed facts of the dataset description are no longer RDF in the configuration. SPieGeL builds the RDF it publishes from them: `prof:Profile`s (FR-CN-03) and the DCAT and VoID description (FR-META-01). The example queries stay RDF, so they are published as they are.
- A configuration in the jar is simple for development, tests and small deployments, but every change then needs a new build of SPieGeL, and NFR-OPS-06 holds only with an external file. An external file is delivered as described in ADR 0013.
- CI validates the configuration with the same binding and checks as the server, for example with a validation run of `spiegel-bootstrap` that stops after start-up. `spiegel-tools` reuses them for the same purpose ([ADR 0026](0026-system-boundary.md)).
- Queries inside YAML are harder to read, edit and diff than `.rq` files. That is the reason the split into files of their own stays open.
- Tenant configuration and Spring configuration live in the same mechanism. What operations manage per environment (credentials, pool sizes, timeouts) can override what the Linked Data team writes, through Spring's precedence; reviews must watch that the two do not mix.
- Open question 20 is answered anew: neither the Linked Data API vocabulary nor a vocabulary of SPieGeL's own, but YAML.
