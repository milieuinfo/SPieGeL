# 0019. Advertise entry points to search engines, with schema.org for datasets and terms

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

Three kinds of finder look for what SPieGeL publishes, and each reads something different:

- **search engines** read `robots.txt`, sitemaps, the HTML and schema.org in JSON-LD;
- **dataset search services**, such as Google Dataset Search, data.europa.eu and the Flemish open data catalogue, harvest DCAT or read schema.org `Dataset`. SPieGeL already describes each domain in DCAT and VoID (FR-META-01);
- **link previews**, in chat tools and social media, read Open Graph tags. Subject pages are the landing pages for links from elsewhere ([ADR 0007](0007-uri-as-shared-identifier.md)), so a shared URI should preview well.

Machines already find the other formats of a page: the HTML has `<link rel="alternate">` to its RDF formats (FR-HTML-04), and every response has `Link` headers with `rel="alternate"` per format and profile (FR-CN-04). A thing's other distributions, such as its feature in OGC API Features, appear only as visible links (FR-HTML-13, [ADR 0007](0007-uri-as-shared-identifier.md)).

The HTML also embeds the domain's own description as JSON-LD (FR-HTML-04). Search engines ignore terms they do not know; they understand schema.org. The [DCAT 3](https://www.w3.org/TR/vocab-dcat-3/) specification includes a mapping from DCAT to schema.org. SKOS concepts and schemes correspond to `schema:DefinedTerm` and `schema:DefinedTermSet`.

Advertising every resource has a cost. The predecessor's logs already show many crawlers, each fetching different resources in every format ([Performance and availability](../analysis/03-requirements/performance.md#the-predecessor-today)). A sitemap of every subject would mean listing all subjects for each data version, and would draw more crawling to pages, such as a single measurement, that search engines often do not index anyway. Variants of one page, such as `?_format=ttl`, `?lang=en`, or the paged lists of [ADR 0015](0015-paged-lists-as-resources.md), would be indexed as duplicates.

Search engines index reliably what is in the HTML as it is sent. This depends on rendering on the server ([open question 40](../analysis/06-open-questions.md#front-end)).

## Decision

1. **A sitemap lists the entry points, not every resource.** `/sitemap.xml` on each tenant's host lists the `doc` URIs of the front page, catalogues and datasets, concept schemes, collections and vocabulary (`ns`) pages. Which resource types are entry points is configurable per tenant ([ADR 0012](0012-tenant-configuration-as-rdf.md)); the shared defaults name `dcat:Catalog`, `dcat:Dataset`, `skos:ConceptScheme`, `co:Collection` and `owl:Ontology`. The sitemap is built with bounded queries, like a front-page block, and cached per data version. It is split into files of at most 50,000 URLs, with a sitemap index when needed, and carries the data version's release date as `lastmod`. Other pages remain reachable by following links.
2. **`robots.txt` per tenant** names the sitemap and excludes `/sparql` and the paged lists (`?_property`, `?_inverse`, `?_page`). It never excludes the format variants (`.ttl`, `?_format=…`), so that Linked Data crawlers can follow the alternates. A tenant can extend it. **Outside production, `robots.txt` excludes everything by default**, so that test environments are not indexed.
3. **Each page names its canonical URL**: `<link rel="canonical">` points to the `doc` URI without format, language or paging parameters. Canonical and alternates complement each other: the canonical names the page, the alternates name the same content in other formats.
4. **Alternates also point to the thing's other distributions.** Where a link of FR-HTML-13 has a media type, such as `application/geo+json` for a feature in OGC API Features, it is also given as `<link rel="alternate" type="…">` in the HTML and as a `Link` header on every format. Every page also links to its domain's dataset description with `rel="describedby"` (FR-META-01). Machines can then follow a thing from SPieGeL to its other forms, as ADR 0007 intends for people.
5. **Responses to authenticated callers are not indexed**: they carry `X-Robots-Tag: noindex`.
6. **SPieGeL adds schema.org in a separate JSON-LD block**, next to the domain's own description, which stays unchanged (FR-HTML-04). The translation is a fixed table in SPieGeL, not configuration:
    - `dcat:Dataset` and `dcat:Catalog` become `schema:Dataset` and `schema:DataCatalog`, following the DCAT 3 mapping: name, description, licence, publisher, keywords, distributions;
    - `skos:Concept` and `skos:ConceptScheme` become `schema:DefinedTerm` and `schema:DefinedTermSet`;
    - every other page gets `name`, `description`, `url` and `identifier` (its `id` URI).
7. **Every page has Open Graph tags**: `og:title` (the label), `og:description`, `og:url` (the canonical URL) and `og:type`.

## Consequences

- Datasets and code lists, the pages people actually search for, can be found in search engines and in Google Dataset Search, without configuration per tenant.
- Crawling is not invited to every measurement or emission record, which keeps load in proportion. Those pages can still be found through links and through the dataset descriptions.
- The schema.org block is a lossy summary for search engines. The domain's own JSON-LD, the RDF formats and the dataset description stay the full sources.
- The translation table is SPieGeL's responsibility: when schema.org or the DCAT mapping changes, SPieGeL follows.
- Server-side rendering becomes one more criterion for open question 40: a page built in the browser is indexed later and less reliably.
- Test environments with production IRIs are kept out of search engines by default; a tenant that wants otherwise must say so explicitly.
- A thing's other distributions become discoverable by machines, not only by people. The link templates of FR-HTML-13 must then say which media type each link has. A link to an event stream (open question 30) can be added the same way later.
