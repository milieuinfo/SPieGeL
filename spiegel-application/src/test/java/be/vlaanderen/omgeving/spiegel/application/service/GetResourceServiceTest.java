package be.vlaanderen.omgeving.spiegel.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.vlaanderen.omgeving.spiegel.model.Resource;
import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetResourceServiceTest {

    private static final String IRI = "https://example.org/id/1";

    @Test
    void returnsResourceFromRepository() {
        var service = new GetResourceService(iri -> Optional.of(new Resource(iri)));

        assertThat(service.getResource(IRI)).isEqualTo(new Resource(IRI));
    }

    @Test
    void unknownResourceIsNotFound() {
        var service = new GetResourceService(iri -> Optional.empty());

        assertThatThrownBy(() -> service.getResource(IRI))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(IRI);
    }
}
