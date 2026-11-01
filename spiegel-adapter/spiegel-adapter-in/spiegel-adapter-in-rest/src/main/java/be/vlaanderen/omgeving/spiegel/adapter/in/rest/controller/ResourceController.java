package be.vlaanderen.omgeving.spiegel.adapter.in.rest.controller;

import be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper.ResourceMapper;
import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.Resource;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final ResourceMapper resourceMapper;

    public ResourceController(GetResourceUseCase getResourceUseCase, ResourceMapper resourceMapper) {
        this.getResourceUseCase = getResourceUseCase;
        this.resourceMapper = resourceMapper;
    }

    @GetMapping("/**")
    public void get(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = HttpHeaders.ACCEPT,
                           required = false,
                           defaultValue = "")
            List<MediaType> acceptHeaders) {

        // get matching redirect endpoint path definitions
        // if match perform redirect

        // else get matching document endpoint pathdefinitions
        // if match select best match
        // create Resource object based on request and document endpoint pathdefinition
        // create UserContext based on SecurityContext
        // create Profile based on query parameter or header
        // call getResourceUseCase
        // map retrieved resource rdf description to jena model
        // generate appropriate view based on content negotiation

        // perform redirect or document
    }

    private Object getResource(UserContext userContext, Resource resource, Profile profile) {
        return getResourceUseCase.getResourceDescription(userContext, resource, profile);
    }
}
