# 0011. Check every query against a query policy before it runs

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

Two kinds of SPARQL reach a store through SPieGeL. **Callers' queries** arrive on `/sparql` (FR-SPARQL-01). Phase 2 makes that endpoint public, for every access level. **Tenant queries** are `.rq` files in the tenant configuration (FR-RA-02), written by people but loaded without review at run time.

The security boundary is the store: each access level reads through its own read-only account, which can only see that level's graphs (NFR-SEC-01, NFR-SEC-02). Checks in SPieGeL are defence in depth. [Open question 29](../analysis/06-open-questions.md#architecture-and-operations) showed that restricting graphs by **rewriting** queries is too weak to be the boundary, because one missed construct leaks data. Still, a query that can only do harm should be refused before it reaches the store:

- an update (`INSERT DATA`, `DELETE`, `LOAD`, `CLEAR`, `DROP`);
- a `SERVICE` call, which would make the store contact a host chosen by the caller, possibly an internal one (NFR-SEC-06).

A `FROM`, `FROM NAMED` or `GRAPH` that names a graph the level may not see is a different case. Refusing it would need a list of graphs per level in the tenant configuration, kept in step with the store by hand, and duplicating permissions that the store already enforces.

Checking query text with regular expressions is unreliable: comments, prefixes, escapes and line breaks defeat it. A parsed query can be checked reliably.

Not every store enforces graph permissions. Virtuoso does, with an account per level, and so does Fuseki with its access control; QLever and Oxigraph do not (question 29). A query on graphs, whether it names them or uses `GRAPH ?g`, is only safe on a store that limits what the level's account can see. A store without graph permissions is only safe if it holds nothing but what one level may see.

The core does not depend on `jena-arq` ([ADR 0010](0010-jena-graph-types-in-the-core.md)). Adapters do not depend on each other, which `ArchitectureTest` enforces. A check written once must therefore be reached through a port, not shared between the web adapter and the configuration adapter.

## Decision

1. **The core defines a `QueryPolicy`**, as data, per tenant:
    - the allowed query forms: `SELECT`, `CONSTRUCT`, `ASK` and `DESCRIBE`;
    - `SERVICE` is not allowed.

    The policy names no graphs. Adding a graph to a store needs no change to the configuration.
2. **The core defines an outbound port `QueryInspector`**, which checks query text against a `QueryPolicy` and returns the violations, if any. `spiegel-adapter-sparql` implements it once:
    - it parses with `QueryFactory` using strict SPARQL 1.1 syntax (`Syntax.syntaxSPARQL_11`), so an update fails as a parse error;
    - it checks the query form;
    - it walks the syntax tree with an `ElementWalker`, and rejects every `SERVICE`, also when nested in a subquery, `OPTIONAL` or `UNION`;
    - it never matches on query text.

    Parsing knows the prefixes the tenant configures (FR-SPARQL-03). A store may predefine prefixes, such as Virtuoso's `bif:`; without them, queries that work on the store would fail to parse.
3. **The checks run in the core, so they cannot be bypassed.** `RunSparqlQuery` calls the inspector before every query on `/sparql` and refuses a violation with `400`. Loading a tenant configuration calls the same inspector for every `.rq` file, and refuses the whole configuration if one violates the policy. The web adapter only passes the query text on.
4. **The store decides which graphs a level sees.** `FROM`, `FROM NAMED` and `GRAPH`, with an IRI or a variable, are not checked by SPieGeL: the level's account, or the store's own instance per level, only holds what the level may see. Each data source declares in the tenant configuration whether it **enforces graph permissions**: yes for a store with an account per level that can only read that level's graphs (Virtuoso, Fuseki with access control), no for a store without them (QLever, Oxigraph). The SPARQL adapter cannot detect this itself, so it is declared, and the adapter passes it on to the core.
5. **A store without graph permissions serves one access level only.** When a tenant configuration is loaded, the core refuses it if a data source that does not enforce graph permissions is shared by more than one access level. Such a store needs its own instance or index per level, as option 1 or 4 of question 29 describes.
6. **Queries are never rewritten.** SPieGeL refuses or passes a query; it does not add filters or `VALUES` to restrict graphs.
7. **The SPARQL adapter enforces a timeout and a maximum number of results** on every query, configured per tenant (NFR-OPS-02, NFR-PERF-03). An unbounded property path is not refused when parsing, because hierarchies need paths such as `skos:broader*` (FR-HTML-05). The timeout stops it.
8. **The README states** that each data source must be reached with a read-only account (NFR-SEC-01).

## Acceptance tests

The implementation includes tests with these queries and configurations. Each test names this ADR and the requirement it covers.

| Input | Expected |
|---|---|
| `INSERT DATA { … }` on `/sparql` | `400`, parse error; nothing reaches the store |
| `LOAD <http://…>` on `/sparql` | `400`, parse error |
| `SELECT * { SERVICE <http://internal-host/sparql> { ?s ?p ?o } }` | `400`, `SERVICE` not allowed |
| `SELECT * { GRAPH <private graph> { ?s ?p ?o } }` at the *public* level, on a store that enforces graph permissions | runs; the result is empty |
| `SELECT * FROM NAMED <private graph> { GRAPH ?g { ?s ?p ?o } }` at the *public* level, on a store that enforces graph permissions | runs; the result holds no triple from a private graph |
| `SELECT * { { SELECT * { SERVICE <http://internal-host/sparql> { ?s ?p ?o } } } }` (`SERVICE` in a subquery) | `400`, `SERVICE` not allowed |
| `SELECT * { GRAPH ?g { ?s ?p ?o } }` at the *public* level, on a store that enforces graph permissions | runs; the result holds no triple from a private graph |
| `SELECT * { ?s ?p ?o }` without `GRAPH` or `FROM`, on a store whose default graph is the union of all graphs | runs; the result holds no triple from a private graph |
| `SELECT * { ?s (<p>|!<p>)* ?o }` (an unbounded property path) | stopped at the tenant's timeout (`504`), within the bounded pool |
| A query that uses a prefix the tenant configures, without declaring it | parses and runs |
| A tenant configuration with an `.rq` file that contains `SERVICE` | refused when loaded |
| A tenant configuration in which one QLever store serves two access levels | refused when loaded |

## Consequences

- One check, written once and tested once, covers both `/sparql` and the tenant's own queries. A faulty `.rq` file is found when the configuration loads, not when a visitor hits it.
- A caller who uses `SERVICE` for federation is refused. Federation is left to consumers ([ADR 0006](0006-resources-and-event-streams.md), question 31).
- The check depends on the parser. A store-specific syntax that strict SPARQL 1.1 does not accept, apart from predefined prefixes, cannot be used on `/sparql`.
- Graphs need no configuration in SPieGeL. A new graph is visible to the levels whose accounts may read it, as soon as it is in the store.
- SPieGeL offers no second check on graphs. The read-only account per level, or the separate instance per level, is the only boundary for graphs. A wrong account is not caught by SPieGeL, so the store's permissions need their own tests in the loading chain.
- Graph permissions become an explicit, declared property of each data source. A wrong declaration is a configuration error that the loading check cannot detect.
- Option 3 of question 29 (one store with named graphs, restricted by rewriting) is ruled out for good. Stores without graph permissions are only possible as one instance or index per level.
- `DESCRIBE` is answered by the store's own algorithm, within the same account, timeout and result limit.
