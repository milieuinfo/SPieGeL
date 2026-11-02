package be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository;

import be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper.JenaModelMapper;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.ResourceObject;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import org.apache.commons.rdf.api.Graph;

import java.util.Objects;
import java.util.Optional;

/**
 * Reads resources from Virtuoso over the SPARQL 1.1 Protocol.
 *
 * <p>Not connected to Virtuoso yet: every lookup comes back empty.
 */
public class VirtuosoResourceRepository implements ResourceRepository {

    private final JenaModelMapper mapper;

    public VirtuosoResourceRepository(JenaModelMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Optional<Graph> findByResource(UserContext userContext, ResourceObject resourceObject, Profile profile) {
        return Optional.empty();
    }

}
