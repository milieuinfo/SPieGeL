# 0018. Minimise personal data from requests; leave personal data in the data to the store

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

Personal data reaches SPieGeL in two ways.

**In the published data.** Some domains hold personal data, such as the names of people behind a company. Whether it may be published, and at which access level, is decided by the data owner. The store and the loading chain keep personal data in separate graphs that only the accounts of the levels allowed to see them can read (NFR-SEC-02, [ADR 0011](0011-query-policy.md)). A public export starts from an allowlist of graphs ([open question 29](../analysis/06-open-questions.md#architecture-and-operations)). SPieGeL relies on this; it does not decide what is personal.

**In the requests.** Every request carries data that never passes through a graph:

- the visitor's IP address, which the Court of Justice of the EU treats as personal data (*Breyer*, C-582/14, 2016);
- what a visitor types: a search term (FR-SRCH-01) or a SPARQL query (FR-SPARQL-01), which may contain a name;
- from phase 2, the identity of an authenticated caller (FR-AC-01).

These end up only in SPieGeL's own logs, metrics and rate-limit counters (NFR-OPS-05, NFR-OPS-08). There, SPieGeL is responsible.

Logs serve real needs: tracing an error, finding slow queries, investigating abuse. They do not need the visitor's full address, what they searched for, or who they are by name. A plain hash of a short query that contains a name can be reversed by trying candidate names, so it is not enough to hide it.

Responses with personal data reach callers at higher access levels. SPieGeL caches them under the access level (NFR-SEC-03), marks them `private`, and invalidates them with the data version (NFR-OPS-03). Public responses may be kept by shared caches outside SPieGeL, such as proxies, which matters when something was published in error and is removed (FR-URI-10).

## Decision

1. **Personal data in the published data is the store's responsibility.** SPieGeL serves it only through the account of a level that may read it, and adds no other protection of its own. Data owners, the loading chain and the store keep it in separate, non-public graphs.
2. **SPieGeL minimises personal data from requests in its logs:**
    - IP addresses are truncated: IPv4 to /24, IPv6 to /48;
    - an authenticated caller is logged as a keyed hash (HMAC) of the token's subject, never by name or e-mail address;
    - a URL is logged as its path, the names of its query parameters, and only the values of `_format`, `_profile`, `_page`, `_property` and `_inverse`;
    - search terms and the text of SPARQL queries are not logged. A query on `/sparql` is logged as a keyed hash, with its duration, status and number of results, so that a recurring slow query can be recognised without its text;
    - the key of the hashes is a secret that changes at least as often as logs are deleted, so that old hashes cannot be linked to new ones.
3. **Rate-limit counters may use the full IP address**, because they must tell clients apart. They live only in memory or in the shared counter, with a lifetime of minutes, and are never written to a log.
4. **Metrics carry no personal data:** they are labelled by tenant, data source, resource type, status and media type, never by IRI, IP address or caller.
5. **How long logs are kept is configuration.** The value is set by the organisation that runs SPieGeL, with its data protection officer. SPieGeL ships a short default.
6. **Caching follows the access level.** A response at a non-public level is `Cache-Control: private` and is cached by SPieGeL only under its access level and data version. A public response may be kept by shared caches for a short time only, minutes by default and configurable per tenant, and is then revalidated with its `ETag`.
7. **No personal data in configuration or tests.** Example queries, tenant configuration and test fixtures use invented or public data, never real personal data. Reviews of the configuration repository check this ([ADR 0013](0013-tenant-configuration-delivery.md)).

## Consequences

- A log line is enough to trace an error through the request identifier ([ADR 0016](0016-problem-details-for-errors.md)), and to find slow or abusive patterns, but not to see who searched for whom.
- Investigating abuse from one address works through the rate limiter and the reverse proxy, not through SPieGeL's logs. The reverse proxy's own access log, with full addresses, is outside this decision; the organisation that runs it sets its own retention.
- A slow query can be recognised by its hash, but not read. To analyse it, someone must reproduce it, for example from the client who reported it.
- Short public caching means more revalidation requests. They are cheap, because a `304` from the data version needs no query (NFR-OPS-03), but shared caches help less than with a long lifetime.
- Removing personal data that was published in error is done in the store and the loading chain. SPieGeL's cache follows with the next data version, and shared caches within minutes.
- The secret for the hashes is one more secret to manage, from the platform's secret store, like the store credentials.
