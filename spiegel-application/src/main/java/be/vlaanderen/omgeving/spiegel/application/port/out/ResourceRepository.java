package be.vlaanderen.omgeving.spiegel.application.port.out;

import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.ResourceObject;
import be.vlaanderen.omgeving.spiegel.model.UserContext;

import java.util.Optional;

/**
 * Reads resources from a data source.
 */
public interface ResourceRepository {

    /**
     * Finds the resource
     *
     * @return a resource description, or empty if the data source does not know it
     */
    Optional<Object> findByResource(UserContext userContext, ResourceObject resourceObject, Profile profile);
}
