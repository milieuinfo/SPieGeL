# ELDA code review

[ELDA](https://github.com/epimorphics/elda) (Epimorphics) is the Java implementation of the [Linked Data API](https://github.com/UKGovLD/linked-data-api) (LDA) specification, developed for UK government open data. In the [comparison](comparison.md) it is conceptually closest to the predecessor's query catalogue: URI template → SPARQL query, configured declaratively, in Java. We read its source code to find out what it does, how much of the predecessor's behaviour it covers, and which of its ideas SPieGeL should adopt.

**Version reviewed:** upstream commit `62d85de` (2 October 2026), version 3.0.5-SNAPSHOT. About 31,000 lines of Java in the main module (`elda-lda`, including its own JSON-RDF library), plus 187 test classes. Release 3.0.0 moved it to Java 21, Jakarta EE 11 (Servlet 6, Tomcat 11), Jersey 3, Jena 5.6 and Velocity 2.4. Packaged as WARs (`elda-common` for deployment, `elda-standalone` with examples) and a Docker image. Apache-2.0 (the comparison previously listed an "own licence").

## How it works

### Configuration: an API specification in RDF

Everything is configured in Turtle with the LDA vocabulary (`api:`) and ELDA extensions (`elda:`). `elda-common` loads every file matching `/etc/elda/conf.d/{APP}/*.ttl` (`web.xml`). Each file can define one or more APIs, and modified files are reloaded without a restart (`RouterRestletSupport`). The example `environment.data.gov.uk-bwq.ttl` (bathing water quality, about 75 endpoints) shows the model:

- **An `api:API`** has a `api:sparqlEndpoint`, a `api:base`, default and maximum page sizes, languages, formatters, variables, cache settings (`elda:cacheExpiryTime "1h"`, `elda:enableETags`) and a list of endpoints.
- **An endpoint** has an `api:uriTemplate` (for example `/doc/bathing-water/{eubwid}`) and is one of two kinds:
    - an **`api:ItemEndpoint`** describes one resource, whose IRI is built from an `api:itemTemplate` (`http://environment.data.gov.uk/id/bathing-water/{eubwid}`);
    - an **`api:ListEndpoint`** selects a page of resources with an `api:selector`: a `api:where` fragment, `api:filter` (`type=BathingWater`), `api:orderBy` or `api:sort`.
- **Viewers** (`api:Viewer`) say what to show of each selected resource: `api:describeViewer` (a `DESCRIBE`), `api:labelledDescribeViewer`, `api:basicViewer`, or a list of **property chains** using short names, such as `name,type,broader.name,contributor.mbox`. An endpoint lists its viewers and a default; callers pick one with `_view=`.
- **Formatters** produce the output: JSON and XML in the LDA result format, Turtle, RDF/XML, JSON-LD, and HTML or CSV through an XSLT stylesheet or a Velocity template. Atom feeds are also available.
- **Variables** (`api:variable`) give typed values that can use URI-template parameters, such as `{scheme}` or `{eubwid}`.
- **Short names** (`api:label`, or derived from the vocabulary) map properties to readable names, used in viewers, filters and JSON output.
- **Credentials** are not in the specification. An endpoint names an `elda:authKey`, which refers to a separate properties file (`sources/AuthMap.java`). Basic authentication over plain `http:` is refused unless `elda:authAllowInsecure` is set (`SparqlSource`).

### Request flow

One JAX-RS resource handles every path (`restlets/RouterRestlet.java`):

1. **Match** the path against the URI templates of all loaded APIs. When several match, the most specific wins: more literal characters first, then more variables (`routing/MatchTemplate.java`). A format suffix (`/doc/x.ttl`) or `?_format=` selects a formatter directly.
2. **No match? Try the item templates in reverse.** If the path matches the path of an endpoint's `api:itemTemplate` (for example `/id/bathing-water/{eubwid}`), ELDA answers **`303 See Other`** to that endpoint's URI template (`/doc/bathing-water/{eubwid}`) (`routing/DefaultRouter.findItemURIPath`). This is the `id`→`doc` redirect, derived from configuration alone.
3. **Build the selection query** (`query/APIQuery.java`, `query/ContextQueryUpdater.java`). Request parameters become filters: `name=value` and `type.label=value` (by short name), `min-`, `max-`, `minEx-`, `maxEx-`, `exists-`, `lang-`, `_search` (text index), `near-lat`/`near-long`/`_distance`, `_sort`, `_page` and `_pageSize`. Values are rendered as SPARQL terms through a small term model (`rdfq/`): literals via Jena's `FmtUtils`, IRIs via `URIUtils.escapeAsURI`.
4. **Two phases.** A `SELECT` fetches one page of item IRIs. Then the view fetches the descriptions of exactly those IRIs, with one `CONSTRUCT` built from the property chains or one `DESCRIBE` (`core/View.java`). Both queries are parsed with Jena's `QueryFactory` before they are sent.
5. **Compose the page.** The result carries page metadata (`api:Page`, first, previous and next, the available views and formats) and, on request (`_metadata=all`), the bindings, the selection and view queries and execution times.
6. **Respond.** The output is rendered by the chosen formatter, with `Content-Location` (the format-specific URL), `Expires` (from the configured expiry time), `ETag` (if enabled), `Access-Control-Allow-Origin: *`, and `Vary: Accept` *only when the format was negotiated*, not when it came from a suffix or `_format`. An empty item endpoint returns `404`.

### Other functions

| Function | Where |
|---|---|
| Several data sources: remote SPARQL, a local file, TDB, data embedded in the specification (`here:`), and combinations of these | `sources/` |
| In-process caching of selections, descriptions, counts and responses, with policies limiting entries or triples | `cache/` |
| Total counts on request (`_count`, `elda:enableCounting`), themselves cached | `APIQuery.requestTotalCount` |
| Rewriting of result IRIs between a public and a local base (`rewriteURLFrom` / `rewriteURLTo`), and support for `X-Forwarded-*` headers | `RouterRestlet.makeRequestURI` |
| Licence and notice metadata on result pages | `licence/`, `metadata/` |
| Control pages: cache contents and clearing, statistics, the loaded configuration (`/control/*`, `/api-config`, `/meta/*`) | `restlets/` |
| Request logging with a response ID, JMX beans, Prometheus metrics (`/metrics`) | `support/LogRequestFilter.java`, `jmx/`, `metrics/` |

## Coverage of the predecessor's functions

| Predecessor function | ELDA | Where in the code |
|---|---|---|
| `/doc/{concept}/{id}` as HTML and as RDF/XML, Turtle, N-Triples and JSON-LD | **yes, except N-Triples**. Also LDA JSON and XML, CSV and Atom. Override by suffix or `_format` | `renderers/`, `RouterRestlet` |
| Own composed query per concept and media type | **per concept yes**: each endpoint has its own selector and viewers. **Per media type no**: the view is chosen independently of the format | `specs/APIEndpointSpec.java`, `core/View.java` |
| HTML view per concept | **yes**: a formatter (XSLT or Velocity) per API or per endpoint | `renderers/XSLT_RendererFactory.java`, `VelocityRendererFactory.java` |
| `303` from `/id/` to `/doc/` | **yes, generically**, derived from each item endpoint's `api:itemTemplate` | `routing/DefaultRouter.java` |
| `/ns/{model}` | **yes**, as ordinary endpoints (the example serves `/def/{scheme}` and `/def/{scheme}/{term}`) | example specification |
| Several domains in one service | **partly**: several specifications in one web application, each with its own endpoint and base, separated by path prefix. No selection by host name | `routing/`, `RouterRestletSupport` |
| `/sparql` as a filtered proxy with HTML results | **no**. A specification can link to a public endpoint. The `_where` and `_select` parameters (see below) are not a substitute | — |
| Access levels through OIDC | **no**. Only fixed basic-auth credentials per data source | `sources/SparqlSource.java` |
| Keyword search | **yes**, with a Jena text index (`_search`, `elda:textQueryProperty`) | `textsearch/`, `APIQuery.addSearchTriple` |
| Reconciliation, Linked Data Fragments, dumps | **no** | — |
| Incoming relations | **not built in**. Property chains follow outgoing links; incoming ones need a template view or a custom selector | `core/View.java` |
| Blank nodes in outgoing relations | **yes**, through property chains or the store's `DESCRIBE` | `core/View.java` |
| Front-end components that need a same-origin `/sparql` | **no** | — |
| Skolem IRIs and ELI paths | **yes, by configuration**: they are just URI templates | `routing/MatchTemplate.java` |
| Caching | **yes**: in-process caches with policies, plus `Expires`, `ETag` and a correct `Vary: Accept` | `cache/`, `RouterRestlet.standardHeaders` |

## Points to note before using it in production

As with the other reviews, these are listed so that SPieGeL avoids them, not as criticism of a project that has served UK government data for years.

1. **Callers can add SPARQL to queries.** As the LDA specification defines, the `_where`, `_select`, `_orderBy`, `_template` and `_graph` parameters pass SPARQL fragments, a whole selection query, a `CONSTRUCT` template or a graph name into the generated queries (`ContextQueryUpdater.handleReservedParameters`). `elda:allowReserved` only controls which *unknown* reserved parameters are ignored; it does not switch these off. Because every query is parsed by Jena as a `SELECT`, `CONSTRUCT` or `DESCRIBE`, updates are impossible. But the API is in effect an unrestricted query interface on the store. → SPieGeL: NFR-SEC-01, NFR-SEC-04, NFR-OPS-02.
2. **Variables are substituted as text.** Variable bindings are written into the assembled query string afterwards, not bound as terms, and not every kind of value goes through the same escaping. The term model in `rdfq/` is a step in the right direction, but it does not cover the whole query. → SPieGeL: NFR-SEC-04, with one query-building mechanism throughout.
3. **Control pages are public.** `/control/clear-cache` (including by `GET`), `/control/show-cache`, `/control/show-stats`, `/api-config` (the whole loaded configuration, including selectors) and `/meta/*` are served by the same application. Deployments must block them at the edge. → SPieGeL: NFR-SEC-08.
4. **No timeouts.** The Java `HttpClient` for SPARQL sources is built without connect or request timeouts, and no query timeout is set. Together with point 1, one expensive request ties up a thread until the store gives up. → SPieGeL: NFR-OPS-02.
5. **Error pages show exception messages.** Parse errors, Velocity errors and general failures are rendered with `e.getMessage()`, which can include query text (`RouterRestlet.runEndpoint`).
6. **`400` instead of `406`.** If no formatter fits the `Accept` header, the answer is `400 Bad Request`. → SPieGeL: FR-CN-05.
7. **In-process, per-replica caches.** The caches live in static registries in the JVM. That is fine for one node, but they are not shared and are cleared only by expiry or the control page. → SPieGeL: NFR-OPS-03.
8. **Architecture.** Query construction appends strings to `StringBuffer`s across a 1,100-line `APIQuery` class. Controllers, Jena and rendering are interwoven, and there is an `Attic` of retired code. Release 3.0 modernised the dependencies, not the design. → SPieGeL: NFR-Q-01.

## What SPieGeL can learn from it

- **The `303` comes from configuration.** Mapping an `id` template (the *item template*) to a `doc` endpoint, and deriving the redirect from that mapping, is exactly FR-URI-02 with no extra configuration. SPieGeL should do the same, and generate the `Link` back from `doc` to `id` as well.
- **Item endpoints and list endpoints.** Two endpoint kinds cover almost every page: one resource, or a page of selected resources. Their configuration (template, selector, viewers, formatter) is a good, proven shape for SPieGeL's tenant configuration (FR-URI-01, FR-RA-01, FR-HTML-03). See [open question 20](../06-open-questions.md#architecture-and-operations).
- **Select, then describe.** Fetching one page of IRIs first and then describing exactly those keeps every query bounded and makes both phases cacheable (NFR-OPS-02, NFR-OPS-03). It also suits listings and collection tables (FR-HTML-05).
- **Named views are profiles without the standard.** `_view=` with a default per endpoint is the predecessor's "different query per representation" in its simplest form. Combined with the Prez [profile model](prez-code-review.md#what-spiegel-can-learn-from-it), views can become `prof:Profile`s with `Accept-Profile` negotiation (FR-CN-03).
- **Property chains as a description language.** `name,type,broader.name` is short, readable and easy to validate. It is a lighter alternative to SHACL shapes (Prez) or hand-written queries (FR-RA-02) for the simple cases (see [open question 19](../06-open-questions.md#architecture-and-operations)).
- **HTTP caching done right.** `Vary: Accept` only on negotiated responses, `Content-Location` pointing to the format-specific URL, `Expires` from configuration, and an `ETag` from the result. SPieGeL should do all four (FR-CN-04, NFR-OPS-03).
- **Format suffixes as well as parameters.** `/doc/x.ttl` next to `?_format=ttl` gives stable, linkable URLs per format (FR-CN-02).
- **Short names for filtering.** Query parameters such as `type.label=…`, `min-date=…` and `exists-geometry=true` give a simple, safe filter language for listings, as long as it is the *only* way callers influence queries (unlike point 1).
- **Credentials by reference.** Naming a credential set by key (`elda:authKey`) and keeping its values in separate files is a ready-made pattern for access-level credentials per source (FR-AC-03, NFR-OPS-06). So is refusing basic authentication over plain HTTP.
- **Configuration from a directory, reloaded on change.** One file per API, picked up by a glob, fits "adding a domain needs configuration only" (FR-MT-02). Reloading on change is useful in development, but in production SPieGeL should prefer immutable configuration with a rolling restart (NFR-OPS-07).
- **Visible queries.** Page metadata that can include the selection and view queries, as in Prez, is valuable for diagnosis (behind NFR-SEC-08).
- **Several kinds of source.** Remote SPARQL, a local file, TDB and inline data behind one `Source` interface is close to SPieGeL's data-source port (FR-DS-01) and makes tests easy.

## Conclusion

- **As a replacement for the predecessor: no**, but it comes closer than any other candidate. It has URI templates with their own queries, a generic `303`, `ns` pages, HTML per endpoint, text search and caching. Missing: access levels, a filtered SPARQL endpoint, host-based multi-tenancy, profile negotiation per media type, and safe handling of caller input.
- **For phase 1 (a single public catalogue): possible.** It is Java and current on Jakarta and Jena 5. However, the reserved query parameters and the control pages would have to be blocked at the edge, and the HTML would need new XSLT or Velocity templates in the Flemish design system.
- **As a reference for SPieGeL: the closest model for configuration.** The LDA endpoint model (item and list endpoints, selectors, viewers, formatters, item templates) and its HTTP behaviour are worth adopting nearly as they are. The parts to leave behind are text-built queries, caller-supplied SPARQL fragments and the monolithic structure.
