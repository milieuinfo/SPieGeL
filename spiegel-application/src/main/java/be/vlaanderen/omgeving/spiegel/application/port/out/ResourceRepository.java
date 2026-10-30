package be.vlaanderen.omgeving.spiegel.application.port.out;

import be.vlaanderen.omgeving.spiegel.model.Resource;
import java.util.Optional;

/**
 * Reads resources from a data source.
 */
public interface ResourceRepository {

    /**
     * Finds the resource identified by the given IRI.
     *
     * @param iri the IRI of the resource
     * @return the resource, or empty if the data source does not know it
     */
    Optional<Resource> findByIri(String iri);
}
