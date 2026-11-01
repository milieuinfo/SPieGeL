package be.vlaanderen.omgeving.spiegel.model.exception;

import be.vlaanderen.omgeving.spiegel.model.Resource;

/**
 * Thrown when no resource is known for a requested IRI.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final Resource resource;

    public ResourceNotFoundException(Resource resource) {
        super("No resource found for resource " + resource);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }
}
