# Comparison with existing open-source tools

Before building something new, we compared the open-source projects closest to SPieGeL's purpose. Activity and licence figures are as reported by the GitHub API on **1 October 2026**. "Last push" also counts automated commits, such as dependency updates.

## Candidates

| Project | Stack · licence | Last push · stars | What it does |
|---|---|---|---|
| [Trifid](https://github.com/zazuko/trifid) (Zazuko) | TypeScript / Node.js · Apache-2.0 | 2026-09-25 · 102 | Lightweight Linked Data server and proxy, inspired by Pubby. Plug-ins for an entity renderer (HTML subject pages), a SPARQL proxy, YASGUI and a graph explorer. Content negotiation; HTML templates per namespace pattern. See the [code review](trifid-code-review.md). |
| [Prez](https://github.com/RDFLib/prez) (RDFLib / Kurrawong) | Python (FastAPI) · BSD-3-Clause | 2026-09-30 · 39 | Data-configurable Linked Data API that delivers profiles of knowledge-graph data according to **Content Negotiation by Profile**. Reads from a read-only SPARQL endpoint and exposes that endpoint itself. Variants for SKOS (VocPrez) and GeoSPARQL / OGC API (SpacePrez). See the [code review](prez-code-review.md). |
| [ELDA](https://github.com/epimorphics/elda) (Epimorphics) | Java · Apache-2.0 | 2026-09-28 · 57 | Implementation of the *Linked Data API* specification. Endpoints with URI templates are configured in RDF; each becomes SPARQL against a triple store, with several output formats. See the [code review](elda-code-review.md). |
| [LodView](https://github.com/LodLive/LodView) | Java (Spring + Jena) · MIT | 2023-12-17 · 137 | IRI dereferencing following W3C practice, with HTML views of RDF resources. Follows Pubby's configuration approach. See the [code review](lodview-code-review.md). |
| [grlc](https://github.com/CLARIAH/grlc) (CLARIAH) | Python · MIT | 2026-10-01 · 151 | Generates a Web API (with an OpenAPI description) from SPARQL queries stored as files in a git repository: one query file is one API operation. No subject pages. |
| [Pubby](https://github.com/cygri/pubby) | Java · Apache-2.0 | 2018-02-26 · 92 · *archived* | The historical reference in this niche. No longer maintained. Listed because Trifid and LodView build on its ideas. |

## Against the requirements

The Trifid, Prez, ELDA and LodView columns were checked in the source code; the grlc column is based on its documentation. None of it is based on hands-on testing. Cells marked "partly" or "to check" need a short spike before any decision.

| Requirement | Trifid | Prez | ELDA | LodView | grlc |
|---|---|---|---|---|---|
| HTML subject pages and RDF content negotiation (FR-CN-01) | yes | RDF yes; HTML only through the separate Prez UI | yes | yes | no |
| Own query per URI template (FR-RA-01) | partly (one renderer instance per path pattern, one query each) | yes, generated from SHACL profiles per class | yes | no (one generic query) | yes |
| Content negotiation by profile (FR-CN-03) | no | **yes, core feature** | no, but named views per endpoint (`_view`) come close | no | no |
| Public SPARQL endpoint (FR-SPARQL-01) | yes (proxy plug-in, unfiltered pass-through) | yes (pass-through, off by default) | no (but `_where` and `_select` let callers add SPARQL fragments) | no (redirects to the store) | no |
| Several domains in one stateless instance (FR-MT-01) | partly (plug-ins per host name, but one default endpoint) | no (one store and one endpoint structure per instance) | partly (several specifications per web application, by path prefix, not host name) | no (one namespace and one endpoint per instance) | to check |
| Access levels mapped to separate read-only credentials (FR-AC-03) | no | no | no | no | no |
| Flemish URI standard, Skolem IRIs, ELI (FR-URI-02 to 05) | partly: Skolem and ELI paths resolve generically, `303` only per IRI in the data or via the edge | no: resources live under CURIE paths or `/object?iri=` | yes by configuration: generic `303` from item templates; Skolem and ELI paths as URI templates | partly: `303` only via the edge, `ns` needs a separate instance, no Skolem or ELI | no |
| Non-RDF sources via RML or R2RML (FR-DS-02) | no | no | no | no | no |
| Fits a Java / Spring Boot team | no | no | partly: Java 21, Jakarta and Jena 5, but a Jersey WAR, not Spring Boot | partly: Spring 4.2 WAR with JSP, not Boot | no |
| Actively maintained | yes | yes | yes | quiet since 2023 | yes |

## What this means

No project covers the combination SPieGeL needs. Two things are missing everywhere: **access levels** and **non-RDF sources** (through RML or R2RML). The closest projects each excel at something different:

- **ELDA** is conceptually closest to the predecessor's query catalogue: URI template → SPARQL query, configured declaratively, in Java. Its endpoint model and HTTP behaviour are worth adopting; see the [code review](elda-code-review.md#what-spiegel-can-learn-from-it).
- **Prez** is the only project built around content negotiation by profile. That is exactly what the predecessor *intended* with its format-dependent queries. Its profile model is the most reusable idea of all; see the [code review](prez-code-review.md#what-spiegel-can-learn-from-it).
- **Trifid** is the most mature and most widely used pure Linked Data front end, including a SPARQL proxy and YASGUI, but on a different stack. Several of its ideas are worth adopting; see the [code review](trifid-code-review.md#what-spiegel-can-learn-from-it).
- **LodView** fits the language but not the architecture, and is functionally narrow.
- **grlc** solves a different problem (queries as an API). Its idea of keeping queries as files in git is a good model for SPieGeL's per-tenant configuration.

## Options considered

1. **Reuse and contribute**, for example by adding access levels and multi-tenancy to Trifid or Prez. This means the least code of our own, but it is outside the team's Java stack, and the roadmap would depend on an outside maintainer.
2. **Reuse as a component**: an existing tool for public data, plus a small service of our own for protected data. This splits the problem along the authorisation boundary, at the cost of two stacks.
3. **Build our own (Spring Boot, hexagonal), adopting existing standards** rather than inventing configuration formats: the Linked Data API's configuration model (as in ELDA), W3C Content Negotiation by Profile (as in Prez), and queries as files in git (as in grlc). This is the only option that covers every requirement, and the most work.

SPieGeL takes option 3. Its distinguishing features are the four things no other project offers together:

1. a **hexagonal core** with pluggable data sources: SPARQL, and later non-RDF sources through RML;
2. **access levels** mapped to separate read-only credentials per source;
3. **several tenants** in one stateless instance;
4. **out-of-the-box conformance to the URI policy**, including dereferenceable Skolem IRIs and ELI paths.
