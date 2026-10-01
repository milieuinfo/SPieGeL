# Data domains

The predecessor publishes seven domains. Each domain is selected by **host name**, not by a path prefix.

| Code | Meaning | Character | Public host |
|---|---|---|---|
| `algemeen` | General (umbrella domain) | Mainly code lists and ontologies; holds the open data catalogue | `data.omgeving.vlaanderen.be` |
| `bodemenondergrond` | Soil and Subsurface Flanders | Mainly code lists and ontologies, plus borehole data. The only domain on a different apex domain | `data.bodemenondergrond.vlaanderen.be` |
| `cbb` | Central Company Register | Created for the annual environmental report | `data.cbb.omgeving.vlaanderen.be` |
| `dba` | Digital Building Application | Forerunner of the current permit portal; **static** dataset, modelled with the W3C Data Cube vocabulary | `data.dba.omgeving.vlaanderen.be` |
| `dsi` | Digital Urban Planning Information | Spatial planning | `data.dsi.omgeving.vlaanderen.be` |
| `imjv` | Integrated Environmental Annual Report | Emissions, discharges, waste processing; includes Data Cube | `data.imjv.omgeving.vlaanderen.be` |
| `zendantennes` | Transmitting antennas | Conformity procedure, modelled in PROV-O, with the antennas themselves as agents | `data.zendantennes.omgeving.vlaanderen.be` |

## Size of the URI space

Number of concepts (`/id/` and `/doc/` endpoints) and models (`/ns/` endpoints) per domain in production at the time of the analysis:

| Domain | Concepts | Models | Representative concepts |
|---|---:|---:|---|
| `algemeen` | 48 | 18 | `catalog`, `dataset`, `dataservice`, SHACL `nodeshape`/`propertyshape`, ten DCAT-AP/OSLO application profiles |
| `bodemenondergrond` | 52 | 1 | `boorgat`, `boring`, `diepteinterval`, `sample` |
| `cbb` | 23 | 2 | `exploitant`, `exploitatie`, `exploitatiestatus` |
| `dba` | 69 | 4 | `dossier`, `besluit`, `kadastraalplanperceel`, `kavel`, `procedurestap` |
| `dsi` | 101 | 2 | `plangebied`, `bestemmingsplangebied`, `rooilijnplangebied`, plus OWL/SHACL constructs as concepts |
| `imjv` | 76 | 2 | `emissie`, `lozing`, `afvalverwerking`, `peilput`/`pompput` |
| `zendantennes` | 45 | 1 | `zendantenne`, `conformiteitsattest`, PROV-O `activity`/`entity` |

The concept lists differ slightly between environments. Concepts appear in test environments before production.

## Newer domains

Several newer domains (`gpbv`, `handhaving`, `mer`, `ovr`) do **not** use the predecessor. Their host names only redirect `/id/` URIs, and send some resource types to small, purpose-built "record page" services. A separate Spring Boot proof of concept, [VKBO-LOD-API](https://github.com/gezever/VKBO-LOD-API), publishes company data on `data.vkbo.omgeving.vlaanderen.be`. Both are precedents for the question of one multi-tenant service versus one service per domain (see [Open questions](../06-open-questions.md)).

## Collections

Every domain publishes, for each RDF class it uses, a *collection* resource (`co:Collection`) that points to that class with `dcterms:relation`:

```turtle
dsi_collection:dossier
    rdf:type          co:Collection ;
    dcterms:relation  dossier:Dossier .
```

The front end's collection table relies on this pattern (see [Client components](client-components.md)). The `dsi` domain alone defines 104 collections this way. SPieGeL should treat this as an established publication pattern.
