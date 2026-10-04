# Data-dependent HTML rendering

The predecessor does not use one fixed layout for its HTML subject pages. Which blocks and [client components](client-components.md) appear on a page, and in what order, depends on the data in the response: the resource's type, its predicates and, in a few cases, the predicates' own definitions. This chapter describes those rules, so that SPieGeL can reproduce what is wanted and drop what is accidental.

The rules live in XSLT 2.0 stylesheets that turn the RDF/XML response into HTML. Version reviewed: the predecessor's source as of 1 October 2026, with `omgeving-ld` 1.7.27.

## Structure of the stylesheets

There are two entry points, one per page family:

| Page family | Entry stylesheet | Imports |
|---|---|---|
| `doc` subject pages | `rdfxml2htmldocgeneric.xsl` | `milieu-general.xsl` → `core-general.xsl` → `core-common.xsl` |
| `ns` vocabulary pages | `rdfxml2htmlmodelgeneric.xsl` | `milieu-def.xsl` → `core-def.xsl` → `core-common.xsl` |

The stylesheets are layered like a template method. A generic layer (`lod-core/`) defines the page and declares empty functions, such as `content-block-taxonomy`, `content-block-geo-map` and `presentation-category`, "intended to be overridden by the importer". An organisation layer (`milieu-common/`) overrides them with the actual rules. The domain (`imjv`, `cbb`, `dsi`, …) is a stylesheet parameter. It selects the page title, the SPARQL endpoint the components talk to, and extra scripts, but not the rendering rules: those are the same for every domain.

## Subject pages (`doc`)

### Choosing the main resource and its title

A response can describe several resources. The main one (`is-starting-point`) is the only described resource that has both an IRI and an `rdf:type`. If there are several, it is the only one of those with an `rdfs:isDefinedBy`. All other descriptions in the response are supporting data: labels of linked resources, definitions of properties, and incoming triples.

The page title is the first non-empty value of `rdfs:label`, `skos:prefLabel` or `dct:title`, followed by the IRI. Wherever a label is chosen, the language preference is: no language tag, then `nl`, then `en…`, then any.

### Rules by resource type

| Condition on the main resource | What is shown | Component and attributes |
|---|---|---|
| `rdf:type dcat:Catalog` | "Browse dataset" card | `<ld-dataset subject="…" predicate="dataset">` |
| `rdf:type dcat:Dataset` | "Browse dataset" card | `<ld-dataset subject="…" predicate="catalog">` |
| `rdf:type skos:ConceptScheme` | "Browse codelijst" card | `<ld-taxonomy subject="…" predicate="hasTopConcept">` |
| `rdf:type skos:Concept` | "Browse codelijst" card | `<ld-taxonomy subject="…" predicate="broader\|topConceptOf">` |
| `rdf:type co:Collection` | "Collectie leden": a searchable, paged table | `<ld-data-table query="/queries/list-by-pattern.rq" …>`. The `resource` attribute is the subject IRI with `/collection/` replaced by `/` |
| One of: `locn:geometry`; `geo:lat` and `geo:long`; `milieu:lambert72_x` and `_y`; `imjv:lambert72_x` and `_y` | A map with one marker, in that order of preference | `<ld-map lon="…" lat="…">` or `<ld-map x="…" y="…">` |
| The response contains triples that point *to* the main resource | "Inkomende relaties", collapsed by default. It holds one `<ld-predicate inbound>` per distinct predicate, which pages through the subjects in the browser | `<ld-predicate inbound about="{predicate}">` |

The four SKOS and DCAT types also change the **layout**. The page gets two columns: the tree on the left (one third), and the map and property blocks on the right (two thirds). Every other resource gets one column.

### Rules by predicate: presentation categories

Every property of the main resource is assigned a *presentation category* (`presentation-category`) by its predicate, and sometimes by its value:

| Category | Predicates | Block title |
|---|---|---|
| hidden | `skos:hiddenLabel`, `milieu:key`, `blazegeo:lat_long` | — |
| `LABEL` | `rdfs:label`, `skos:prefLabel`, `rdf:type`, `dct:title`, `dct:identifier` | Labels |
| `MEASUREMENT` | any predicate that is itself typed `qb:MeasureProperty` **in the response**, and `sdmx-attribute:unitMeasure` | — |
| `DIMENSION` | any predicate typed `qb:DimensionProperty` in the response | — |
| `LOC` | `geo:lat`, `geo:long`, `milieu:lambert72_x`/`_y`, `imjv:lambert72_x`/`_y`, `imjv:lambertWktString`, `locn:geometry` | Geo informatie |
| `REPORT` | IMJV predicates whose local name starts with `aangifte`, `aanvulling` or `bijlage` | Rapporten en bijlagen |
| `VALUE` | any other predicate with a literal value | Eigenschappen |
| `OBJECT` | any other predicate with an IRI value | Uitgaande relaties (collapsed) |
| `DOC` | `rdfs:isDefinedBy`, `foaf:page` | Linken naar de beschrijvende documenten (collapsed) |
| `DATASET` | `qb:dataSet`, `dct:isPartOf` | Link naar kubus |

The blocks appear in a fixed order: Labels, Measurement, Dimension, Geo, Reports, Properties, Collection members, Outgoing relations, Incoming relations, Documents, Cube. A last block, "Document eigenschappen", shows the properties of the *document* resource: the subject IRI without its `#id` fragment.

Within a block, properties are grouped by predicate in an `<ld-predicate>`. Labels come first, then the rest alphabetically by label. Values are deduplicated and sorted. `true` and `false` get their own CSS class. An IRI value is a link labelled with the linked resource's label if the response contains one; otherwise the IRI, shortened for `id.milieuinfo.be`. Blank nodes are rendered recursively as nested `<ld-object bnode>` elements.

## Vocabulary pages (`ns`)

`core-def.xsl` has no type-dependent components. It lists every resource whose type is one of `owl:Class`, `rdfs:Class`, `owl:ObjectProperty`, `owl:DatatypeProperty`, `sh:PropertyShape`, `sh:NodeShape` and `rdf:Property`, grouped by type in that order and sorted by IRI. Each term becomes an `<ld-card>` with an anchor equal to its fragment, so `…/ns/model#Term` links straight to it. Literal properties come first, then IRIs, and blank nodes (such as `owl:unionOf` lists) are nested. The page title is the last path segment of the first term, in upper case.

## Observations

1. **The rules are code, not configuration.** Supporting a new type, a new geometry property or a new domain vocabulary means changing XSLT. Organisation-specific vocabularies (`milieu:`, `imjv:`) are hard-coded in rules that apply to every domain.
2. **Some rules depend on what the query returns.** A predicate counts as a measure or dimension only if its definition happens to be in the response. The incoming-relations panel appears only if the server-side query returned at least one incoming triple, even though the panel then fetches the rest itself. The HTML is therefore coupled to the query catalogue ([Request flows](request-flows.md)).
3. **Outgoing groups never query the browser.** Only the incoming `<ld-predicate>` gets an `endpoint` attribute. The function that would give outgoing groups one (`block-presentation-endpoint`) is called only from code that is commented out. So outgoing groups show only the server-rendered values, `objects-outbound.rq` is not used, and `ld-predicate`'s built-in switch to `ld-taxonomy` for SKOS predicates (which requires an endpoint) never fires. The SKOS tree appears only through the type rule above.
4. **Geometries are reduced to one point.** For `locn:geometry`, a string operation picks one coordinate pair out of the WKT literal; a comment calls it "a hack to select one long/lat point from polygons too". Lines and polygons are not drawn, and a CRS IRI in the literal is ignored. The map (OpenLayers, Lambert 72, the public GRB base map) shows one marker.
5. **The collection table relies on an IRI convention.** The members are found through the collection's IRI with `/collection/` removed (see [Collections](data-domains.md#collections)).
6. **Per-domain values are hard-coded.** The SPARQL endpoint for each domain is a `choose` over domain names (marked "THIS IS A HACK - GET PUBLIC ENDPOINT FROM CONFIG LATER"), as are the page titles, the explorer scripts for `imjv`, `cbb` and `dba`, and the rewriting of links per environment.
7. **Dead code.** A template that renders an `rdf:value` with its unit symbol (`qudt:symbol`) exists, but its mode is never applied, so a value with a unit is never shown as "value unit".

## What this means for SPieGeL

- The behaviour worth keeping is **choosing presentation blocks by rules on the data**: by `rdf:type`, by predicate, by the predicate's own type (measures and dimensions), and by value type (geometries). This is finer-grained than FR-HTML-03, which chooses a template per page. → FR-HTML-06.
- These rules should be **configuration per tenant with shared defaults**, not code (FR-RA-02, FR-MT-02). Organisation vocabularies such as `milieu:` belong in the tenant's configuration.
- A rule should depend only on data that a **query declared for that purpose** retrieves, not on whatever the main query happens to return. For example, the definitions of the properties in use, or a count of incoming triples. This connects naturally to [content negotiation by profile](../03-requirements/functional.md#content-negotiation): an HTML profile can declare the extra queries it needs (FR-CN-03, FR-RA-03).
- Geometries deserve a proper map: any WKT geometry type, honouring the CRS, plus WGS84 and Lambert 72 coordinate pairs. → FR-HTML-07.
- The blocks and components themselves must come from the Flux design system, with new components where Flux has none. See [Design system: Flux](../03-requirements/design-system.md).
- The landscape reviews show three ways to express such rules: per-class profiles and SHACL shapes in [Prez](../04-landscape/prez-code-review.md), viewers per endpoint in [ELDA](../04-landscape/elda-code-review.md), and a generic web component with rendering options in [Trifid](../04-landscape/trifid-code-review.md). See [open question 21](../06-open-questions.md#front-end).
