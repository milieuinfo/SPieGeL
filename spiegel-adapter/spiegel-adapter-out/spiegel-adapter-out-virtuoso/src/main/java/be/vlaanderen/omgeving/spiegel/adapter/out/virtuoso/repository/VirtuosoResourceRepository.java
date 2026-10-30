package be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository;

import be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper.VirtuosoMapper;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.model.Resource;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads resources from Virtuoso over the SPARQL 1.1 Protocol.
 *
 * <p>Not connected to Virtuoso yet: every lookup comes back empty.
 */
public class VirtuosoResourceRepository implements ResourceRepository {

    private final VirtuosoMapper mapper;

    public VirtuosoResourceRepository(VirtuosoMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Optional<Resource> findByIri(String iri) {
        return Optional.empty();
    }
}
