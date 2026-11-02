package be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository;

import be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint.mapper.JenaModelMapper;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.SpResource;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import org.apache.commons.rdf.api.Graph;

import java.util.Objects;
import java.util.Optional;

/**
 * Reads resources from Virtuoso
 * This class might extend the SparqlEndpointRepository or use the jdbs support of Virtuoso.
 */
public class VirtuosoResourceRepository implements ResourceRepository {

    private final JenaModelMapper mapper;

    public VirtuosoResourceRepository(JenaModelMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Optional<Graph> findByResource(UserContext userContext, SpResource spResource, Profile profile) {
        return Optional.empty();
    }

}
