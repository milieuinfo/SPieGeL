package be.vlaanderen.omgeving.spiegel.adapter.in.rest.controller;

import be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper.RdfGraphMapper;
import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.ResourceObject;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.rdf.api.Graph;
import org.apache.jena.rdf.model.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Serves resources over HTTP.
 */
@RestController
public class ResourceController {

    private final GetResourceUseCase getResourceUseCase;
    private final RdfGraphMapper rdfGraphMapper;

    public ResourceController(GetResourceUseCase getResourceUseCase, RdfGraphMapper rdfGraphMapper) {
        this.getResourceUseCase = getResourceUseCase;
        this.rdfGraphMapper = rdfGraphMapper;
    }

    @GetMapping("/**")
    public void get(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = HttpHeaders.ACCEPT,
                           required = false,
                           defaultValue = "")
            List<MediaType> acceptHeaders) {

        // 1. get matching redirect endpoint path definitions
        // if match perform redirect
        // else
        // 2. get all matching document endpoint path definitions
        // 2.1 select the best match
        // 3. Create Resource object based on incoming request and document endpoint pathdefinition
        // 4. Create UserContext based on SecurityContext
        // 5, Create Profile based on query parameter or header of incoming request
        // 6. Call getResourceUseCase
        // 7. Map retrieved resource rdf description to jena model
        // 8. Generate appropriate view based on content negotiation
    }

    private Model getResource(UserContext userContext, ResourceObject resourceObject, Profile profile) {
        Graph resourceDescription = getResourceUseCase.getResourceDescription(userContext, resourceObject, profile);
        return rdfGraphMapper.toJenaModel(resourceDescription);
    }
}
