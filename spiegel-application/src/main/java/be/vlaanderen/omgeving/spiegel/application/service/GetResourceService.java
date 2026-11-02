package be.vlaanderen.omgeving.spiegel.application.service;

import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.SpResource;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;
import org.apache.commons.rdf.api.Graph;

import java.util.Objects;

/**
 * Application service that implements {@link GetResourceUseCase}.
 * <p>
 * Delegates the actual lookup of a resource's RDF description to a {@link ResourceRepository},
 * and throws a {@link ResourceNotFoundException} when the repository does not have a description
 * for the requested resource.
 * <p>
 * Todo: Configure this service with the appropriate configuration class(es) during bootstrap.
 * Todo: For a given resource, determine the specific {@link ResourceRepository} adapter instance to use based on that configuration.
 */
public class GetResourceService implements GetResourceUseCase {

    private final ResourceRepository resourceRepository;

    public GetResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository");
    }

    @Override
    public Graph getResourceDescription(UserContext userContext, SpResource resource, Profile profile) {
        return resourceRepository.findByResource(userContext, resource, profile).orElseThrow(() -> new ResourceNotFoundException(resource));
    }
}
