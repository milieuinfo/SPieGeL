## Maven modules

- model
- application
- bootstrap
- adapter
  - in
    - rest
  - out
    - virtuoso
    - postgres (?)
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


### Adapter out: Virtuoso

- be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository.VirtuosoResourceRepository (implements ResourceRepository)
- be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper.VirtuosoMapper (maps Jena model to domain model)

### Bootstrap

- be.vlaanderen.omgeving.spiegel.SpiegelApplication
