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

## What SPieGeL must provide for these components

1. **A same-origin SPARQL endpoint** at `/sparql` that accepts `POST` with a `query` form field and returns `application/sparql-results+json`.
2. **The subject IRI embedded in the HTML** on an element with class `ld-subject` and an `about` attribute, because `ld-predicate` reads its subject from there.
3. **The template files** served at `/queries/{name}.rq`. The predecessor copies them from the package's build output into its static resources.
4. **Example files** per domain (`/txt/{domain}-sparql.txt`, `-search.txt`, `-lookup.txt`) for the editor and search form.

## Domain-specific explorer scripts

The HTML of three domains (`imjv`, `cbb`, `dba`) loads extra configuration and "explorer" scripts. Their source has not been found in `omgeving-ld`. Whether they are still needed is an [open question](../06-open-questions.md).

## Will SPieGeL keep these components?

No, not as they are. SPieGeL's HTML must comply with the Flux design system ([ADR 0005](../../adr/0005-flux-design-system-for-html.md)), and these components are Vue 2 components outside Flux. Their *functions* (incoming relations, tree browsing, collection tables, the map, the SPARQL editor) still need a home: Flux components where they exist, and new components where they do not. See [Design system: Flux](../03-requirements/design-system.md#from-omgeving-ld-to-flux). Whether the `/sparql` and HTML conventions for the old components must stay for a transition period is [open question 8](../06-open-questions.md#front-end).
