# 0026. Draw SPieGeL's boundary by what answers requests, what runs in CI, what holds data, and what runs at the consumer

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

SPieGeL serves several domains (tenants), each with its own host name, data sources and configuration (FR-MT-01). A growing number of things concern all domains together rather than one:

- **already decided, inside SPieGeL:** the portal page on the umbrella domain's host (FR-HTML-08), the shared defaults of the configuration ([ADR 0012](0012-tenant-configuration-as-rdf.md), [ADR 0013](0013-tenant-configuration-delivery.md)), the extracts of external vocabularies ([ADR 0021](0021-labels-of-external-vocabularies.md)) and the federated SPARQL editor in the portal ([ADR 0025](0025-federated-query-in-the-portal.md));
- **shared infrastructure between nodes:** the shared cache ([ADR 0020](0020-cache-key-and-entity-tag.md)) and the shared counters for rate limits ([ADR 0022](0022-edge-concerns.md));
- **tools without a fixed place:** the context generator ([ADR 0017](0017-framed-json-ld-with-a-frozen-context.md)), the vocabulary extractor (ADR 0021), and the validation of tenant configuration with SHACL in CI (ADR 0012, ADR 0013);
- **published conventions without a fixed place:** the configuration vocabulary (`https://w3id.org/spiegel/config#`), the problem types (`https://w3id.org/spiegel/problem/`, [ADR 0016](0016-problem-details-for-errors.md)), the use of `rel="collection"` for event streams ([ADR 0023](0023-event-stream-discovery-and-versions.md)), and the convention for URIs in other distributions ([open question 32](../analysis/06-open-questions.md#architecture-and-operations));
- **still open:** an index of labels across domains ([open question 42](../analysis/06-open-questions.md#architecture-and-operations)), full-text search across domains (FR-SRCH-02), the umbrella catalogue that harvests the dataset descriptions (FR-META-01, [open question 25](../analysis/06-open-questions.md#product-and-scope)), and monitoring across tenants;
- **outside the project, but depended on:** the loading chain ([open question 15](../analysis/06-open-questions.md#architecture-and-operations)), which also signals each tenant's data version, the LDES servers ([ADR 0006](0006-resources-and-event-streams.md)), and the identity provider for phase 2.

Without a rule, each of these would be placed case by case: some in the application, some as separate services, some nowhere. A component that holds data from every domain inside SPieGeL would make SPieGeL stateful and a joiner of data across domains, which ADR 0006 leaves to consumers.

## Decision

1. **What answers HTTP requests on a tenant's host, from configuration, is part of SPieGeL, in the same artefact.** This includes the portal, as the front page of the umbrella domain's tenant, and the federated editor on it. A cross-domain feature that only presents what other tenants publish is a page of a tenant, not a separate service.
2. **What is used to write, check or prepare configuration, and not while a request is handled, is a separate artefact in the same repository:** a Maven module `spiegel-tools`, delivered as a command-line tool. It holds the context generator, the vocabulary extractor and the configuration validator. It shares code with the adapters, such as the configuration model and its SHACL shapes, so that CI and the server check the same rules. The scripts under `tools/` in the repository, such as the latency measurement, stay outside the build.
3. **What holds data gathered from several domains is never built or held by SPieGeL.** Such an index, of labels or for search across domains, is a store of its own, filled by the loading chain. SPieGeL reads it as an ordinary data source, through `DataSourcePort` or `SearchPort`, with the same access levels as any other. Whether such an index is needed is decided per case (open question 42, FR-SRCH-02).
4. **What runs at the consumer is not part of SPieGeL.** Query engines, LDES clients and integrations belong to the consumer, even when SPieGeL ships them to the browser, as the portal does with Comunica (ADR 0025).
5. **Defaults have two layers.** SPieGeL ships built-in defaults, such as the generic template and the default queries, that make it work without configuration. The organisation's shared defaults, such as the list of external vocabularies and the large properties, live in the configuration repository and are maintained by the Linked Data team (ADR 0013).
6. **Conventions that SPieGeL defines are documented and published with SPieGeL**: the configuration vocabulary and its shapes, the problem types, and the link relations it sends. They are versioned with SPieGeL's releases.
7. **Shared infrastructure between nodes is not a domain component.** A shared cache or counter store is a deployment choice behind a port (ADR 0020, ADR 0022), not a part of SPieGeL's design. Monitoring dashboards across tenants belong to the platform; SPieGeL provides metrics labelled by tenant ([ADR 0018](0018-personal-data-in-requests-and-caches.md)).

## Consequences

- SPieGeL stays a stateless reader: every piece of data it serves comes from a data source or from configuration, and it can be scaled and restarted freely (NFR-OPS-01).
- The portal and the federated editor need no deployment of their own. A new domain appears in them through configuration only (FR-MT-02).
- CI and the server use the same configuration model and the same validation, because `spiegel-tools` and the adapters share code.
- A cross-domain index is a separate piece of work, with an owner of its own, usually alongside the loading chain. SPieGeL only needs its endpoint in the configuration.
- Open question 42 is narrowed: SPieGeL will not build a label index itself. The options that remain are copies in each store, a shared label store, reading another tenant's public data source, or the IRI as it is.
- Several components outside the project (the loading chain, the umbrella catalogue, LDES servers, the identity provider, platform monitoring) need an owner. That is an organisational question, which this decision does not answer.
- The module structure gains `spiegel-tools`, which arrives when its first tool is built.
