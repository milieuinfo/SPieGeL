# Hexagonal architecture

Overview of the core application (the hexagon) with one input port and one output port, and one concrete adapter on each side.

```mermaid
flowchart LR
    client(["Client<br/>(browser, RDF client)"])

    subgraph input["Input adapter: Spring MVC"]
        direction TB
        controller["ResourceController<br/>«@RestController»"]
        restMapper["RestMapper<br/>SpiegelModel → Jena Model"]
        jenaRest[("Jena Model<br/>(REST model)")]
        subgraph views["Views (content negotiation)"]
            direction TB
            turtle["Turtle<br/>text/turtle"]
            jsonld["JSON-LD<br/>application/ld+json"]
            html["HTML<br/>text/html"]
        end
    end

    subgraph core["Core application (hexagon)"]
        direction TB
        inPort{{"«input port»<br/>GetResourceUseCase<br/>SpiegelModel getResource(Resource id)"}}
        service["GetResourceService<br/>implements GetResourceUseCase"]
        outPort{{"«output port»<br/>ResourceRepository<br/>SpiegelModel findById(Resource id)"}}
        subgraph domain["Domain model"]
            direction TB
            resource["Resource<br/>structured identifier"]
            spiegel["SpiegelModel<br/>description of a resource"]
        end
    end

    subgraph output["Output adapter: Apache Jena"]
        direction TB
        repo["VirtuosoResourceRepository<br/>implements ResourceRepository"]
        virtuosoMapper["VirtuosoMapper<br/>Jena Model → SpiegelModel"]
        jenaRaw[("Jena Model<br/>(raw triples)")]
    end

    store[("Virtuoso<br/>triplestore")]

    client -- "HTTP GET + Accept header" --> controller
    controller -- "getResource(Resource id)" --> inPort
    inPort -. "implemented by" .-> service
    service -- "findById(Resource id)" --> outPort
    outPort -. "implemented by" .-> repo
    repo -- "SPARQL query" --> store
    store -- "triples" --> jenaRaw
    jenaRaw --> virtuosoMapper
    virtuosoMapper -- "SpiegelModel" --> repo
    repo -- "SpiegelModel" --> service
    service -- "SpiegelModel" --> controller
    controller --> restMapper
    restMapper --> jenaRest
    jenaRest --> turtle
    jenaRest --> jsonld
    jenaRest --> html
    views -- "HTTP response" --> client

    service -. "uses" .- domain
```

## Flow

1. The **client** sends an HTTP request to the `ResourceController`; the `Accept` header determines the representation it wants.
2. The `ResourceController` builds a `Resource` (structured identifier) and calls the input port `GetResourceUseCase.getResource(Resource id)`.
3. The core application asks the output port `ResourceRepository.findById(Resource id)` for the description of that resource.
4. `VirtuosoResourceRepository`, the concrete implementation of `ResourceRepository`, queries the triplestore with Apache Jena. The raw triples arrive in a Jena `Model`.
5. `VirtuosoMapper` maps that Jena `Model` to a `SpiegelModel`, which is returned to the hexagon.
6. The hexagon returns the `SpiegelModel` to the `ResourceController`.
7. `RestMapper` maps the `SpiegelModel` back to a Jena `Model`, the REST model.
8. Based on content negotiation, the controller selects the view that renders the Jena `Model`: an RDF serialisation (Turtle, JSON-LD) or an HTML representation.

The core depends only on its own model (`SpiegelModel`, `Resource`) and its ports; Apache Jena appears only in the adapters.
