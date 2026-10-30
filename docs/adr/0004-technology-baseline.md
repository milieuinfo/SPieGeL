# 0004. Technology baseline

- **Status:** Accepted
- **Date:** 2026-10-01

## Context

The [analysis](../analysis/05-architecture/module-structure.md) proposes a Maven multi-module build in a hexagonal layout. Before the first line of code, we fix the build coordinates and the main technology versions, so that every module starts from the same base.

## Decision

| Item | Choice |
|---|---|
| Group identifier | `be.vlaanderen.omgeving.spiegel` (in line with other departmental projects, such as `be.vlaanderen.omgeving:oddtoolkit`) |
| Artefact identifiers | `spiegel-parent`, `spiegel-core`, `spiegel-adapter-*`, `spiegel-app` |
| Java | 21 (LTS) as the compilation target. The build also runs on newer JDKs, including the JDK 25 used in CI. |
| Spring Boot | 4.1.x, through `spring-boot-starter-parent`. Only `spiegel-app` and inbound web adapters depend on Spring. |
| Apache Jena | 6.2.x, for the SPARQL adapter. |
| Tests | JUnit 5 and AssertJ. ArchUnit enforces the dependency rules. |
| Versioning | Semantic versioning; tags `v{version}` through the Maven release plugin. |
| Artefacts | Published to the departmental Artifactory, like other departmental projects. |

Modules are added when they are needed. Phase 1 starts with `spiegel-core`, `spiegel-adapter-config`, `spiegel-adapter-sparql`, `spiegel-adapter-web`, `spiegel-adapter-html` and `spiegel-app`. `spiegel-adapter-security` follows in a later phase.

*Amended on 2026-10-05:* the module for non-RDF sources was called `spiegel-adapter-r2rml`. It is renamed `spiegel-adapter-rml`, because RML generalises R2RML (see FR-DS-02). The module did not exist yet, so nothing else changed.

*Amended on 2026-10-25:* there is no module for non-RDF sources. Relational sources are read through `spiegel-adapter-sparql` from an Ontop endpoint, and other sources are materialised in the loading chain ([ADR 0009](0009-ontop-for-relational-sources.md)).

*Amended on 2026-10-28:* the modules are restructured along the hexagonal layers: `spiegel-core` is split into `spiegel-model` and `spiegel-application`, adapters are grouped under `spiegel-adapter-in` and `spiegel-adapter-out` (`spiegel-adapter-web` becomes `spiegel-adapter-in-rest`, `spiegel-adapter-sparql` becomes `spiegel-adapter-out-virtuoso`), and `spiegel-app` becomes `spiegel-bootstrap`. `spiegel-adapter-config` and `spiegel-adapter-html` are removed until they are needed. See [Module structure](../analysis/05-architecture/module-structure.md).

*Amended on 2026-10-30:* HTML rendering and authentication are not adapters of their own in the hexagonal design. They belong to the web adapter, so `spiegel-adapter-html` and `spiegel-adapter-security` will not be added: rendering (Jena RIOT, the template engine and the front-end build) and authentication (Spring Security) live in `spiegel-adapter-in-rest`. See [Hexagonal design](../analysis/05-architecture/hexagonal-design.md#ports-and-adapters).

## Consequences

- `spiegel-core` inherits Spring Boot's plug-in and dependency management through the parent POM, but has no Spring dependency. `ArchitectureTest` checks this on every build.
- Raising the Java target to 25 is a separate, later decision, once every build and run environment supports it.
- The Maven enforcer plug-in requires Maven 3.8.7 or later and Java 21 or later. It does not enforce dependency convergence: Jena and Spring Boot pull in slightly different versions of shared libraries, and strict convergence would break on every upgrade.
