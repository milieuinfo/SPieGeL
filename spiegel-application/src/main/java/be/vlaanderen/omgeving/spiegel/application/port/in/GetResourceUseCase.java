package be.vlaanderen.omgeving.spiegel.application.port.in;

import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.SpResource;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;
import org.apache.commons.rdf.api.Graph;

public interface GetResourceUseCase {

    /**
     * Returns a description of the resource.
     *
     * @param spResource, structured identifier of requested resource
     * @return the resource description (a rdf model)
     * @throws ResourceNotFoundException if no resource is known for the SpResource
     */
    Graph getResourceDescription(UserContext userContext, SpResource spResource, Profile profile);
}
