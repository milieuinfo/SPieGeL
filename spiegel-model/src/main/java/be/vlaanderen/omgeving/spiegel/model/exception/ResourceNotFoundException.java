package be.vlaanderen.omgeving.spiegel.model.exception;

/**
 * Thrown when no resource is known for a requested IRI.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String iri;

    public ResourceNotFoundException(String iri) {
        super("No resource found for IRI " + iri);
        this.iri = iri;
    }

    public String getIri() {
        return iri;
    }
}
