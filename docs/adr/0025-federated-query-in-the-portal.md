# 0025. Offer federated queries in the portal, run by Comunica in the visitor's browser

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

Integration across domains is left to consumers ([ADR 0006](0006-resources-and-event-streams.md)): SPieGeL does not join data across domains, and its `/sparql` refuses `SERVICE` ([ADR 0011](0011-query-policy.md)). [Open question 31](../analysis/06-open-questions.md#architecture-and-operations) asked whether a federating query engine has a role on the consumer's side, and whether SPieGeL documents or offers one.

[Comunica](https://comunica.dev/) is a query engine for JavaScript (MIT). It answers one SPARQL query over several sources, such as SPARQL endpoints, by splitting it into subqueries per source and joining the results itself; it needs no `SERVICE`. It runs in the browser, with ready-made builds per major version and per fixed version. Release 5.2 (April 2026) focused on the robustness and performance of federated queries over SPARQL endpoints.

The portal page (FR-HTML-08) on the umbrella domain's host already knows every domain ([open question 25](../analysis/06-open-questions.md#product-and-scope)), and each domain's dataset description names its SPARQL endpoint (`void:sparqlEndpoint`, FR-META-01). Public endpoints send `Access-Control-Allow-Origin: *` (NFR-OPEN-01), which is what a client in the browser needs. Each domain host will have a SPARQL editor ([open question 37](../analysis/06-open-questions.md#front-end)).

A federated query becomes many requests: per triple pattern and per source, often after an `ASK` or a count to find out which source can answer. One query in the portal can send tens or hundreds of requests to every domain, and each domain limits each client ([ADR 0022](0022-edge-concerns.md)). Comunica can retry failed requests (`httpRetryCount`, `httpRetryDelay`), set time-outs and use a custom `fetch` function. Its documentation names no option to honour `Retry-After` or to bound the number of parallel requests.

A limit that is raised for federation traffic would need to recognise it, for example by a header or user agent, which any client can imitate.

## Decision

1. **The portal offers a federated SPARQL editor.** The query runs in the visitor's browser, with Comunica. SPieGeL runs no federated query on the server, and `/sparql` keeps refusing `SERVICE`.
2. **The sources are the public `/sparql` endpoints of the domains the portal lists**, taken from their dataset descriptions. A visitor may choose a subset. No credentials are sent: federation covers public data only.
3. **It is the same editor as on each domain's host** (question 37), with Comunica as its engine instead of a single endpoint.
4. **The portal makes Comunica a polite client**, with a `fetch` function of its own that:
    - sends only a few requests at a time to each domain;
    - on `429` or `503`, waits as long as `Retry-After` says, and then tries again;
    - stops after an overall time-out, which the visitor sees and can cancel sooner.
5. **SPieGeL makes no exception for federation.** Its rate limits are the same for every client. The limit for `/sparql` is chosen with federation in mind, and checked in a spike with typical federated queries.
6. **Comunica is a pinned version, served from the portal's own host**, and loaded only when the editor is opened. Nothing is fetched from a third-party CDN at run time. An update is a deliberate change, like any other dependency.
7. **It comes after `/sparql`**, which is phase 2.

## Consequences

- People can ask questions across domains in one place, without installing anything, and the department runs no federation service.
- ADR 0006 holds: data is joined on the consumer's side, in the consumer's browser.
- Federated queries are slower than queries on one store, and can be slowed down further by rate limits. The editor says so, and shows progress where Comunica allows it.
- The polite `fetch` is a small piece of code to maintain. It also shows other clients how to behave towards SPieGeL's limits.
- The portal page carries a large script, but only when the editor is opened. The rest of the portal stays readable without JavaScript (NFR-UI-04), and the CSP allows the script from the portal's own host only (NFR-UI-03).
- A domain that is down or slow delays or fails a federated query. The editor reports which domain did not answer.
- Federation over private data, with a token per domain, is not offered. It would need a decision of its own.
