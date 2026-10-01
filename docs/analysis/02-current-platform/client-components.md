# Client components

The predecessor's HTML pages embed web components from the shared front-end package [`omgeving-ld`](https://github.com/milieuinfo/linked-data) (Vue 2, version 1.7.27 at the time of writing). These components fetch further data **from the browser**, by sending SPARQL queries to the domain's own `/sparql` endpoint.

| Component | Shown as | Templates |
|---|---|---|
| `<ld-predicate inbound>` | "Incoming relations" panel | `objects-inbound.rq`, `objects-inbound-count.rq` |
| `<ld-predicate>` | Expandable groups of properties | `objects-outbound.rq`, `objects-outbound-count.rq` |
| `<ld-taxonomy>` | SKOS tree. Used automatically by `ld-predicate` for `skos:broader`, `narrower`, `topConceptOf`, `hasTopConcept` and `inScheme` | `taxonomy-up.rq`, `taxonomy-down.rq` |
| `<ld-dataset>` | DCAT catalogue tree | `dataset-up.rq`, `dataset-down.rq` |
| `<ld-data-table>` | Searchable, paged table of collection members | `list-by-pattern.rq`, `list-by-pattern-count.rq` |
| `<ld-sparql-form>` | SPARQL editor (YASQE) with example queries | none; navigates to `/sparql?query=…` |
| `<ld-search-form>` | Search box with example terms | none; navigates to `/keywordsearch?search=…` |

The literal templates are in the [Query catalogue](query-catalogue.md#browser-side).

## What SPieGeL must provide for these components

1. **A same-origin SPARQL endpoint** at `/sparql` that accepts `POST` with a `query` form field and returns `application/sparql-results+json`.
2. **The subject IRI embedded in the HTML** on an element with class `ld-subject` and an `about` attribute, because `ld-predicate` reads its subject from there.
3. **The template files** served at `/queries/{name}.rq`. The predecessor copies them from the package's build output into its static resources.
4. **Example files** per domain (`/txt/{domain}-sparql.txt`, `-search.txt`, `-lookup.txt`) for the editor and search form.

## Domain-specific explorer scripts

The HTML of three domains (`imjv`, `cbb`, `dba`) loads extra configuration and "explorer" scripts. Their source has not been found in `omgeving-ld`. Whether they are still needed is an [open question](../06-open-questions.md).

## Will SPieGeL keep these components?

That is not decided. The successor's HTML could be built with a different design system (FluxUI is under consideration) without these components. If the components go, their *functions* (incoming relations, tree browsing, collection tables) still need a home, either rendered on the server or rebuilt. → [Open questions](../06-open-questions.md)
