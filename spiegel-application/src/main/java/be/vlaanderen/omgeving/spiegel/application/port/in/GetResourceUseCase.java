package be.vlaanderen.omgeving.spiegel.application.port.in;

import be.vlaanderen.omgeving.spiegel.model.Resource;
import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;

/**
 * Looks up a single resource by its IRI.
 */
public interface GetResourceUseCase {

    /**
     * Returns the resource identified by the given IRI.
     *
     * @param iri the IRI of the resource
     * @return the resource
     * @throws ResourceNotFoundException if no resource is known for the IRI
     */
    Resource getResource(String iri);
}
