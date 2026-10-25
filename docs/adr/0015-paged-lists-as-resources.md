# 0015. Serve large and follow-up data as paged Hydra lists

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

Two open questions meet here:

- [Open question 24](../analysis/06-open-questions.md#front-end): do the interactive components query `/sparql` from the browser, as the predecessor's do, or call endpoints on the server?
- [Open question 35](../analysis/06-open-questions.md#architecture-and-operations): which vocabulary describes the pages of a large property (FR-RA-07)?

FR-RA-07 already requires that a large property has a **paged list with its own URI**, and that HTML exploration (FR-HTML-05) uses the same pages, so that HTML and RDF follow one rule. The thin-layer components of [ADR 0014](0014-placement-of-linked-data-components.md) need exactly such lists:

- the **collection table** pages through the members of a collection, catalogue or organisation;
- **incoming relations** show the resources that refer to the subject, per predicate, with a count, and page through them. They are not part of the description ([ADR 0006](0006-resources-and-event-streams.md)), so FR-RA-07 did not cover them;
- the **hierarchy** (candidate `vl-cascader`, open question 39) loads the children of one node at a time, such as `skos:narrower` or `dcat:dataset`.

Queries built in the browser live outside the tenant configuration, cannot be cached per access level, and expose store dialects to the client. A separate JSON API per component would keep queries on the server, but adds a contract next to the RDF that only SPieGeL's own components can use.

The Flux team asked whether `vl-pager` can work without `total-items`, because SPieGeL asks for *N*+1 rows instead of counting. A `COUNT` on a large store is costly. Yet the mock-up of the incoming relations block shows a count per relation.

The [Hydra](https://www.hydra-cg.com/spec/latest/core/) vocabulary describes a page as a `hydra:PartialCollectionView` of a `hydra:Collection`, with `hydra:first`, `hydra:previous` and `hydra:next`, and an optional `hydra:totalItems`. [TREE](https://w3id.org/tree/specification) is the basis of event streams, and is made to fragment a collection by a value, such as time.

## Decision

1. **Follow-up data are paged lists, served as Linked Data.** The interactive components do not query `/sparql`, and SPieGeL offers no separate JSON API. A component requests the URI of a list as JSON-LD; a harvester can request the same list as Turtle.
2. **Lists use Hydra.** A list is a `hydra:Collection` whose members are `hydra:member`. A page is a `hydra:PartialCollectionView` with `hydra:first`, `hydra:previous` and `hydra:next`. The last page has no `hydra:next`. A component therefore knows from `hydra:next` whether there is another page, without a total. TREE remains the vocabulary of event streams.
3. **A list's URI is its subject's `doc` URI with reserved query parameters**, in the style of `_format` and `_profile` (FR-CN-02, FR-CN-03):

    | Kind of list | URI |
    |---|---|
    | Values of a property of the subject | `/doc/collectie/dossier?_property=co:element` |
    | Resources that refer to the subject through one predicate | `/doc/stof/benzeen?_inverse=imjv:stof` |
    | Overview of incoming predicates, with a count each | `/doc/stof/benzeen?_inverse` |
    | A further page | `…&_page=2` |

    Properties are prefixed names, with the prefixes the tenant configures (FR-SPARQL-03). No path segment is reserved, so lists cannot collide with URI templates, including hierarchical ones such as ELI.
4. **Which lists exist.** `_property` is answered for properties declared as large ([ADR 0012](0012-tenant-configuration-as-rdf.md)) and for the properties the components need, such as `skos:hasTopConcept`, `skos:narrower`, `dcat:dataset` and `dcat:catalog`, in the shared defaults. Other properties are part of the description and get `404` as a list. `_inverse` is answered for any predicate, because incoming relations are generic.
5. **A description links to its lists.** It holds the first *N* values of a large property and a `hydra:view` to the list (FR-RA-07).
6. **Each member comes with its label**, chosen by the tenant's language priority (FR-HTML-10), so that a table, a pager or a tree can show it without another request.
7. **Pages have a fixed size and a stable order.** The default size is 100, configurable per tenant and per property, with a maximum. Members are ordered by their IRI, so `_page=N` gives the same members within one data version. Each page is queried with `LIMIT N+1` and an offset, within the same timeout and ceiling as any query (NFR-OPS-02, NFR-OPS-09).
8. **Counts are bounded and cached.** A count runs up to a ceiling, for example 1,000, and is shown as "more than 1,000" above it. It is cached per data version (NFR-OPS-03). In RDF, `hydra:totalItems` is given only when the count is exact.
9. **Lists follow the same flow as descriptions:** tenant, access level, cache under the data version with the access level in the key, and bounded queries (NFR-SEC-03). Their queries are SPieGeL's default templates or a tenant's own `QuerySpec`s ([ADR 0012](0012-tenant-configuration-as-rdf.md)).
10. **JSON-LD for lists uses a fixed `@context`**, published by SPieGeL, so that a component reads plain JSON keys such as `member`, `next` and `label` instead of expanded IRIs.

## Consequences

- One mechanism serves the collection table, incoming relations, the hierarchy and harvesters. Its queries are configurable, cached and authorised like descriptions. No query is built in the browser.
- The answer to the Flux team: `vl-pager` needs no total. "Next" depends on `hydra:next`, and a count, when there is one, is bounded.
- Incoming relations become reachable for RDF clients without SPARQL, while staying out of the description itself, as ADR 0006 requires.
- Ordering and offsets cost more on deep pages of very large lists, because the store sorts before it skips. Caching per data version softens this. If it proves too slow, a cursor that continues after the last member can replace `_page` without changing the URIs that clients follow: they only follow `hydra:next`.
- `_inverse` for any predicate lets callers request costly lists. The timeout, the bounded pool and the rate limit (NFR-OPS-08) limit the damage.
- The fixed `@context` covers lists only. A `@context` for descriptions in general is a separate requirement that is still missing.
- Hydra is a Community Group draft that hardly moves. SPieGeL uses only its stable core terms.
