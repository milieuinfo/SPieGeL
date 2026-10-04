# Trifid code review

[Trifid](https://github.com/zazuko/trifid) (Zazuko) is the most mature and most widely used pure Linked Data front end in the [comparison](comparison.md). It is on a different stack (TypeScript on Node.js), so adopting it was never likely. We read its source code to find out what it actually does, how much of the predecessor's behaviour it covers, and which of its ideas are worth taking into SPieGeL.

**Version reviewed:** upstream commit `370eb49` (1 October 2026), `trifid` and `trifid-core` 6.1.0. A pnpm monorepo of twelve packages, about 6,600 lines of TypeScript excluding tests. Built on Fastify 5, Handlebars, the RDF/JS libraries (`@zazuko/env`, `sparql-http-client`) and OpenTelemetry. Apache-2.0. Shipped as npm packages and as a Docker image that starts from an example configuration.

## How it works

### Core: a configurable plug-in host

`trifid-core` contains no Linked Data logic at all. It reads one YAML or JSON5 configuration file with four sections (`server`, `globals`, `template`, `plugins`) and mounts each plug-in on Fastify routes (`lib/plugins/apply.ts`).

- **Plug-ins** are factories that receive the server, a logger, the merged configuration (`globals` overlaid with the plug-in's own `config`), a `render` function for Handlebars templates and a `query` function for the configured SPARQL endpoints. They return a route handler. Each entry in `plugins` names a module and can restrict it to `paths`, `methods` and `hosts` (Fastify route constraints). An `order` field sets the mounting order.
- **The same module can be mounted several times** under different names, each with its own paths and configuration. This is how a site gets several subject-page variants.
- **Configuration layering:** `extends` pulls in other configuration files recursively. Values can be resolved with `env:NAME`, `file:path` (relative to the configuration file) and `cwd:path`. `config/schema.json` validates the structure.
- **Built-in plug-ins:** static files, a Handlebars view, a fixed redirect, `/healthz` (always answers `OK`) and a plug-in that passes values to templates.
- **Cross-cutting concerns:** compression (including Turtle, N-Triples and JSON-LD), CORS, cookies, `Accept` parsing, a `subpath` for serving under a prefix, Pino logging, and OpenTelemetry traces and metrics (`otel.ts`).

### Entity renderer: subject pages and content negotiation

`@zazuko/trifid-entity-renderer` is the part that dereferences IRIs. By default it is mounted on `GET /*` and does the following for each request (`entity-renderer/index.ts`):

1. **Rebuild the IRI** from protocol, host, port and path, and drop the query string. If `datasetBaseUrl` is configured, swap the request origin for that base. This lets data with production IRIs be browsed on `localhost` or a test host. Several base URLs may be configured; they are tried in order.
2. **Check that the resource exists** with `resourceExistsQuery` (default `ASK { <{{iri}}> ?p ?o }`), once per base URL until one matches. If none matches, it returns `404`.
3. **Optional data-driven redirects** (`followRedirects`): a `SELECT` on the W3C *HTTP in RDF* vocabulary (2006 and 2011 versions) finds a stored `http:GetRequest` for the IRI and answers with its `http:responseCode` and `http:location`. With `enableSchemaUrlRedirect`, a `schema:URL` of type `xsd:anyURI` on the resource sends HTML clients to that URL.
4. **Fetch the description** with `resourceGraphQuery` (default `DESCRIBE <{{iri}}>`). If `resourceNoSlash` is set and the IRI ends in `/`, it is treated as a *container*: the subjects whose IRI starts with it are listed through a `FILTER REGEX`.
5. **Negotiate the format** (`lib/headers.ts`): `?format=ttl|jsonld|xml|nt|trig|csv|html` overrides `Accept`. Otherwise `mimeparse` picks the best of JSON-LD, N-Triples, RDF/XML, Turtle, TriG, CSV and HTML.
6. **Machine formats** are serialised from the parsed quad stream. CSV is a flattened key/value view of the JSON-LD.
7. **HTML** goes through a server-side rendered Lit web component (`@zazuko/rdf-entity-webcomponent` with `@lit-labs/ssr`). A *label loader* first fetches labels for referenced IRIs that have none in the description: in chunks of 30, two at a time, with a timeout and a cap of 1,000 terms. The page also embeds the description as JSON-LD and shows a count of triples per named graph. Callers can change the rendering through query parameters: `compactMode`, `technicalCues`, `embedNamedNodes`, `embedBlankNodes`, `maxLevel`, `lang` and others.

All queries and the template (`path`) are configuration. Every query goes to the `default` endpoint in `globals.endpoints`. In the shipped configurations that is `/query`, so the entity renderer queries **its own SPARQL proxy over HTTP**.

### SPARQL proxy

`@zazuko/trifid-plugin-sparql-proxy` (`sparql-proxy/index.ts`) mounts `/query` for `GET` and `POST`.

- It forwards the query as-is, as a `POST` form field, to the configured endpoint, adding basic-auth credentials or extra headers from the configuration. The response is streamed back with its status and content type, plus a `Server-Timing` header.
- **IRI rewriting:** when `rewrite` is on, the instance's origin is replaced by `datasetBaseUrl` in the query text, and the other way round in the response stream (`lib/ReplaceStream.ts`). Callers can toggle this with `?rewrite=true|false` (`allowRewriteToggle`, on by default).
- **Several named endpoints** can be configured. The caller picks one with `?endpoint=name`, which is then remembered in a cookie for 30 days.
- A `GET` without parameters returns a SPARQL **Service Description**. A worker thread fetches it from the upstream endpoint at start-up and filters it to the `sd:` vocabulary. If that fails, a minimal one is generated.

### Other packages

| Package | What it does |
|---|---|
| `trifid` | The ready-to-run server, plus example configurations (`docker-sparql`, `docker-fetch`, `tbbt`). |
| `trifid-handler-fetch` | Loads an RDF file (local or remote) into an in-memory Oxigraph store in a worker thread and serves SPARQL on it, with a 30-second timeout per query. Useful for demonstrations and small datasets. |
| `trifid-plugin-yasgui` | YASGUI query editor at `/sparql`, with two extra result views: a Leaflet map for `geo:wktLiteral` values and a pivot table. |
| `trifid-plugin-graph-explorer` | Zazuko's Graph Explorer (visual browsing), with SPARQL dialect presets. |
| `trifid-plugin-spex` | SPEX, a schema and data explorer. |
| `trifid-plugin-i18n` | Interface translations; the chosen language is kept in a cookie. |
| `trifid-markdown-content` | Serves directories of Markdown files as multilingual HTML pages. |
| `trifid-plugin-ckan` | DCAT-AP-CH export for the opendata.swiss CKAN harvester. |
| `trifid-plugin-iiif` | IIIF manifests; marked as no longer used. |

## Coverage of the predecessor's functions

| Predecessor function | Trifid | Where in the code |
|---|---|---|
| `/doc/{concept}/{id}` as HTML and as RDF/XML, Turtle, N-Triples and JSON-LD | yes, plus TriG and CSV; override with `?format=` | `entity-renderer/lib/headers.ts`, `core/lib/sparql.ts` |
| Own composed query per concept and media type | **partly**: one existence query and one description query per entity-renderer *instance*. Mounting one instance per path pattern gives each concept its own query, but not several queries per concept, and not different queries per media type | `entity-renderer/lib/config.ts`, `core/lib/plugins/apply.ts` |
| HTML view per concept | **yes**, through the instance's `path` template, but the description itself is always rendered by the generic web component | `entity-renderer/index.ts` |
| `303` from `/id/` to `/doc/` | **not as a rule**. The `redirect` plug-in has one fixed target. Redirects can be stored in the data per IRI (HTTP in RDF), which does not scale to a whole URI pattern | `core/plugins/redirect.ts`, `redirectQuery` |
| `/ns/{model}` | yes, as a separate entity-renderer instance on that path | — |
| Several domains in one service | **partly**. Plug-ins can be bound to host names, but the entity renderer and the label loader always query the single `default` endpoint, and its relative URL is resolved against the listener address rather than the request's host. In practice: one instance per domain | `core/lib/sparql.ts` (`initQuery`), `core/index.ts` |
| `/sparql` as a filtered proxy with HTML results | **proxy yes, filter no**. Every query is passed through unchanged; read-only behaviour depends entirely on the store's credentials. HTML results through YASGUI | `sparql-proxy/index.ts`, `yasgui` |
| Access levels through OIDC | **no**. No user authentication at all; the named endpoints are selectable by any caller | `sparql-proxy/index.ts` (`?endpoint=`) |
| Keyword search, reconciliation, Linked Data Fragments, dumps | **no** | — |
| Incoming relations | **no**, unless the store's `DESCRIBE` includes them or `resourceGraphQuery` is replaced | `entity-renderer/lib/config.ts` |
| Blank nodes in outgoing relations | **depends on the store's `DESCRIBE`**; the renderer embeds whatever blank nodes it receives | `renderer/entity.ts` (`embedBlankNodes`) |
| Front-end components that need a same-origin `/sparql` | **yes**: the proxy on `/query` (or another path) is same-origin | `sparql-proxy` |
| Skolem IRIs and ELI paths | **yes, generically**: any path under the base URL is dereferenced if it exists in the data | `entity-renderer/index.ts` |
| Caching | **no**: every request goes to the store, and no `Cache-Control`, `ETag` or `Vary` headers are set | — |

## Points to note before using it in production

As with the LodView review, these are properties of the design that matter on a public network. They are listed so that SPieGeL avoids them, not as criticism of a well-maintained project.

1. **Unfiltered SPARQL pass-through.** The proxy forwards any query text, with the configured credentials, and sets no timeout of its own on the upstream call. Protection against expensive queries and updates sits entirely with the store. → SPieGeL: NFR-SEC-01, NFR-OPS-02.
2. **Endpoints are chosen by the caller.** `?endpoint=` and the `endpointName` cookie select any configured endpoint. So endpoints cannot be used to separate public from protected data. → SPieGeL: FR-AC-03, NFR-SEC-02.
3. **Browser-side credentials for SPEX.** If `user` and `password` are configured for the SPEX plug-in, they are serialised into the HTML page for every visitor (`spex/index.ts`, `views/index.hbs`).
4. **Permissive CORS.** CORS is registered with `origin: true` and `credentials: true`, which reflects any origin and allows credentials. Trifid has no logins, so the impact is small, but this setting must not be copied into a service that has them. → SPieGeL: NFR-OPEN-01.
5. **Queries built by string substitution.** `{{iri}}` is replaced textually (`replaceIriInQuery`). The IRI is rebuilt through the WHATWG `URL` class, which percent-encodes `<`, `>`, `"` and spaces in paths, and we found no way to break out of `<…>`. That safety comes from URL normalisation, though, not from the query layer. In the container query the IRI ends up unescaped inside a regular expression, so regex metacharacters in a path change what matches. → SPieGeL: NFR-SEC-04.
6. **No `Vary: Accept` and no `406`.** HTML and RDF are served from the same URL without `Vary: Accept`, so a shared cache can return the wrong format. An unsupported `Accept` value falls back to HTML instead of `406`. → SPieGeL: FR-CN-05, NFR-SEC-03.
7. **Cost per page.** An HTML page takes at least an `ASK` (one per configured base URL until a match), a `DESCRIBE`, optionally a redirect `SELECT`, and up to 34 label queries (1,000 terms in chunks of 30). All go over HTTP through the instance's own proxy, and none are cached. The container query (`FILTER REGEX(STR(?s), "^…")`) scans every typed subject.
8. **Rewriting is plain text substitution.** The base URL is also replaced inside literals and in query text. `ReplaceStream` keeps everything after the last match in memory, so a large response that contains no match is buffered whole before it is sent. Its final flush also builds a regular expression from the base URL without escaping it.
9. **`/healthz` does not check anything.** It always answers `OK`, whatever the state of the store. → SPieGeL: NFR-OPS-04.
10. **The description depends on the store.** With the default `DESCRIBE`, what a subject page shows (blank nodes, incoming triples) differs between Fuseki, Virtuoso, Stardog and GraphDB. → SPieGeL keeps the description in its own queries (FR-RA-01, FR-RA-04).

## What SPieGeL can learn from it

These are the ideas worth taking over, with where they apply.

- **Base-URL rewriting for non-production environments.** Being able to browse and query data with production IRIs on a test or local host is useful for SPieGeL's own test and acceptance environments. SPieGeL should do it at the term level (rewriting IRIs in the parsed result and binding IRIs in the query) rather than with text substitution. Relates to FR-URI-01 and NFR-OPS-06.
- **Batched label lookup with limits.** Fetching labels for referenced IRIs in chunks, with bounded concurrency, a timeout and a cap on the number of terms, is a good pattern for readable subject pages that cannot overload the store. It is a concrete form of NFR-OPS-02 for FR-HTML-01.
- **Redirects as data.** Recording moved or retired URIs in the store with the W3C HTTP vocabulary lets data owners manage them without a deployment. That supports NFR-OPEN-02 (stable URIs). See [open question 18](../06-open-questions.md#architecture-and-operations).
- **Mount the same handler several times.** One generic subject-page handler with configuration per path pattern (queries and template) is close to SPieGeL's URI-template model (FR-URI-01, FR-RA-01, FR-HTML-03). SPieGeL adds what Trifid lacks: several queries per template, conditions on type and profile (FR-RA-03), and a tenant boundary (FR-MT-01).
- **Layered, file-based configuration.** `extends` together with `env:` and `file:` resolvers gives a shared base configuration with small overlays per environment or tenant. It fits NFR-OPS-06 and the "queries as files" approach of FR-RA-02.
- **Format override with short names.** `?format=ttl|jsonld|xml|nt|html` is a compact, linkable form of FR-CN-02.
- **Service Description on the SPARQL endpoint.** Answering a bare `GET` with an `sd:Service` description is cheap and helps clients. A candidate addition to FR-SPARQL-01.
- **Observability.** OpenTelemetry metrics per endpoint and format (`sparql_entities_dereferenced_total`, `sparql_queries_total`) and a `Server-Timing` header on proxied queries are good examples for NFR-OPS-05.
- **Useful extras on the query page.** A map view for `geo:wktLiteral` results and a pivot table are cheap features that suit spatial data. They are relevant to FR-SPARQL-03.
- **An in-memory store for demonstrations.** Serving a file through an embedded store (Oxigraph there; Jena's in-memory dataset for SPieGeL) makes a self-contained demonstration and test set-up easy.

## Conclusion

- **As a replacement for the predecessor: no.** Like LodView, it lacks access levels, a filtered SPARQL endpoint, search, several queries per concept and real multi-tenancy. On top of that it is outside the team's stack.
- **For phase 1 (a single public catalogue): yes, functionally.** One catalogue, one public store, content negotiation, subject pages, a SPARQL proxy and YASGUI are what Trifid does out of the box, and it is actively maintained. If a stopgap were needed, Trifid would be a better one than LodView, provided the store enforces read-only access and query limits, and the reverse proxy adds the `303`, caching and `Vary`.
- **As a reference for SPieGeL: valuable in its ideas, not in its code.** The plug-in host is a clean, small design, and the points under *What SPieGeL can learn from it* are proven in production. Its weak spots (text-substituted queries and rewriting, no caching, endpoints chosen by the caller) are exactly the areas where SPieGeL's requirements are stricter.
