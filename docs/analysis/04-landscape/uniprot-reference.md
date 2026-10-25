# Reference: the UniProt SPARQL service

[UniProt](https://www.uniprot.org/) is not a tool to reuse, but a public SPARQL service that does several things SPieGeL needs, at a scale far beyond the department's domains: 244 billion triples in release 2026_03, served by [QLever](https://github.com/ad-freiburg/qlever) (a candidate store, see [open question 29](../06-open-questions.md#architecture-and-operations)). We looked at [`https://sparql.uniprot.org/`](https://sparql.uniprot.org/) on 7 October 2026. Its response times are on [Performance and availability](../03-requirements/performance.md).

## What it does

- **Example queries are the home page.** The page is mainly a list of example queries, each with a one-line description in plain language. A click opens the query in the editor, through a URL that carries the query (`/sparql/?query=…`).
- **Example queries are data.** Each example is a SHACL `sh:SPARQLExecutable` with the query text, an `rdfs:comment` and keywords. The examples are loaded into the endpoint itself, so they can be queried like any other data, and are published under `/.well-known/sparql-examples/`. This follows the convention of the [SIB SPARQL examples](https://github.com/sib-swiss/sparql-examples), shared by several life-science services.
- **The endpoint describes itself.** `GET /sparql` with `Accept: text/turtle` returns a [SPARQL 1.1 Service Description](https://www.w3.org/TR/sparql11-service-description/) of 474 kB: the result formats, features such as the union default graph and federated queries, the data version (`pav:version "2026_03"`) and its release date, and all 21 named graphs. **Per named graph it adds VoID statistics**: the number of triples, distinct subjects and objects, and class and property partitions. A client, or a person, can see what the data contains without running a single query. `/.well-known/void` itself answers with an empty body.
- **The same facts are readable on the page.** An *About* block gives the number of triples in this release, the release, the query timeout (45 minutes), that the default graph is the union of all graphs, and the number of named graphs. It links to the vocabulary, a tutorial, and statistics with diagrams. The licence (CC BY 4.0) is in the footer of every page.
- **Responses are versioned by release.** Responses carry `ETag: W/"2026_03"`, `Cache-Control: public` and an `Expires` one day later, so caches can keep them until the data changes.
- **Withdrawn entries stay available.** An `obsolete` named graph keeps entries that are no longer current.
- **The editor helps with prefixes.** Next to *Clear* and *Submit*, an *Add common prefixes* button inserts the usual `PREFIX` lines. The editor is YASQE.

## How it is built

UniProt has two separate front ends, built in opposite ways.

- **The SPARQL page** (`sparql.uniprot.org`) is plain XHTML, rendered on the server by SIB's Java web application behind Apache. Query results also come back as a server-rendered HTML table. In the browser there are two style sheets, the YASQE editor (YASGUI 2.11) from a CDN, and one small script without a framework (`sparql.js`) for the prefix and clear buttons. That script reads the prefixes **from the endpoint itself**, as SHACL `sh:prefix` and `sh:namespace` declarations, the same convention as the examples: prefixes are data too. This is the approach SPieGeL takes: content rendered on the server, JavaScript only for the editor (NFR-UI-04).
- **The main site** (`www.uniprot.org`) is a client-side application. Its HTML holds only an empty `<div id="root">`. Its [public code](https://github.com/ebi-uniprot/uniprot-website) (MIT) uses React 19 with TypeScript, React Router and webpack, with separate bundles for modern and older browsers, and its own design system (`franklin-sites`). The protein visualisations are web components (Nightingale and ProtVista, built with Lit), wrapped for React. Its data comes from a REST API (`rest.uniprot.org`), not from SPARQL. Like [data.vlaanderen.be](../03-requirements/design-system.md#the-same-house-style-elsewhere-datavlaanderenbe), it shows nothing without JavaScript.

SIB also publishes a newer editor, [`@sib-swiss/sparql-editor`](https://github.com/sib-swiss/sparql-editor) (MIT, last change August 2026). It is a web component built on YASGUI. It takes autocompletion for classes and properties from the endpoint's VoID description, filtered by the class of the subject at the cursor, the example queries from their SHACL descriptions, and the prefixes from `sh:prefix`. It needs no configuration beyond that metadata, so it works on any store whose endpoint publishes it. SPieGeL publishes exactly that metadata (FR-META-03, FR-HTML-15, FR-SPARQL-04), which makes the component a candidate for SPieGeL's SPARQL editor ([open question 37](../06-open-questions.md#front-end)).

## What SPieGeL takes from it

| UniProt | SPieGeL |
|---|---|
| Example queries as SHACL in git and in the endpoint, at `/.well-known/sparql-examples/` | FR-SPARQL-04 |
| Service Description with VoID statistics per named graph | FR-META-03, FR-META-01, FR-HTML-15 |
| Key facts (size, version, timeout, graphs, licence) readable on the page | FR-META-01, FR-META-02 |
| Entity tag per data release | NFR-OPS-03, [Performance and availability](../03-requirements/performance.md) |
| Withdrawn entries still described | FR-URI-09 |
| A URL per query; common prefixes in the editor, read from `sh:prefix` declarations | FR-SPARQL-03 |
| Content rendered on the server, JavaScript only for the editor | NFR-UI-04 |
| An editor that autocompletes from VoID and lists the SHACL examples (`@sib-swiss/sparql-editor`) | FR-SPARQL-03, [open question 37](../06-open-questions.md#front-end) |

What SPieGeL does **not** copy is the 45-minute timeout for anonymous queries. UniProt's store and hardware can afford it; SPieGeL keeps a configured timeout per tenant (NFR-PERF-03, NFR-OPS-02).

Example queries as data also serve a purpose beyond the editor: a set of questions in plain language with their correct SPARQL is ready-made context for applications that translate questions into queries ([open question 34](../06-open-questions.md#architecture-and-operations)).
