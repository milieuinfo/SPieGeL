# 0027. Render HTML on the server, as one more format of the same description, with islands for interaction

- **Status:** Proposed
- **Date:** 2026-10-27

## Context

SPieGeL serves HTML pages for its resources, next to Turtle, N-Triples, RDF/XML and JSON-LD (FR-HTML-01). The analysis assumed that HTML is rendered on the server, with Flux components that upgrade in the browser ([Design system: Flux](../analysis/03-requirements/design-system.md#constraints-for-spiegel), constraint 2). The Flux team first suggested a one-page application in JavaScript instead, and the question was left open ([open question 40](../analysis/06-open-questions.md#front-end)). The choice of template engine waited for it (open question 9).

On 26 October 2026 the Flux team advised, for the successor of the CBB Linked Data site, a **multi-page application rendered on the server**, with interaction as bounded islands. Its reasons match what SPieGeL had already decided:

- **One pipeline.** Content negotiation needs a server that decides per request whatever happens in the browser. A one-page application does not replace that server but adds to it: a second rendering path for HTML, a JSON API for it, and the risk that the HTML and the RDF drift apart. Rendered on the server, HTML is one more serialiser of the same description.
- **Content in the HTML as sent.** Crawlers, link previews, web archives and assistive technology read what the server sends (NFR-UI-04, FR-HTML-04, [ADR 0007](0007-uri-as-shared-identifier.md), [ADR 0019](0019-findability.md)). A one-page application reaches that only with prerendering.
- **Every resource and list page has its own URL**, without client-side routing and a server fallback.
- **Rendering shadow DOM on the server with Lit** needs Node.js and is still pre-release. Flux components that take their content as child elements or slots show it without that ([ADR 0014](0014-placement-of-linked-data-components.md), decision 4).

The one real advantage of a one-page application is that the global header and footer, with Digitaal Vlaanderen's profile widget, load once per visit instead of once per page. The Flux team measured that widget at more than 200 requests per page load, most of it between 0.15 and 1.7 seconds after navigation starts. The content does not wait for it, because it is already in the HTML, but each click pays for it again.

## Decision

1. **SPieGeL renders HTML on the server.** HTML is one more format of a description, chosen by the same content negotiation, on the same URL, with a format URL of its own such as `/doc/x.html` and the same `Link` alternates (FR-CN-01, FR-CN-02). It is built from the same bounded description as the RDF formats, and from the same cache entry ([ADR 0020](0020-cache-key-and-entity-tag.md)). There is no separate JSON API for HTML.
2. **Content is in the light DOM or in slots.** Templates emit Flux tags, slots and CSS classes, and the components upgrade in the browser. Shadow DOM is not rendered on the server, so SPieGeL needs no Node.js at run time. This confirms constraint 2 of the design-system analysis and decision 4 of ADR 0014.
3. **Interaction comes as islands**, each a component that a page works without:
    - the **geometry map** (`vl-map`, loaded lazily), with the coordinates or the address also written as text by the server (FR-HTML-07);
    - the **global header and footer**, loaded asynchronously, with their height reserved, a skip link to `<main>`, and the domains they need added to the CSP (NFR-UI-03);
    - the **thin layers of ADR 0014**: incoming relations, the hierarchy and the collection table, which load the paged lists of [ADR 0015](0015-paged-lists-as-resources.md) as JSON-LD. Without JavaScript, they are links to those lists, which are pages of their own;
    - the **SPARQL editor** of the portal ([ADR 0025](0025-federated-query-in-the-portal.md)).
4. **The test for every page: with JavaScript switched off, everything still works**, only with full page loads and without the islands' extras. End-to-end tests check it (NFR-Q-02).
5. **The cost of the global header is reduced, in this order:**
    1. asynchronous loading and reserved space (decision 3);
    2. the **back/forward cache** and **Speculation Rules** (prerendering a link on hover). HTML pages therefore never get `Cache-Control: no-store`, which SPieGeL sends only with `5xx` responses. Because the CSP allows no inline scripts, the rules come from an external file named in the `Speculation-Rules` response header, not from an inline `<script type="speculationrules">`;
    3. **partial navigation** (fetching the server's HTML on a click and swapping only `<main>`, as Turbo Drive or `hx-boost` do) only if measurements show that visitors click through many pages per visit. It must pass the test of decision 4, and handles focus, title, scroll position and page-view counting itself.
6. **The template engine is chosen next** ([open question 9](../analysis/06-open-questions.md#front-end)). The candidates stay Thymeleaf and JTE: both encode output by context (NFR-SEC-05), and both work without Spring in `spiegel-adapter-html` ([ADR 0004](0004-technology-baseline.md)).

## Consequences

- Open question 40 is answered, and question 9 can be decided.
- HTML follows the same request flow as the RDF formats: tenant, template, access level, cache, negotiation. NFR-PERF-01 applies to HTML as it does to RDF, and an HTML page cannot show other data than its RDF.
- The web adapter gets a renderer port when feature 005 adds HTML, as the [plan of feature 001](../specs/001-dereference-resource-as-rdf/plan.md) foresaw (decision 10): RDF serialisation and HTML rendering become two implementations of it.
- `spiegel-adapter-html` keeps the front-end build of [ADR 0005](0005-flux-design-system-for-html.md), but only for the islands and the Flux bundle; pages and their content are Java templates.
- Every page pays for the global header, which the predecessor's site does not load. SPieGeL accepts that cost and reduces it as in decision 5. A one-page application, or partial navigation, can still be added later on top of pages rendered on the server; the reverse, making a one-page application crawlable afterwards, is much harder.
- The Speculation Rules header and the back/forward cache become requirements of feature 005, and CSP must allow the external rules file.
- The same advice raised points that this record does not decide. Two of them differ from earlier decisions and stay open until they are discussed:
    - **keyset instead of offset pagination** for paged lists, which [ADR 0015](0015-paged-lists-as-resources.md) (decision 7) solves with offsets and mentions a cursor only as a later way out;
    - **crawlable list pages**, while [ADR 0019](0019-findability.md) excludes `?_page`, `?_property` and `?_inverse` in `robots.txt` and lists only entry points in the sitemap.

  How an HTTP cache in front of SPieGeL handles negotiated responses is [open question 43](../analysis/06-open-questions.md#architecture-and-operations). Whether to reuse an existing identifier for the profile widget, and login in the header, belong to access levels in phase 2 (FR-AC-01).
