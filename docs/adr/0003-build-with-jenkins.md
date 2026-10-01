# 0003. Build with the departmental Jenkins

- **Status:** Accepted
- **Date:** 2026-10-01

## Context

SPieGeL is hosted on GitHub but built and released by the Department of Environment and Spatial Development. The department's other public repositories on GitHub are built by its own Jenkins, using a shared pipeline library. That library covers Maven builds, releases, static analysis and vulnerability scanning. Some of these repositories also publish GitHub Pages from Jenkins.

## Decision

- SPieGeL is built by the departmental Jenkins with the shared pipeline library, through `Jenkinsfile.groovy` in the repository root.
- The pipeline builds the documentation with MkDocs and, on the `main` branch, pushes the result to the `gh-pages` branch.
- No GitHub Actions workflows for now.

## Consequences

- Builds, releases and scans follow the same process as the department's other projects.
- Pull requests from external forks are not built automatically, because untrusted code must not run on internal infrastructure, and contributors cannot see internal build results. When external contributions start, a minimal public CI for forks will be reconsidered (see [Open questions](../analysis/06-open-questions.md)).
