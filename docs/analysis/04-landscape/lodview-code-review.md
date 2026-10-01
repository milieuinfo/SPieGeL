# LodView code review

[LodView](https://github.com/LodLive/LodView) is the existing tool closest to SPieGeL's language (Java, Apache Jena). We therefore read its source code to find out how much of the predecessor's behaviour it already covers.

**Version reviewed:** upstream commit `e8768a5` (8 September 2023), the latest at the time of review. 14 Java classes (about 1,900 lines) and JSP views. Packaged as a WAR with XML-based Spring 4.2.4 configuration, Java 11 and Jena 4.4. All configuration is in one `conf.ttl` file. Each key can be overridden through `.env` (`LodView<key>`).

## Coverage of the predecessor's functions

| Predecessor function | LodView | Where in the code |
|---|---|---|
| `/doc/{concept}/{id}` as HTML and as RDF/XML, Turtle, N-Triples and JSON-LD | yes | `ResourceController`: `AcceptList` matching on `offeringRDF` / `offeringResources`; override with `?output=` |
| Own composed query per concept and media type | **no**: one `conf:defaultQueries` and one `conf:defaultRawDataQueries` for all resources | `conf.ttl`, `ResourceBuilder.buildHtmlResource` |
| HTML view per concept | **no**: one `resource.jsp` | `servlet-context.xml` |
| `303` from `/id/` to `/doc/` | **not built in**. The redirect supports only a suffix (`.html`) or a prefix (`/page/`), and the "pubby" strategy hard-codes `resource/`. It does work if the reverse proxy keeps doing the `303`, with `IRInamespace = …/id/` and `publicUrlPrefix = …/doc/` | `ResourceController.resource`, `Misc.toBrowsableUrl` |
| `/ns/{model}` | **only as a separate instance**: one `IRInamespace` per instance | `ConfigurationBean.populateBean` |
| Several domains in one service | **no**: one endpoint and one namespace per instance. Seven domains × (`id` + `ns`) means up to **14 deployments** | `ConfigurationBean.populateBean` |
| `/sparql` as a filtered proxy with HTML results | **no**: a `redirect:` to the store's own URL (the code has a TODO for proxy features), so the store would have to be public | `SPARQLController` |
| Access levels through OIDC | **no**: one set of basic-auth credentials per instance; no user authentication | `SPARQLEndPoint.doQuery` |
| Keyword search, reconciliation, Linked Data Fragments, dumps | **no**. For Virtuoso it adds links to `DESCRIBE` as CSV and OData, straight to the store | `ResourceController.addDataLinks` |
| Incoming relations | **yes**, with counts per predicate and paging | `LinkedResourcesController` (`/linkedResourceInverses`), `conf:defaultInverses*` |
| Blank nodes in outgoing relations | **yes**, up to four levels deep | `conf:defaultQueries` |
| Front-end components that need a same-origin `/sparql` | **no** | — |
| Skolem IRIs and ELI paths | **no**: outside `IRInamespace` | — |
| Caching | **no**: every request goes to the store | — |

## Points to note before using it in production

These are properties of LodView's design, relevant to anyone who deploys it on a public network. They are listed so that SPieGeL avoids them, not as criticism of a project that served many sites well.

1. **Caller-chosen remote fetching.** The `/rawdata` endpoint accepts the data source URL as a request parameter (`resourceRawController` → `SPARQLEndPoint.extractData`), and with `sparql=<>` it fetches an arbitrary `IRI` itself (`m.read(IRI)`). `/linkedResource` fetches remote resources by design, for LodLive. Deployments should block these paths. → SPieGeL: NFR-SEC-06.
2. **Queries built by string substitution.** `${IRI}` is replaced textually with the decoded request path (`SPARQLEndPoint.parseQuery`). → SPieGeL: NFR-SEC-04.
3. **Assumes `http://` IRIs.** Subjects that do not start with `http://` are treated as blank nodes (`SPARQLEndPoint.moreThenOneQuery`). The impact seems limited to the blank-node branches of the default query, but needs testing with `https://` data.
4. **Ageing stack.** Moving to Spring Boot 3 means migrating from `javax` to `jakarta`, from XML configuration to Boot, and from JSP to a template engine. In practice that is a rewrite. Controllers call Jena directly; there is no hexagonal separation.

## Conclusion

- **As a replacement for the predecessor: no.** The heavy parts are missing: several domains, access levels, the SPARQL proxy, search, the per-concept query catalogue and the front-end components.
- **For phase 1 (a single public catalogue): close.** One catalogue, no authentication, one SPARQL source, content negotiation and simple subject pages are what LodView does today. Only configurable URI templates with their own queries, and a modern design system, are missing. It could serve as a stopgap or demonstrator, provided the remote-fetching paths are blocked.
- **As a reference for SPieGeL: useful in small pieces.** The content negotiation logic (`AcceptList` matching), the default queries that expand blank nodes, and the incoming-relations behaviour with counts and paging are compact, proven solutions worth learning from. The architecture is not.
