# 0022. Keep TLS and flood protection at the edge; move the 303, CORS and real rate limits into SPieGeL

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

In the predecessor, a reverse proxy per domain maps the public host name to the domain, answers `/id/` requests with the `303` redirect, and adds an open CORS policy, HSTS and some input filtering ([What sits in front of it](../analysis/02-current-platform/functional-overview.md#what-sits-in-front-of-it)). The application has its own `303` as well, but public traffic never reaches it. Which of these concerns move into SPieGeL was [open question 13](../analysis/06-open-questions.md#architecture-and-operations). Rate limiting (NFR-OPS-08) is a new concern for the same choice.

Several decisions already point one way:

- **The URI templates are tenant configuration** ([ADR 0012](0012-tenant-configuration-as-rdf.md)). A proxy that answers the `303` needs its own copy of them.
- **CORS must be set in exactly one place.** If the proxy and the application both add `Access-Control-Allow-Origin`, the browser receives the header twice and rejects the response. Only SPieGeL knows whether a response is public; from phase 2 some are not (NFR-OPEN-01).
- **TLS ends at the proxy**, and HSTS belongs to the host's TLS ([request flow, step 0](../analysis/05-architecture/request-flow.md#steps)).
- **SPieGeL encodes output and does not filter input by character lists** (NFR-SEC-05). It validates what it uses: template variables by pattern ([ADR 0012](0012-tenant-configuration-as-rdf.md)), queries by the query policy ([ADR 0011](0011-query-policy.md)), parameters such as `_page` ([ADR 0016](0016-problem-details-for-errors.md)). A content filter at the edge would block legitimate SPARQL, in which `<`, `SELECT` or `FILTER` are normal.

Rate limiting is a real choice. The proxy stops load before any work is done, but knows only addresses: not the tenant, the kind of request or, from phase 2, the caller's identity, and it cannot answer with `problem+json`. SPieGeL knows all of these, but a flood reaches it first, and its limits must hold across nodes without sticky sessions (NFR-OPS-01, NFR-OPS-08). A counter shared exactly between nodes needs a shared store, such as Redis or Valkey: one more stateful component, and one more thing that can fail. A counter per node, with a share of the limit, needs nothing, but is approximate.

In phase 1 every caller is anonymous, and the proxy is the first defence. SPieGeL's own limits are there to share the data sources fairly, for example so that one client cannot hold every slot for `/sparql`, not to count exactly.

## Decision

1. **SPieGeL answers the `303`** for every `id` URI, from the tenant's URI templates (request flow, step 5). The proxy forwards `/id/` requests like any other.
2. **SPieGeL sets the CORS headers, and only SPieGeL.** The proxy adds none and removes none.
3. **The proxy terminates TLS and sets HSTS.** It passes the original host, scheme and client address in `X-Forwarded-*` headers. SPieGeL trusts those headers only from the configured addresses of the proxy.
4. **The proxy sets generic limits only:** the size of the request, the URL and the headers, connection limits and time-outs. It does not filter on content.
5. **Rate limiting has two layers, each with its own role:**
    - the **proxy** protects against floods, with a generous limit per client address, well above SPieGeL's own limits;
    - **SPieGeL** sets the real limits: per client, per tenant and per kind of request, with a stricter limit for `/sparql`, answered with `429`, `Retry-After` and `problem+json` (request flow, step 3a). The limits are tenant configuration, with shared defaults ([ADR 0012](0012-tenant-configuration-as-rdf.md)).
6. **A client is identified** by its address as passed on by the proxy, and from phase 2 by its identity once it is authenticated. Counters never reach a log ([ADR 0018](0018-personal-data-in-requests-and-caches.md)).
7. **SPieGeL counts behind a port, `RateLimitPort`.**
    - The **first adapter**, for phase 1, counts in memory on each node, up to the limit divided by the number of nodes. That number is deployment configuration.
    - A **second adapter** counts in a shared store, once one exists, for example for the shared cache of [ADR 0020](0020-cache-key-and-entity-tag.md) or for limits per identity in phase 2.
    - If the shared store fails, it **fails open, falling back to the count per node**: a helper component never takes the service down, and a limit remains.

## Consequences

- The URI templates, CORS and the limits per tenant live in one place, the tenant configuration, and are tested with SPieGeL. A change to a tenant's limits needs no change at the proxy.
- The proxy's configuration becomes the same for every tenant: TLS, HSTS, forwarding headers, generic limits and flood protection. A new tenant needs only its host name and certificate there.
- In phase 1, SPieGeL's limits hold across nodes only approximately: a client whose requests happen to land on one node meets its limit a little earlier. NFR-OPS-08 says so. With autoscaling, the configured number of nodes may differ from the real one for a while; the limit is then briefly too strict or too loose.
- A flood is stopped at the proxy only above its generous limit. Below it, the load reaches SPieGeL, where the rate limit and the bounded pools of NFR-OPS-02 contain it.
- Moving to a shared store is a change of adapter, not of the core or the configuration.
- During migration, the predecessor's proxy rules for the `303`, CORS and input filtering must be removed for hosts served by SPieGeL. Otherwise CORS headers appear twice.
