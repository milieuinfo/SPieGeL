package be.vlaanderen.omgeving.spiegel.adapter.in.rest.controller;

import be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper.RdfGraphMapper;
import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.SpResource;
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
 * Driving (inbound) adapter that exposes the core's {@link GetResourceUseCase} over HTTP using Spring MVC.
 * <p>
 * This controller is the HTTP entry point of the hexagon described in {@code docs/hexagonal-architecture.md}:
 * it translates an incoming HTTP request into the domain objects the core understands
 * ({@link UserContext}, {@link SpResource}, {@link Profile}), invokes the {@link GetResourceUseCase}
 * input port, and maps the resulting RDF {@link Graph} to a Jena {@link Model} (via {@link RdfGraphMapper})
 * so it can be rendered as the representation requested through content negotiation (the {@code Accept} header).
 * <p>
 * Besides plain dereferencing, this adapter is also expected to apply any configured {@code 303} redirects
 * before invoking the use case; see the package Javadoc for the full set of responsibilities of this adapter
 * (dereferencing, redirects, content negotiation and the {@code /sparql} endpoint).
 * <p>
 * <strong>Work in progress:</strong> request parsing (matching endpoint configuration, building the
 * {@link SpResource} and {@link Profile}, selecting redirects) and response rendering (the content
 * negotiation views) are not yet implemented; see the inline {@code todo} list on {@link #get}.
 *
 * @see GetResourceUseCase
 * @see RdfGraphMapper
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

        // todo
        // 0. Before this controller method is entered some http servlet filters will be executed (i.e authentication, rate limit, etc) which will be result
        // in objects available in the request scope or a direct response (i.e rate limit).
        // Which filters is still to decide but will probably result in a UserContext object being available in the request scopee and a hostname (tenant)
        // 1. Create or get UserContext.
        // 2. get matching redirect endpoints (defined in the configuration) based on incoming request and the configuration
        //   - this are endpoints configurations that the application must do a redirect.
        // 2.1 if match perform the redirect on the best matched redirect configuration (most selective)
        // 2.2 else (no redirect endpoint) get all matching document endpoint path definitions
        // 3. select the best match (most selective).
        // 5. Determine and create SpResource object based on incoming request and selected document endpoint path definition
        // 5, Determine and create Profile object based on incoming request, selected document endpoint path definition, a specific query parameter or header of incoming request
        // 6. Call getResourceUseCase
        // 7. Map retrieved description of the resource to jena model (the structured data)
        // 8. Render the appropriate view (representation) based on content negotiation and profile (in this step the profile only deteremines the reprenstation,
    }

    private Model getResource(UserContext userContext, SpResource spResource, Profile profile) {
        Graph resourceDescription = getResourceUseCase.getResourceDescription(userContext, spResource, profile);
        return rdfGraphMapper.toJenaModel(resourceDescription);
    }
}
