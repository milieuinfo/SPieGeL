package be.vlaanderen.omgeving.spiegel.application.port.out;

import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.SpResource;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import org.apache.commons.rdf.api.Graph;

import java.util.Optional;

/**
 * Reads resources from a data source.
 * <p>
 * Implementations are responsible for retrieving a resource's RDF description
 * from a particular backing store (e.g. a SPARQL endpoint, Virtuoso, Elasticsearch, etc.),
 * taking into account the requesting user's context and the requested profile.
 */
public interface ResourceRepository {

    /**
     * Finds the resource description for the given resource.
     *
     * @param userContext    the context of the user performing the request, may influence access control or filtering
     * @param spResource     the structured identifier of the resource to look up
     * @param profile        the profile that determines which representation/subset of the resource is returned
     * @return a resource description, or empty if the data source does not know it
     */
    Optional<Graph> findByResource(UserContext userContext, SpResource spResource, Profile profile);
}
