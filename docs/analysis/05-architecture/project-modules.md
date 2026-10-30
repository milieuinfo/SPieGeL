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
    - spiegel-adapter-out-virtuoso
    - spiegel-adapter-out-elastic
    - ...

## Packages

### Model

- be.vlaanderen.omgeving.spiegel.model
- be.vlaanderen.omgeving.spiegel.model.exception

### Application

- be.vlaanderen.omgeving.spiegel.application.port
- be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase (interface)
- be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository (interface)
- be.vlaanderen.omgeving.spiegel.application.service.GetResourceService (implements GetResourceUseCase)

### Adapter in: Rest

- be.vlaanderen.omgeving.spiegel.adapter.in.rest.controller.ResourceController (uses GetResourceUseCase)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper.ResourceMapper (maps domain model to REST model)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.model (REST model)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.model.exception (exceptions in REST model)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.view (planned: RDF serialisation with Jena RIOT, HTML templates)
- be.vlaanderen.omgeving.spiegel.adapter.in.rest.security (planned, phase 2: Spring Security, roles → AccessLevel)


### Adapter out: Virtuoso

- be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository.VirtuosoResourceRepository (implements ResourceRepository)
- be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper.VirtuosoMapper (maps Jena model to domain model)

### Bootstrap

- be.vlaanderen.omgeving.spiegel.SpiegelApplication
