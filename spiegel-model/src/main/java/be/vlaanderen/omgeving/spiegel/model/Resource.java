package be.vlaanderen.omgeving.spiegel.model;

import java.util.Objects;

/**
 * A resource published by SPieGeL, identified by its IRI.
 *
 * @param iri the IRI that identifies the resource
 */
public record Resource(String iri) {

    public Resource {
        Objects.requireNonNull(iri, "iri");
    }
}
