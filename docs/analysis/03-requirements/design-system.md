# Design system: Flux

SPieGeL's HTML must comply with **Flux**, the design system of the Department of Environment and Spatial Development (Departement Omgeving). See [ADR 0005](../../adr/0005-flux-design-system-for-html.md). Flux turns the Flemish Government's house style (Digitaal Vlaanderen's *Webuniversum*) into web components and adds guidelines, patterns and recipes. Its documentation is a Storybook, published at <https://flux.omgeving.vlaanderen.be/release-v2/latest/storybook>. The source is at <https://github.com/milieuinfo/flux-web-components>.

**Terminology.** In the department, Flux's components are usually called *the web components*; the package scope `@domg-wc` stands for *Departement Omgeving web components*. They are web components in the technical sense: custom elements registered in the browser. Digitaal Vlaanderen also calls its Webuniversum components "webcomponents", but Flux points out that those are mainly CSS with a little JavaScript, not custom elements. Both follow the same house style and use the same `vl-` class names, so they look alike. This analysis says *Flux* or *Flux web components* to avoid confusion.

This chapter describes what Flux offers, which constraints it puts on SPieGeL, how the predecessor's [client components](../02-current-platform/client-components.md) map onto it, and which components SPieGeL still has to build. **Version reviewed:** Flux 2.20.0 (18 September 2026).

## What Flux offers

- **Components** in npm packages under the `@domg-wc` scope:
    - `@domg-wc/components` holds the web components, in four groups:
        - *atom*: button, icon, link, text, title;
        - *block*: accordion, table, rich data table, properties, description data, pager, tabs, modal, alert, info tile, infoblock and others;
        - *compliance*: department-specific components such as header, footer, accessibility statement, cookie consent and privacy;
        - *form*: input field, select, checkbox, date picker and others.
    - `@domg-wc/map`: a map component on OpenLayers, built from plug-in sub-components (base layers, vector, WMS and WMTS layers, legend, actions).
    - `@domg-wc/styles`: global and layout CSS (grid, section, content block, spacing).
    - `@domg-wc/common`: utilities, such as `registerWebComponents` and `FluxConfig`.
- **Guidelines** on accessibility (WCAG), Content Security Policy and testing. **Patterns** for page layout, forms, navigation, search and maps. **Recipes** for configuration, styling and bundling.
- **Conventions** for components. Each component is a Lit element (`LitElement`) with a custom `vl-` tag. Its styles are written as CSS-in-TypeScript and use standard CSS variables. The older style, extending native elements through `is=` with `data-` attributes, is being removed.

## Constraints for SPieGeL

1. **Consumed through a bundler.** The packages come from the department's Artifactory, which also serves the `@domg-wc` scope to non-employees. They are not on the public npm registry. A versioned CDN bundle exists, but it contains **only the compliance components** (header, footer, …). So SPieGeL needs a front-end build: Node.js with pnpm and a bundler, producing a tree-shaken bundle of the components it uses. SPieGeL serves that bundle itself, from its own origin.
2. **Server-side rendering in Java.** SPieGeL renders HTML on the server, as the predecessor does. Flux describes this set-up as common in the department. Lit's server-side rendering of shadow DOM needs Node.js, so it is not available in a Java server. Templates therefore emit Flux tags and CSS classes, and the components upgrade in the browser. The content that matters (title, properties, links) should be in the light DOM or in slots, so that it is readable before and without JavaScript, by crawlers and by assistive technology. Flux supports this. `vl-properties` accepts its rows as child elements (`<vl-property>Woonplaats</vl-property><vl-property-data>Brussel</vl-property-data>`), and `vl-description-data-item` accepts `slot="label"` and `slot="value"`. Both also have an attribute or JavaScript-property variant (`label=`/`value=`, `.props=`). SPieGeL uses the child and slot variants, because the attribute variants show nothing until the script has run.
3. **Page layout.** Pages follow the *Pagina Opbouw* pattern: `<vl-template>` with `<vl-header>`, a `<vl-functional-header>`, sections with `vl-section` and `vl-content-block`, and `<vl-footer>`. The full-width variant is meant for wide data tables. The header loads Digitaal Vlaanderen's global header and handles login, logout and changing organisation. That becomes relevant once access levels are introduced (FR-AC-01).
4. **Content Security Policy.** Flux recommends the strictest possible CSP. That is only meaningful from Flux 2.4.0 onwards, and with the CSP-compliant header introduced in 2.3.0. The policy has to allow the Digitaal Vlaanderen and CDN domains that the header and fonts use. Inline scripts are not allowed, so SPieGeL must not copy the predecessor's inline loader script. → NFR-UI-03.
5. **Accessibility.** Government sites must meet WCAG 2.1 AA by law. Flux's approach sets *bronze [basic]* (WCAG A, the basic part) for every application, and expects public applications to plan a route to *silver [plus]* (all of WCAG 2.2 AA). SPieGeL is public. → NFR-UI-02.
6. **Version line.** Flux v2 is current. Some components exist as a legacy and a *next* variant (`vl-header-next`, `vl-footer-next`, `vl-tabs-next`, `vl-side-navigation-next`). The next variant replaces the legacy one in v3, for which there is no date yet. `vl-search` and `vl-share-buttons` are deprecated and will be removed in v3. SPieGeL should use the next variants and avoid deprecated components from the start.
7. **Maps.** The map uses Lambert 72 (EPSG:31370) by default, and Lambert 2008 (EPSG:3812) is announced as the future default. The features layer takes features in **GeoJSON structure**, but reads their coordinates **in the map's projection** by default (Lambert 72), not in WGS84 as [RFC 7946](https://www.rfc-editor.org/rfc/rfc7946) prescribes. With the `projection-code` attribute the layer reprojects from another projection. The documented case is Lambert 72 data on a Lambert 2008 map. Only OpenLayers `Feature` objects added directly (`addFeatures`, `setFeatures`) are not transformed. For SPieGeL this means the map needs **no conversion on the server and no extra request**. The geometry is already in the page, in the description and in the embedded JSON-LD (FR-HTML-04). A small component reads the WKT literal and its CRS from there. Following GeoSPARQL, the CRS is the IRI at the start of the literal, for example `<http://www.opengis.net/def/crs/EPSG/0/31370> POINT(…)`; without one, it is WGS84 (CRS84). That covers RDF data in WGS84 and data from other sources in Lambert alike. The component turns the WKT into features with OpenLayers' WKT reader (`ol/format/WKT`), which reprojects into the map's projection. Proj4 is already registered in the Flux map for Lambert 72 and Lambert 2008. Alternatively, it passes GeoJSON-structured features with the CRS as `projection-code`, if the layer accepts that CRS. Whether it accepts WGS84 (EPSG:4326) still has to be checked. → FR-HTML-07.
8. **Testing.** For server-rendered applications, Flux recommends putting most tests on the server side and covering the front end with end-to-end tests (Cypress), including accessibility checks. → NFR-Q-02.

## From `omgeving-ld` to Flux

The predecessor's components come from `omgeving-ld`, which uses Vue 2. They are not Flux components. The table maps each one, and the other building blocks of the [current pages](../02-current-platform/html-rendering.md), to Flux 2.20.0.

| Predecessor | Purpose | Flux | Coverage |
|---|---|---|---|
| Page skin (`department-header`, header and footer partials, `ld-view`) | Page frame | `vl-template`, `vl-header-next`, `vl-footer-next`, `vl-functional-header` | **covered** |
| `flex-container`, `flex-item` | Grid | grid, section and content-block styles from `@domg-wc/styles` | **covered** |
| `ld-card`, `ld-card-title`, `ld-card-content` | Card around a block | `vl-info-tile`, `vl-infoblock`, `vl-content-block` | **covered** (a layout choice) |
| `ld-collapsible` | Collapsible block | `vl-accordion` | **covered** |
| `ld-subject`, `ld-predicate`, `ld-object` | List of properties: grouped values, links with labels, nested blank nodes | `vl-properties`, `vl-description-data` (label and value pairs) | **partly**: several values per predicate, nested blank nodes, language tags and datatypes need composition |
| Export links (HTML, JSON-LD, Turtle, N-Triples, RDF/XML) | Links to alternate formats | `vl-link`, `vl-button`, `vl-pill` | **covered** |
| `ld-predicate inbound` | Paged list of incoming relations, loaded in the browser, with counts | `vl-accordion` and `vl-pager` | **missing**: loading and paging relations is Linked-Data-specific |
| `ld-taxonomy`, `ld-dataset` | Lazily expanding tree of SKOS concepts or DCAT catalogues | none: Flux has no tree component | **missing** |
| `ld-data-table` | Searchable, paged table of collection members | `vl-rich-data-table` (filter, sorting, paging with `vl-pager`, responsive), `vl-search-filter` | **partly**: the interface exists, but loading the data is Linked-Data-specific |
| `ld-map` | One point on a map | `vl-map`, `vl-map-features-layer`, `vl-map-baselayer-grb-gray` | **covered**: features in GeoJSON structure, in the map's projection or with their `projection-code` |
| `ld-sparql-form` (YASQE) | SPARQL editor with example queries | none: Flux has no code editor | **missing** |
| SPARQL results as HTML (FR-SPARQL-03) | Result table | `vl-table`, `vl-rich-data-table` | **covered**, except for rendering RDF terms |
| `ld-search-form` | Search box | the input-group pattern (`vl-input-group` with a button); `vl-search` is deprecated | **covered** |
| `ld-lookup-form` | Lookup with suggestions | `vl-autocomplete` | **covered** |
| Error pages (404, 406, 500) | Error message | `vl-http-error-message` | **covered** |
| Front page per domain (FR-HTML-02, FR-HTML-09) | Introduction, discovered blocks (catalogues with datasets, thesauri, classes), example queries, search | `vl-content-header`, `vl-info-tile`, `vl-accordion`, `vl-rich-data-table` with `vl-pager` for long blocks, `vl-typography` | **covered** |
| Portal page (FR-HTML-08) | Overview of all domains, each linking to its front page | `vl-info-tile` (clickable) in a grid, `vl-content-header` | **covered** (new; the predecessor has no portal) |

## Components SPieGeL has to build

These are Linked-Data-specific. The first two are mostly markup composed on the server; the others are interactive web components.

| Component (working name) | What it does | Built from | Requirement |
|---|---|---|---|
| Resource description | The properties of a resource in [presentation blocks](../02-current-platform/html-rendering.md#rules-by-predicate-presentation-categories): grouped values, every property label and every IRI value as a link to its IRI with its label (FR-HTML-10), language tags and datatypes, nested blank nodes. Rendered on the server, so it works without JavaScript | `vl-properties` or `vl-description-data`, `vl-link`, `vl-pill`, `vl-accordion` | FR-HTML-01, FR-HTML-06 |
| RDF term | One IRI, literal or blank node: a label with the IRI as a link, the language as a pill, the datatype as a hint | `vl-link`, `vl-pill`, `vl-text` | FR-HTML-01 |
| Incoming relations | Per predicate: a count, plus paged subjects, loaded on demand | `vl-accordion`, `vl-pager` | FR-HTML-05 |
| Hierarchy tree | A lazily expanding tree for SKOS (`skos:hasTopConcept`, `skos:narrower`) and DCAT (`dcat:dataset`, `dcat:catalog`), keyboard-accessible, following the WAI-ARIA tree pattern | new; style from `@domg-wc/styles` | FR-HTML-05 |
| Collection table | Loads the members of a collection page by page and connects searching and sorting to SPieGeL | `vl-rich-data-table`, `vl-pager`, `vl-search-filter` | FR-HTML-05 |
| Geometry map | Shows one or more geometries of a resource (points, lines, polygons). It reads the WKT literals and their CRS from the page, so it needs no extra request, and turns them into features in the map's projection with OpenLayers' WKT reader | `vl-map`, `vl-map-features-layer`, a GRB base layer | FR-HTML-07 |
| SPARQL editor | Edits a query with syntax highlighting and example queries, runs it, and shows the results | an editor library behind a Flux-styled wrapper; results in `vl-rich-data-table` | FR-SPARQL-03, FR-SPARQL-04 |

The new components follow Flux's conventions:
- a Lit element with a custom tag in **its own prefix**, not `vl-`, which is Flux's;
- CSS-in-JS with Flux's CSS variables, and no inline `<style>` blocks or scripts;
- WCAG AA, keyboard operation and ARIA patterns;
- documentation and Cypress component tests, including accessibility checks (`cypress-axe`).

Whether they live in SPieGeL or are contributed to Flux is an [open question](../06-open-questions.md#front-end). Flux accepts contributions from other teams, provided they are backwards compatible and a new approach is agreed with the Flux team first.

The interactive components need data from the server. Today they build SPARQL queries in the browser ([query catalogue](../02-current-platform/query-catalogue.md#browser-side)). SPieGeL can keep that, through its same-origin `/sparql` (FR-SPARQL-02), or offer small JSON endpoints per function (counts, a page of relations, the children of a tree node). The second option keeps queries on the server, where they can be configured, cached and authorised (NFR-SEC-03, NFR-SEC-04). → [open question 24](../06-open-questions.md#front-end).

## Requirements and components

Which Flux components implement which requirement, and where a new component is needed. **Flux** means Flux components are enough. **Composed** means Flux components put together by SPieGeL's templates, with no new component. **New** means a component from the [list above](#components-spiegel-has-to-build) is needed. Requirements that are server logic only (choosing blocks, labels, languages, discovery) appear here only for the part that is visible.

| Requirement | Function on the page | Flux components | New component | Status |
|---|---|---|---|---|
| NFR-UI-01 | Page frame: header, functional header, content, footer | `vl-template`, `vl-header-next`, `vl-functional-header`, `vl-footer-next`; `vl-section`, `vl-content-block` and grid styles | — | Flux |
| FR-HTML-01, FR-HTML-06 | Subject page: title, presentation blocks, collapsible blocks | `vl-title`, `vl-accordion`, `vl-properties`, `vl-description-data` | Resource description | New |
| FR-HTML-10, FR-HTML-11 | Every IRI as a link with its label; literals with language and datatype | `vl-link`, `vl-pill`, `vl-text` | RDF term | New |
| FR-RA-04 | Nested structures (blank nodes) shown inside their resource | `vl-properties` (nested) | Resource description | New |
| FR-CN-02, FR-CN-04 | Links to the other formats and profiles of a resource | `vl-link`, `vl-button`, `vl-pill` | — | Composed |
| FR-HTML-04 | Data in machine-readable form (subject IRI, `<link rel="alternate">`, JSON-LD) | none: not visible | — | Template |
| FR-HTML-05 | Incoming relations: count per relation, paged subjects | `vl-accordion`, `vl-pager` | Incoming relations | New |
| FR-HTML-05 | Thesaurus hierarchy and catalogue structure as a tree | none: Flux has no tree component | Hierarchy tree | New |
| FR-HTML-05 | Members of a collection, paged, searchable and sortable | `vl-rich-data-table`, `vl-pager`, `vl-search-filter` | Collection table (loads the data) | New, on a Flux base |
| FR-HTML-07 | Geometries on a map | `vl-map`, `vl-map-features-layer`, `vl-map-baselayer-grb-gray` | Geometry map (reads WKT and CRS from the page; reprojection in the browser) | New, on a Flux base (thin) |
| FR-URI-03, FR-RA-05 | Vocabulary page: one card per term, class expressions as lists | `vl-info-tile` or `vl-infoblock`, `vl-properties` | Resource description | New |
| FR-HTML-02, FR-HTML-09 | Front page per domain: introduction, discovered blocks (catalogues with datasets, thesauri, classes), example queries | `vl-content-header`, `vl-typography`, `vl-info-tile`, `vl-accordion`; `vl-rich-data-table` and `vl-pager` for long blocks | — | Composed |
| FR-HTML-08 | Portal: one tile per domain, linking to its front page | `vl-info-tile` (clickable) in a grid, `vl-content-header` | — | Composed |
| FR-HTML-12 | Language choice and translated interface texts | none: Flux has no language switcher; links in the functional header (`vl-link`) | — | Composed (to check with the Flux team) |
| FR-SPARQL-03, FR-SPARQL-04 | Query editor with example queries, run, share by URL | `vl-button`, `vl-select` (examples) | SPARQL editor | New |
| FR-SPARQL-03 | Query results as a table with clickable IRIs | `vl-rich-data-table`, `vl-table`, `vl-pager` | RDF term (cells) | Flux + new |
| FR-SRCH-01 | Search box and result list | input-group pattern (`vl-input-group`, `vl-input-field`, `vl-button`), `vl-search-result`, `vl-pager` | — | Flux |
| FR-URI-06, FR-CN-05 | Error pages (`404`, `406`, `5xx`) | `vl-http-error-message` | — | Flux |
| FR-AC-01 *(phase 2)* | Login, logout, changing organisation | `vl-header-next` (Digitaal Vlaanderen's global header) | — | Flux |
| NFR-UI-02 | Skip link, keyboard operation, ARIA | `skip-to-content-id` on `vl-header-next` and `vl-functional-header`; Flux components' own accessibility | WAI-ARIA patterns in every new component | Flux + new |

In summary, Flux covers the page frame, search, errors, the login header, the front page and the portal. **Seven new components** are needed, all of them specific to Linked Data:
1. resource description;
2. RDF term;
3. incoming relations;
4. hierarchy tree;
5. collection table;
6. geometry map;
7. SPARQL editor.

The collection table and the geometry map are thin layers on Flux components (`vl-rich-data-table`, `vl-map`). The hierarchy tree and the SPARQL editor have no Flux base at all. Where the new components live is [open question 23](../06-open-questions.md#front-end).

## The same house style elsewhere: data.vlaanderen.be

Digitaal Vlaanderen's own Linked Data site shows the same house style built in a different way. We looked at one subject page, [`/doc/onderneming/0439543622`](https://data.vlaanderen.be/doc/onderneming/0439543622), on 4 October 2026:

- **No Flux and no custom elements.** The page is a Nuxt application (Vue 3.5) using Digitaal Vlaanderen's Vue implementation of the Webuniversum (`vl-ui-*` packages such as `vl-ui-properties`, `vl-ui-pager`, `vl-ui-tabs` and `vl-ui-search-filter`, and `vl-ui-ol-map` for maps). No `@domg-wc` package is loaded and no custom element is registered. The header and footer come from Digitaal Vlaanderen's widget client, the same global header that Flux's `vl-header` wraps.
- **Empty without JavaScript.** The server sends only empty header and footer containers and the page data as embedded JSON. All content is built in the browser. That is what NFR-UI-04 rules out for SPieGeL. This follows from the application's architecture (rendering in the browser), not from the component library. With Flux, the server can write the content into the page itself (see [constraint 2](#constraints-for-spiegel)).
- **One hand-built page per type.** The application has its own route and page for each resource type: `onderneming`, `vestiging`, `adres`, `gebouw`, `perceel`, `concept`, `conceptscheme` and about twenty more. Each page reads a JSON endpoint per type (for example `/doc/api/enterprise/{id}`). SPieGeL aims for the opposite: generic pages with presentation rules as configuration (FR-HTML-06). The JSON endpoints are, however, a working example of the server-side option in [open question 24](../06-open-questions.md#front-end).
- **Content negotiation, but no `303`.** The `doc` URI returns Turtle and JSON-LD for the matching `Accept` header. The `id` URI answers `200` directly, for HTML and Turtle alike, instead of `303 See Other` to `doc` (FR-URI-02). The response carries no `Vary`, `Link` or Content-Security-Policy header (FR-CN-04, NFR-UI-03).

**Why Flux suits data-driven subject pages.** In SPieGeL, the server decides from the data which blocks and components a page gets (FR-HTML-06). It then writes the matching Flux tags with the content inside them. The components only add styling and interaction in the browser. A generic page per resource therefore works with Flux. data.vlaanderen.be instead needs a hand-built Vue page per type, because its components exist only in the browser.

The comparison confirms the choice for Flux: SPieGeL gets the same look as the rest of the government, and its pages remain server-rendered, generic and usable without JavaScript.

## Consequences for the design

- **`spiegel-adapter-html`** gets a front-end build next to its Java code: Node.js, pnpm, a bundler, and the new components with their tests. Maven and Jenkins must run it (for example through the `frontend-maven-plugin`), and the build needs read access to the department's Artifactory for the `@domg-wc` scope. External contributors need that too.
- **Templates** generate Flux markup: tags, slots and CSS classes. The choice of template engine is still open ([open question 9](../06-open-questions.md#front-end)), but it must emit custom elements and attributes cleanly and encode output by context (NFR-SEC-05).
- **Tenant configuration** includes what Flux needs per domain: the page title, the links in the header, and, once access levels exist, the login and logout URLs.
- **Response headers** include a strict CSP alongside the existing open CORS policy (NFR-OPEN-01).
- **Geometries:** the server only writes the WKT literals, with their CRS, into the page. Conversion and reprojection happen in the browser, in the geometry map component, with OpenLayers and proj4 from the Flux map. There is no conversion service and no extra request.

See [NFR-UI-01 to NFR-UI-04](non-functional.md#user-interface) and [FR-HTML-07](functional.md#html).
