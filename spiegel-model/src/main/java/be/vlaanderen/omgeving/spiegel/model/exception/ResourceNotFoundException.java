package be.vlaanderen.omgeving.spiegel.model.exception;

import be.vlaanderen.omgeving.spiegel.model.ResourceObject;

/**
 * Thrown when no resource is known for a requested IRI.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final ResourceObject resourceObject;

    public ResourceNotFoundException(ResourceObject resourceObject) {
        super("No resource found for resource " + resourceObject);
        this.resourceObject = resourceObject;
    }

    public ResourceObject getResource() {
        return resourceObject;
    }
}
