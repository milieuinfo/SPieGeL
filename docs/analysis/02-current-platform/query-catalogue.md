# Query catalogue

This chapter lists the predecessor's SPARQL query paths. It covers queries run on the server while a response is built, and queries run in the browser by front-end components. Where possible the literal query text is given, because it is the most precise specification of what the data must support.

## Overview

| Path / trigger | Runs on | Status | Template(s) |
|---|---|---|---|
| `/doc/{concept}/{id}` | server | active | 6-entry query catalogue (see [Request flows](request-flows.md)) |
| `/ns/{model}` | server | active | one `CONSTRUCT` |
| `/sparql` | server | active | user's own query |
| `/keywordsearch` | server | active | count + result query on `bif:contains` |
| Incoming relations panel | browser | active | `objects-inbound(-count).rq` |
| Expandable property groups | browser | active | `objects-outbound(-count).rq` |
| SKOS tree ("Browse code list") | browser | active | `taxonomy-up.rq`, `taxonomy-down.rq` |
| DCAT tree ("Browse dataset") | browser | active (repaired in `omgeving-ld` 1.7.27) | `dataset-up.rq`, `dataset-down.rq` |
| Collection table | browser | active | `list-by-pattern(-count).rq` |
| SPARQL editor and search form | browser builds, server runs | active | none; navigates to `/sparql?query=…` or `/keywordsearch?search=…` |
| Reconciliation | — | not routed | Jena `text:query` |
| Linked Data Fragments | — | not routed | count + paged `CONSTRUCT` |
| Jena / Blazegraph keyword search | — | not routed | `text:query`, `bds:search` |
| Per-graph triple count for home page | — | disabled | — |
| `/dump` | — | placeholder, returns no data | — |

## Server side

### `/ns/{model}`

```sparql
CONSTRUCT { ?subject ?predicate ?object . ?object ?prop ?value . … }
WHERE {
  GRAPH ?g {
    { ?subject rdfs:isDefinedBy <${base}/id/ontology/${model}> .
      ?subject ?predicate ?object . FILTER (!isBlank(?object)) }
    UNION
    { ?subject rdfs:isDefinedBy <${base}/id/ontology/${model}> .
      ?subject ?predicate ?object . FILTER (isBlank(?object))
      ?object ?prop ?value . FILTER (?prop != owl:unionOf && ?prop != owl:intersectionOf)
      OPTIONAL { … walk rdf:first / rdf:rest of owl:unionOf / owl:intersectionOf … } }
  }
  FILTER (STRSTARTS(STR(?subject), '${base}/ns/${model}#'))
}
```

### `/keywordsearch`

```sparql
SELECT DISTINCT ?id (?sc AS ?score) ?label
{ ?id ?p ?label . ?label bif:contains "'${search}'" OPTION ( SCORE ?sc ) }
ORDER BY DESC(?sc) LIMIT ${limit} OFFSET ${offset}
```

!!! note
    Search input must never be pasted into a query string. SPieGeL builds queries with bound parameters or proper escaping. → [NFR-SEC-04](../03-requirements/non-functional.md#security)

### `/sparql`

There is no template. The user's query is forwarded as is. When the request has no query, a per-domain default is used, falling back to:

```sparql
SELECT ?s ?p ?o WHERE { ?s ?p ?o . } LIMIT 5
```

## Browser side

The front-end components come from [`omgeving-ld`](https://github.com/milieuinfo/linked-data) (Vue 2). They fill `%placeholder%` tokens in `.rq` templates and `POST` the result to the domain's `/sparql` endpoint:

```javascript
this.$http.post(this.endpoint, { query: query }, {
  credentials: true, emulateJSON: true,
  headers: { Accept: 'application/sparql-results+json, application/json, */*' }
})
```

**This is a compatibility contract.** As long as these components are in use, SPieGeL must offer a `/sparql` endpoint on the same host that accepts a `POST` with a `query` form field and returns `application/sparql-results+json`. → [FR-SPARQL-02](../03-requirements/functional.md#sparql-endpoint)

### Incoming and outgoing relations

```sparql
# objects-inbound.rq (objects-outbound.rq uses <%subject%> <%predicate%> ?uri)
SELECT DISTINCT ?uri ?titel
%from%
WHERE {
    ?uri <%predicate%> <%subject%> .
    OPTIONAL { ?uri rdfs:label ?rdfsLabel }
    OPTIONAL { ?uri skos:prefLabel ?skosLabel }
    OPTIONAL { ?uri dct:title ?dctTitle }
    BIND(COALESCE(?rdfsLabel, ?skosLabel, ?dctTitle) AS ?titel)
    %filters%
}
ORDER BY %sortDirection%(?%sortField%) LIMIT %pageSize% OFFSET %offset%
```

The component reads `%subject%` from the nearest HTML element with class `ld-subject` and an `about` attribute. **SPieGeL's HTML pages must embed the subject IRI that way** for the component to work.

### SKOS tree

```sparql
# taxonomy-up.rq: path from the subject to the top of its concept scheme
SELECT DISTINCT ?uri ?type ?label
%from%
WHERE {
    { <%subject%> skos:broader* ?uri . ?uri a ?type }
    UNION { <%subject%> skos:broader*/skos:topConceptOf ?uri . ?uri a ?type .
            FILTER(?type = skos:ConceptScheme) }
    OPTIONAL { ?uri rdfs:label ?rdfsLabel } OPTIONAL { ?uri skos:prefLabel ?skosLabel }
    OPTIONAL { ?uri dct:title ?dctTitle }
    BIND(COALESCE(?rdfsLabel, ?skosLabel, ?dctTitle) AS ?label)
}

# taxonomy-down.rq: children of one node, loaded on expand
SELECT DISTINCT ?uri ?label
%from%
WHERE {
    { <%parentResource%> skos:narrower|skos:hasTopConcept ?uri . }
    OPTIONAL { ?uri rdfs:label ?rdfsLabel } OPTIONAL { ?uri skos:prefLabel ?skosLabel }
    OPTIONAL { ?uri dct:title ?dctTitle }
    BIND(COALESCE(?rdfsLabel, ?skosLabel, ?dctTitle) AS ?label)
}
```

### DCAT tree

```sparql
# dataset-up.rq
SELECT DISTINCT ?uri ?type ?label
%from%
WHERE {
    { <%subject%> ^dct:hasPart|^dct:hasVersion ?uri . ?uri a ?type . FILTER(?type = dcat:Dataset) }
    UNION { <%subject%> (^dct:hasPart|^dct:hasVersion)*/^dcat:dataset ?uri . ?uri a ?type .
            FILTER(?type = dcat:Catalog) }
    OPTIONAL { ?uri rdfs:label ?rdfsLabel }
    BIND(IF(BOUND(?rdfsLabel), ?rdfsLabel, ?uri) AS ?label)
}

# dataset-down.rq (as repaired in omgeving-ld 1.7.27)
SELECT DISTINCT ?uri ?label
%from%
WHERE {
    { <%parentResource%> dcat:dataset|dct:hasVersion|dct:hasPart ?uri .
      ?uri a ?type .
      FILTER(?type = dcat:Dataset) }
    OPTIONAL { ?uri rdfs:label ?rdfsLabel }
    BIND(IF(BOUND(?rdfsLabel), ?rdfsLabel, ?uri) AS ?label)
}
```

### Collection table

```sparql
# list-by-pattern.rq
SELECT DISTINCT (?uri AS ?URI) (?label AS ?Label) (?type AS ?Type) (?typeLabel AS ?_TypeLabel)
%from%
WHERE {
    <%resource%> dct:relation ?type .
    ?uri a ?type .
    OPTIONAL { ?uri rdfs:label ?uriRdfsLabel } OPTIONAL { ?uri skos:prefLabel ?uriSkosLabel }
    OPTIONAL { ?uri dct:title ?uriDctTitle }
    BIND(COALESCE(?uriRdfsLabel, ?uriSkosLabel, ?uriDctTitle) AS ?label)
    OPTIONAL { ?type rdfs:label ?typeLabel }
    %filters%
}
ORDER BY %sortDirection%(?%sortField%) LIMIT %pageSize% OFFSET %offset%
```

This relies on the collection pattern described in [Data domains](data-domains.md#collections).

## Not routed

### Reconciliation (OpenRefine-compatible)

```sparql
SELECT DISTINCT ?id ?type ?score ?label
{ ?id rdf:type ?type . (?id ?score) text:query ('${search}' ${limit}) .
  ?id (rdfs:label|skos:prefLabel|skos:altLabel) ?label . ${typefilterclause} }
ORDER BY DESC(?score) LIMIT ${limit}
```

This needs a Jena full-text index, which the production store does not use. That probably explains why it is not routed.

### Linked Data Fragments

```sparql
# count
SELECT (COUNT(*) AS ?triples) WHERE { ${subject} ${predicate} ${object} . }

# fragment (abridged)
CONSTRUCT { … hydra:Collection metadata … ?subject ?predicate ?object . }
WHERE { OPTIONAL { SELECT * WHERE { ${subject} ${predicate} ${object} . }
        ORDER BY ?subject ?predicate ?object OFFSET ${offset} LIMIT ${limit} } … }
```

## Lessons for SPieGeL

- The server-side paths are what SPieGeL's domain model must cover explicitly.
- The browser-side templates are a **specification of what the front end expects from the data**: the `dct:relation` pattern for collections, and `dct:hasPart` / `dct:hasVersion` / `dcat:dataset` for catalogues.
- Front-end behaviour must not be taken as a specification without checking that it works. The DCAT tree was broken by two faults in the front-end package until version 1.7.27.
