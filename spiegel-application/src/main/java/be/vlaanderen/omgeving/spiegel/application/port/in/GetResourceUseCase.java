package be.vlaanderen.omgeving.spiegel.application.port.in;

import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.ResourceObject;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;
import org.apache.commons.rdf.api.Graph;

/**
 * Looks up a single resource by its IRI.
 */
public interface GetResourceUseCase {

    /**
     * Returns the resource identified by the given IRI.
     *
     * @param resourceObject, structured identifier of requested resource
     * @return the resource description (a rdf model)
     * @throws ResourceNotFoundException if no resource is known for the IRI
     */
    Graph getResourceDescription(UserContext userContext, ResourceObject resourceObject, Profile profile);
}
