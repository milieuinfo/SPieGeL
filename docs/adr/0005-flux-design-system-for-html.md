# 0005. Use the Flux design system for HTML

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

SPieGeL serves HTML subject pages, vocabulary pages, a front page per domain and an HTML view of SPARQL results. The predecessor renders them with XSLT and the Vue 2 components of `omgeving-ld`, which are not part of any design system. The Flemish Government's house style is turned into web components by the department's Flux team. New applications of the department are expected to use Flux, which also brings guidelines on accessibility and Content Security Policy.

The choice was left open in the analysis (open question 9). See [Design system: Flux](../analysis/03-requirements/design-system.md) for what Flux offers and how it maps onto the predecessor.

## Decision

SPieGeL's HTML complies with the **Flux design system**, version 2 (2.20.0 at the time of writing), and follows its upgrades:

- Pages follow Flux's page-layout pattern and use Flux components and styles wherever one exists, preferring the *next* variants and avoiding deprecated components.
- Linked-Data-specific components that Flux lacks are built to Flux's conventions: Lit elements, CSS-in-JS with Flux's CSS variables, CSP-compliant, WCAG AA, with component and accessibility tests.
- The `omgeving-ld` components are not carried over.

The choice of template engine remains open.

## Consequences

- `spiegel-adapter-html` needs a front-end build (Node.js, pnpm, a bundler) that produces a bundle of the Flux components SPieGeL uses and of its own components. That build must run in Maven and in Jenkins, with read access to the department's Artifactory for the `@domg-wc` packages.
- Several components have to be built: a resource description, a hierarchy tree, incoming relations, a collection table, a geometry map and a SPARQL editor. That is work the predecessor did not need to do again.
- Accessibility (NFR-UI-02) and a strict Content Security Policy (NFR-UI-03) become testable requirements.
- SPieGeL depends on Flux's release cycle, including the planned v3. Its licence and availability to external contributors are still to be clarified (open question 22).
- Pages that rely on the old components' conventions (`ld-subject`, browser-side `.rq` templates) will no longer get them. Whether those conventions must stay for a transition period is open question 8.
