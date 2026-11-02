package be.vlaanderen.omgeving.spiegel.application.service;

import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.ResourceObject;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;
import org.apache.commons.rdf.api.Graph;

import java.util.Objects;

/**
 * Implements {@link GetResourceUseCase} on top of a {@link ResourceRepository}.
 */
public class GetResourceService implements GetResourceUseCase {

    private final ResourceRepository resourceRepository;

    public GetResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository");
    }

    @Override
    public Graph getResourceDescription(UserContext userContext, ResourceObject resource, Profile profile) {
        return resourceRepository.findByResource(userContext, resource, profile).orElseThrow(() -> new ResourceNotFoundException(resource));
    }
}
