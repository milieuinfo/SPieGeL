# 0014. Keep Linked-Data-specific components in SPieGeL, contribute generic ones to Flux

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

SPieGeL's HTML follows the Flux design system ([ADR 0005](0005-flux-design-system-for-html.md)). The analysis listed seven components that Flux does not offer as such: a resource description, an RDF term, incoming relations, a hierarchy tree, a collection table, a geometry map and a SPARQL editor (see [Design system: Flux](../analysis/03-requirements/design-system.md#components-spiegel-has-to-build)). [Open question 23](../analysis/06-open-questions.md#front-end) asked whether they live in SPieGeL or in Flux.

The Flux team reviewed the seven against Flux 2.21.0 in a working document of 16 October 2026. It sorted them into three kinds:

- **composition:** markup in the server templates, built entirely from Flux components, with no custom element of its own (resource description, RDF term);
- **thin layer:** a small component that loads data and passes it to an existing Flux component, which provides the look (incoming relations, hierarchy tree, collection table, geometry map);
- **new:** no Flux base, so a new interface that follows Flux's conventions and CSS variables (SPARQL editor).

For the hierarchy tree, Flux 2.21.0 offers `vl-cascader`, a drilldown with a breadcrumb that loads each level on demand. The analysis had found no base for it. Whether a drilldown is enough, or whether data structures need another kind of visualisation, has been put back to the Flux team as a broader question ([open question 39](../analysis/06-open-questions.md#front-end)).

Flux accepts contributions from other teams, provided they are backwards compatible and the approach is agreed with the Flux team first. A contribution makes a component reusable, but ties its release to Flux's.

## Decision

1. **What is specific to Linked Data stays in SPieGeL. What turns out to be generic is offered to Flux**, backwards compatible and agreed with the Flux team beforehand.
2. Per component:

    | Component | Kind | Where |
    |---|---|---|
    | Resource description | composition | server template in SPieGeL |
    | RDF term | composition | server template in SPieGeL; the choice of label and language is made on the server |
    | Incoming relations | thin layer on `vl-accordion`, `vl-pager`, `vl-loader` | component in SPieGeL |
    | Hierarchy | thin layer, candidate `vl-cascader` | adapter in SPieGeL; **the visualisation itself waits for open question 39**. A generic tree or structure view would be a Flux component |
    | Collection table | thin layer on `vl-rich-data-table`, `vl-search-filter`, `vl-pager` | controller in SPieGeL; the pattern for paging on the server may be offered to Flux |
    | Geometry map | thin layer on `vl-map`, `vl-map-features-layer` | component in SPieGeL; WKT as an input format for the features layer may be offered to Flux |
    | SPARQL editor | new | component in SPieGeL, wrapping an existing editor ([open question 37](../analysis/06-open-questions.md#front-end)) |

3. **SPieGeL's own components follow Flux's conventions:**
    - Lit elements with **their own prefix**, not `vl-`;
    - Flux's CSS variables, with no inline styles or scripts (NFR-UI-03);
    - Cypress component tests with `cypress-axe` (NFR-UI-02).
4. **Content stays on the server.** Compositions are written into the page by the templates, in the light DOM or in slots, so that the page is readable without JavaScript (NFR-UI-04). Thin layers only load more data and add interaction.

## Consequences

- SPieGeL builds three to four small components and one wrapper, not seven components. Five of the seven add no new visual element to the house style.
- The templates carry more of the work: the resource description and the RDF term are markup patterns, tested on the server side (NFR-Q-02), not custom elements.
- Contributions to Flux, such as paging on the server or WKT input, need agreement and follow Flux's release cycle. Until they are released, SPieGeL keeps its own version.
- The hierarchy on subject pages of concept schemes, concepts, catalogues and datasets (FR-HTML-05) cannot be finished before open question 39 is answered.
- Open points that Flux has to answer, such as a lazy-loading event on `vl-accordion` or a `vl-pager` without a total, are listed in [Design system: Flux](../analysis/03-requirements/design-system.md#feedback-from-the-flux-team). SPieGeL works around a missing feature until Flux offers it.
- The licence of Flux and access to its packages for external contributors ([open question 22](../analysis/06-open-questions.md#front-end)) are not settled by this decision.
