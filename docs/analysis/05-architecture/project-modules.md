# Project modules

The Maven modules and the packages in each. See [Module structure](module-structure.md) for the dependency rules.

## Maven modules

- spiegel-model
- spiegel-application
- spiegel-bootstrap
- spiegel-adapter
  - spiegel-adapter-in
    - spiegel-adapter-in-rest
  - spiegel-adapter-out
    - spiegel-adapter-out-sparqlendpoint
    - spiegel-adapter-out-virtuoso
    - spiegel-adapter-out-elastic
    - ...

## Packages

### Model

- be.vlaanderen.omgeving.spiegel.model
- be.vlaanderen.omgeving.spiegel.model.exception

### Application

- be.vlaanderen.omgeving.spiegel.application.port
- be.vlaanderen.omgeving.spiegel.application.port.in (inbound ports (interfaces): the use cases that adapters call)
- be.vlaanderen.omgeving.spiegel.application.port.out (outbound ports: the interfaces the core needs from the outside world, such as data sources)
- be.vlaanderen.omgeving.spiegel.application.service (application services: i.e implementing services that are not exposed to the outside world of the hexagon)

### Adapter in: Rest

- be.vlaanderen.omgeving.spiegel.adapter.in.rest (inbound HTTP adapter code (spring mvc). i.e redirecting, dereferencing, content negotiation, view/response generation,...)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.controller (web controllers, i.e controller serving configured rest endpoints using inbound port(s))
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper (mappers, mapping domain model to REST model)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.model (REST model)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.model.exception (exceptions in REST model)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.view (RDF serialisation with Jena RIOT, HTML templates)

### Adapter out: Sparqlendpoint (ResourceRepository over SPARQL 1.1)

- be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint (generic sparql endpoint adapter)
- be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint.repository (sparql endpoint resource repository)
- be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint.mapper (mappers, mapping used adapter model (i.e Jena) to domain model)

### Adapter out: Virtuoso

- be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso (virtuoso specific adapter)
- be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository (virtuoso resource repository)

### Bootstrap

- be.vlaanderen.omgeving.spiegel (bootstrap class en bootstrap configuration classes)
