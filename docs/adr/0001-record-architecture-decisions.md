# 0001. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-10-01

## Context

SPieGeL replaces a platform whose design decisions were largely implicit: spread over scripts, templates and deployment configuration, and often only recoverable by reading code. As an open-source project, SPieGeL will also have contributors who were not present when decisions were taken.

## Decision

We record every architecturally significant decision as an ADR in `docs/adr/`, following Michael Nygard's [format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions). The [analysis](../analysis/README.md) is a snapshot that informs decisions; ADRs are where decisions are made.

## Consequences

- Open questions in the analysis are closed by an ADR, which the question then links to.
- Reversing a decision means writing a new ADR, so the history stays readable.
