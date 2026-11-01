package be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;
import org.apache.jena.rdf.model.Model;

/**
 * Maps domain rdf description model to rdf description model used in rest adapter (jena model)
 * Todo define both models.
 */
@Component
public class ResourceMapper {

    public Model toResponse(Object rdfdescription) {
        return null; // todo
    }
}
