# 0013. Deliver tenant configuration as a versioned artefact

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

[Open question 16](../analysis/06-open-questions.md#architecture-and-operations) asks how the configuration reaches a running SPieGeL in production. The tenant configuration is Turtle with `.rq` files ([ADR 0012](0012-tenant-configuration-as-rdf.md)). The requirements constrain the answer:

- NFR-OPS-06: configuration is external to the build artefact and under version control;
- NFR-OPS-07: a deployment never mixes incompatible versions behind one host name;
- [ADR 0011](0011-query-policy.md): a configuration that fails validation is refused;
- FR-SPARQL-04: every example query is run against the domain's store, so that one that no longer works is noticed.

The options were:

- a **versioned artefact**, mounted read-only and rolled out with a rolling restart;
- **files synchronised live** from Git on every node, reloaded without a restart;
- a **configuration server**, such as Spring Cloud Config, with a refresh.

The last two make a change visible in seconds, but nodes run different versions until all of them have synchronised, and a configuration server is one more service to run. Most domains change their configuration rarely: their data changes far more often than their structure (see [Performance and availability](../analysis/03-requirements/performance.md#the-predecessor-today)).

The configuration must also live somewhere. Each domain already has vocabulary projects under version control, but keeping the configuration there would make one rollout depend on many repositories.

## Decision

1. **One central configuration repository**, with a directory per tenant and a directory for shared defaults, such as the large properties and label sources that hold for every tenant.
2. **CI validates every change** before anything is built:
    - the SHACL shapes of the configuration vocabulary;
    - the query policy, for every `.rq` file;
    - every example query, run against the domain's store (FR-SPARQL-04).
3. **CI builds a versioned artefact** from the repository and publishes it to the departmental Artifactory.
4. **In production the artefact is mounted read-only and rolled out with a rolling restart.** SPieGeL does not reload configuration at run time in production. A configuration that fails to load stops the new node from starting, and the nodes on the previous version keep serving. During development, SPieGeL may reload a local directory on change.
5. **The configuration version is visible:** in the logs at start-up and in the health endpoint (NFR-OPS-04), so that every response can be traced to a configuration.
6. **Credentials come from the platform's secret store**, looked up by the key that the configuration names. They are never in the repository or the artefact.

## Consequences

- One version of the configuration runs behind a host name at a time, and a faulty configuration never replaces a working one.
- A change takes minutes (build, publish, roll out), not seconds. That suits domains whose configuration changes rarely. Urgent changes to a single URI, such as a removal under the GDPR, do not wait for it, because they are data in the store (FR-URI-10, FR-URI-11).
- The Linked Data team works in one repository for all tenants. Changes to a domain's vocabulary and to its configuration are made in separate repositories, so a change that needs both needs two merges.
- CI needs read access to each domain's store, at the public level, to run the example queries.
- The configuration repository is separate from SPieGeL's own public repository. Whether it can be public too depends on what it contains: it holds host names and query files, but no credentials.
