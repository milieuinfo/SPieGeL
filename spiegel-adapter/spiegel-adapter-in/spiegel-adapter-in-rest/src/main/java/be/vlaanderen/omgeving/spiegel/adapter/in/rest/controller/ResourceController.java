package be.vlaanderen.omgeving.spiegel.adapter.in.rest.controller;

import be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper.ResourceMapper;
import be.vlaanderen.omgeving.spiegel.adapter.in.rest.model.ResourceResponse;
import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves resources over HTTP.
 */
@RestController
public class ResourceController {

    private final GetResourceUseCase getResourceUseCase;
    private final ResourceMapper resourceMapper;

    public ResourceController(GetResourceUseCase getResourceUseCase, ResourceMapper resourceMapper) {
        this.getResourceUseCase = getResourceUseCase;
        this.resourceMapper = resourceMapper;
    }

    @GetMapping("/resource")
    public ResourceResponse getResource(@RequestParam String iri) {
        return resourceMapper.toResponse(getResourceUseCase.getResource(iri));
    }
}
