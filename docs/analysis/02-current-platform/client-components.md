# Client components

The predecessor's HTML pages embed web components from the shared front-end package [`omgeving-ld`](https://github.com/milieuinfo/linked-data) (Vue 2, version 1.7.27 at the time of writing). These components fetch further data **from the browser**, by sending SPARQL queries to the domain's own `/sparql` endpoint.

| Component | Shown as | Templates |
|---|---|---|
| `<ld-predicate inbound>` | "Incoming relations" panel | `objects-inbound.rq`, `objects-inbound-count.rq` |
| `<ld-predicate>` | Expandable groups of properties | `objects-outbound.rq`, `objects-outbound-count.rq` |
| `<ld-taxonomy>` | SKOS tree. `ld-predicate` would switch to it for `skos:broader`, `narrower`, `topConceptOf`, `hasTopConcept` and `inScheme`, but in practice it is placed by type (see [Data-dependent HTML rendering](html-rendering.md#observations)) | `taxonomy-up.rq`, `taxonomy-down.rq` |
| `<ld-dataset>` | DCAT catalogue tree | `dataset-up.rq`, `dataset-down.rq` |
| `<ld-data-table>` | Searchable, paged table of collection members | `list-by-pattern.rq`, `list-by-pattern-count.rq` |
| `<ld-map>` | Map with one marker (OpenLayers, Lambert 72, GRB base map), from `lon`/`lat` or `x`/`y` attributes | none |
| `<ld-sparql-form>` | SPARQL editor (YASQE) with example queries | none; navigates to `/sparql?query=…` |
| `<ld-search-form>` | Search box with example terms | none; navigates to `/keywordsearch?search=…` |

The literal templates are in the [Query catalogue](query-catalogue.md#browser-side). The package also provides layout components that run no queries (`ld-view`, `ld-card`, `ld-collapsible`, `ld-subject`, `ld-predicate` without `endpoint`, `ld-object`, `flex-container`, `flex-item`). Which components appear on a page depends on the data; see [Data-dependent HTML rendering](html-rendering.md).

## The `omgeving-ld` package

**Version reviewed:** the source of `omgeving-ld` 1.7.27, repository [`milieuinfo/linked-data`](https://github.com/milieuinfo/linked-data), last change 13 August 2026.

### History and status

The package was started in 2019 as "Omgeving Linked Data UI": the changelog runs from 1.0.0 (April 2019) to 1.7.0 (June 2019). The repository itself starts in August 2020. Since then the changes have been small: colours, the URL of the base map service, a new layout of `ld-view` and the `ld-dataset` component (both September 2023), and a repair of `dataset-down.rq` (August 2026). The changelog stops at 1.7.0. Later versions are only release commits that increase the version number.

The package is marked `UNLICENSED` in `package.json`, although the repository is public. SPieGeL takes no code from it, only its behaviour as a specification.

### Technology

| Concern | Choice |
|---|---|
| Framework | Vue 2.5 with Vuex (shared state) and vue-resource (HTTP) |
| Build | webpack 4, Babel 6, Sass and Stylus. Node 10 is pinned, because `node-sass` 4.10 and webpack 4 do not run on later versions |
| Map | OpenLayers 5 with proj4 |
| SPARQL editor | YASQE from `@triply/yasgui` 4.2 |
| Icons and fonts | Material Design Icons, Lato and Flanders Art |
| Older browsers | `babel-polyfill`, loaded only when `Promise` or `Symbol` is missing, and a polyfill for `closest()` |

Vuetify is listed as a dependency and configured in webpack, but no component uses it.

### Build and distribution

- `npm run build` writes the bundles to `build/dist/` and copies the query templates to `build/queries/`. Both directories, and an older copy in `dist/`, are committed to the repository.
- A release increases the version, commits the build output and publishes the package to the npm registry. This used to run as a Bamboo plan in a Docker image with Volta. A Jenkins pipeline that does the same, with a Trivy scan of `package-lock.json`, was added in October 2026.
- `npm run app-dev` starts an Express server with Handlebars views (`src/views/*.hbs`) that show the components on example pages: a resource, a collection, an ontology, query results and a domain front page. `bin/build.js` saves these pages as a static demo in `demo/`.

### How a page uses it

The README describes the contract with the server-rendered HTML:

1. The server serves the bundles at `/dist/` and the query templates at `/queries/`. The base URL must be `/`; another path needs another build.
2. The page includes the initialisation snippet (`src/partials/init.html`) at the end of `body`. It hides the body until the styles are loaded, loads the polyfill if needed and then loads `/dist/index.js`.
3. The content sits in `<div id="content"><ld-view>…</ld-view></div>`. After 100 ms, `index.js` mounts one Vue application on `#content`. Markup outside `#content` is not touched.
4. The subject IRI is in an `about` attribute on `body` or on an `<ld-subject>`.
5. A `#header` element is replaced by the department header. A footer snippet (`src/partials/footer.html`) is available for the layout.

Components are registered globally and loaded lazily, in webpack chunks by purpose:

| Chunk | Components |
|---|---|
| `layout` | `department-header` |
| `ui` | `ld-view`, `ld-card`, `ld-card-title`, `ld-card-content`, `ld-accordion`, `ld-collapsible`, `flex-container`, `flex-item` |
| `data` | `ld-subject`, `ld-predicate`, `ld-object`, `ld-taxonomy` |
| `api-explorer` | `ld-lookup-form`, `ld-sparql-form`, `ld-search-form` |
| `data-table` | `ld-data-table` |
| `map` | `ld-map` |

`ld-dataset` is missing from this list; see [Observations](#observations).

### Shared behaviour

- **Query templates.** A component fetches its `.rq` file from `/queries/` and fills in placeholders written as `%name%` with its own properties: `%subject%`, `%predicate%`, `%from%`, `%filters%`, `%sortField%`, `%sortDirection%`, `%pageSize%`, `%offset%`. If a placeholder has no value yet, the component tries again after 500 ms.
- **Requests.** Queries go to the component's `endpoint` attribute as `POST` with a form-encoded `query` field, with credentials and `Accept: application/sparql-results+json`. A newer request with the same query text aborts the older one. Errors are ignored and shown as an empty result.
- **Cache.** Results are kept in a Vuex store keyed on the full query text and shared by all components on the page, for as long as the page is open.
- **Paging and search.** Every list query has a count query next to it. Search terms are split on whitespace and turned into a `FILTER` of case-insensitive `regex()` calls on the configured `search-fields`. The search term is inserted into the query text in the browser.
- **Local links.** `ld-object`, `ld-taxonomy` and `ld-data-table` rewrite a left-click on a link in the subject's own URI space to a local path (`https://data.…/id/x` becomes `/id/x`), so that a test or local environment stays on its own host. Copying the link or opening it with a right-click still gives the official URI.
- **Map.** `ld-map` draws one marker on the GRB base map from `geo.api.vlaanderen.be` (WMTS, Lambert 72, `EPSG:31370`). WGS84 coordinates are transformed in the browser.
- **Example files.** The three forms read examples from a text file. For queries, a line that starts with `##` gives the label of the query that follows. For lookups, each line is an IRI followed by a label.

### Observations

1. **`ld-dataset` is not in the bundle.** `src/vue/ld-dataset.vue` exists and was repaired in August 2026, but `src/js/index.js` does not register it, and neither `build/dist/` nor `dist/` contains it or the `dataset-*.rq` templates. Unless the predecessor registers it elsewhere, the "Browse dataset" card that the XSLT renders for `dcat:Catalog` and `dcat:Dataset` (see [Data-dependent HTML rendering](html-rendering.md)) stays empty. This has to be checked on a live page ([open question 38](../06-open-questions.md#front-end)). For SPieGeL it does not change the requirement: FR-HTML-05 asks for the tree either way.
2. **Unused templates.** `list-by-type.rq` and its count query (members by `rdf:type`) are still shipped, but the XSLT only uses `list-by-pattern.rq` (members through `dct:relation`).
3. **Unrelated content.** `src/scripts/` holds two shell scripts that download documents from the `dsi` domain with SPARQL and `curl`. They are copied into the build output but have nothing to do with the components.
4. **The domain explorer scripts are not here either.** The `api-explorer` chunk holds the three form components, not the domain-specific scripts described [below](#domain-specific-explorer-scripts).
5. **Hard-wired paths.** Template paths (`/queries/…`), the bundle path (`/dist/`) and the base map URL are fixed in the code. Only the endpoint and the example files are attributes.
6. **Ageing tool chain.** Node 10, webpack 4, Babel 6 and Vue 2 have all reached end of life. Rebuilding the package already needs a pinned Node 10 image.

### Lessons for SPieGeL

- Keep the division of labour: the server renders the description and the first values, and the browser loads only what can be large (incoming relations, tree levels, collection members). This is what FR-HTML-05 asks for.
- Pass search terms and IRIs to the server as parameters, not as SPARQL text built in the browser. This is part of [open question 24](../06-open-questions.md#front-end).
- Make every list come with a count and a stable sort order, as the predecessor's templates do.
- Keep the local-link behaviour, so that test environments do not send users to production. In SPieGeL this can happen on the server, when the HTML is rendered.
- Do not commit build output to the source repository, and do not depend on a tool chain that only runs on an old runtime.

## What SPieGeL must provide for these components

1. **A same-origin SPARQL endpoint** at `/sparql` that accepts `POST` with a `query` form field and returns `application/sparql-results+json`.
2. **The subject IRI embedded in the HTML** on an element with class `ld-subject` and an `about` attribute, because `ld-predicate` reads its subject from there.
3. **The template files** served at `/queries/{name}.rq`. The predecessor copies them from the package's build output into its static resources.
4. **Example files** per domain (`/txt/{domain}-sparql.txt`, `-search.txt`, `-lookup.txt`) for the editor and search form.

## Domain-specific explorer scripts

The HTML of three domains (`imjv`, `cbb`, `dba`) loads extra configuration and "explorer" scripts. Their source is not in `omgeving-ld`. Whether they are still needed is an [open question](../06-open-questions.md).

## Will SPieGeL keep these components?

No, not as they are. SPieGeL's HTML must comply with the Flux design system ([ADR 0005](../../adr/0005-flux-design-system-for-html.md)), and these components are Vue 2 components outside Flux. Their *functions* (incoming relations, tree browsing, collection tables, the map, the SPARQL editor) still need a home: Flux components where they exist, and new components where they do not. See [Design system: Flux](../03-requirements/design-system.md#from-omgeving-ld-to-flux). Whether the `/sparql` and HTML conventions for the old components must stay for a transition period is [open question 8](../06-open-questions.md#front-end).
