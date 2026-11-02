package be.vlaanderen.omgeving.spiegel.model.exception;

import be.vlaanderen.omgeving.spiegel.model.SpResource;

/**
 * Thrown when no resource is known for a requested IRI.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final SpResource spResource;

    public ResourceNotFoundException(SpResource spResource) {
        super("No resource found for resource " + spResource);
        this.spResource = spResource;
    }

    public SpResource getResource() {
        return spResource;
    }
}
