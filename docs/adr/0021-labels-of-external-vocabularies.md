# 0021. Take labels of external vocabularies from reviewed extracts in the configuration

- **Status:** Accepted
- **Date:** 2026-10-26

## Context

Every IRI on a page is shown by its label (FR-HTML-10). For terms of external vocabularies, such as `dct:title`, `skos:broader` or `dcat:Dataset`, FR-HTML-11 consults label sources in order: the domain's own data source, then a shared vocabulary source, then the fallback of a prefixed name or the full IRI. Which sources are the default, and what the shared source is, was [open question 26](../analysis/06-open-questions.md#architecture-and-operations). Fetching a vocabulary from the web while a request is handled is ruled out (NFR-SEC-06).

This decision covers **terms of vocabularies**: a few thousand properties, classes and individuals, the same for every tenant, which rarely change. It does not cover **resources of other domains** that the data links to, such as a concept from a code list of another tenant. Those change with that tenant's data and have their own access levels; they are [open question 42](../analysis/06-open-questions.md#architecture-and-operations).

Loading every external ontology into every store means maintaining the same ontologies many times, in the loading chain. Prez bundles labels of common vocabularies with the server instead ([Prez code review](../analysis/04-landscape/prez-code-review.md#what-spiegel-can-learn-from-it)).

The *RDF and SPARQL* plugin for IntelliJ IDEA solves the same problem for an editor. It downloads each vocabulary ahead of time, from a download URL that is the namespace IRI by default but can be overridden, and keeps a snapshot in RDF Thrift with the date of the download. It refreshes only on request. For its hover texts and completion it does not use the graph, but an index that holds per term its kind (class or property), its label (`rdfs:label`, `skos:prefLabel`, `dct:title`) and its description (`rdfs:comment`, `skos:definition`, `dct:description`), each with its language. Two observations from its settings:

- a namespace IRI is not always a usable source: XSD and schema.org are downloaded from a repository instead;
- whole vocabularies can be large: QUDT's units are 81,000 triples, mostly labels in many languages.

## Decision

1. **Labels of external terms come from, in order:** the domain's own data source, through the label query of the HTML profile ([request flow](../analysis/05-architecture/request-flow.md#rendering-html)); the tenant's own vocabulary extracts; the shared vocabulary extracts; then the fallback of FR-HTML-10.
2. **The shared source is part of the configuration, not a store.** The shared defaults of the configuration repository ([ADR 0013](0013-tenant-configuration-delivery.md)) hold a list of vocabularies and an **extract** per vocabulary. A tenant can add vocabularies of its own in the same way.
3. **Each vocabulary in the list has a namespace and a download URL**, which is the namespace IRI unless it is overridden, as for XSD and schema.org.
4. **An extract holds only what pages need.** That is, per term in the namespace:
    - its kind: class, property or other;
    - its labels (`rdfs:label`, `skos:prefLabel`, `dct:title`);
    - its descriptions (`rdfs:comment`, `skos:definition`, `dct:description`).

    Only the interface languages of SPieGeL ([open question 27](../analysis/06-open-questions.md#front-end)) and literals without a language tag are kept. The extract is sorted Turtle, so that its diffs can be read, and records its source URL and the date it was retrieved.
5. **Extracts are refreshed ahead of time, never while a request is handled.** A tool shipped with SPieGeL downloads the vocabularies and writes the extracts. A scheduled job in the CI of the configuration repository runs it and opens a merge request, which the Linked Data team reviews like any other change of configuration.
6. **Additional label files may sit next to an extract**, for example Dutch labels for external terms, such as those published by OSLO. They take precedence over the extract of the same vocabulary.
7. **At start-up SPieGeL builds an index** from the extracts: per term, its kind, and its labels and descriptions per language. Looking up a label needs no query. Language follows the tenant's priority list (FR-HTML-10).
8. **Shared labels serve presentation only.** They are used in HTML, for link texts and, with the descriptions, as explanations of properties. They are not added to RDF responses, which hold only what the data sources return.

## Consequences

- One copy of each external vocabulary, maintained in one place, serves every tenant. Stores no longer need to hold external ontologies only for their labels.
- A build of the configuration is reproducible: it depends on no external site, and every change to a vocabulary's labels is reviewed. Because extracts are configuration, a refresh changes the configuration version, and responses follow ([ADR 0020](0020-cache-key-and-entity-tag.md)).
- A refresh can fail when a vocabulary's site is down or no longer negotiates RDF. The previous extract then stays, and the merge request says which vocabulary could not be retrieved.
- Extracts are small, because they leave out axioms and other languages. Their size, and that of the index in memory, must be checked for large vocabularies such as QUDT.
- A domain still wins over the shared source, so it can give its own labels for external terms.
- SPieGeL ships one more tool, next to the context generator of [ADR 0017](0017-framed-json-ld-with-a-frozen-context.md). Both read vocabularies and shapes, and could share code.
- Labels of resources of other domains stay unsolved until open question 42 is answered: until then their links show the IRI, unless the domain's own store holds a label.
