package be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper;

import be.vlaanderen.omgeving.spiegel.adapter.in.rest.model.ResourceResponse;
import be.vlaanderen.omgeving.spiegel.model.Resource;
import org.springframework.stereotype.Component;

/**
 * Maps domain resources to their REST representation.
 */
@Component
public class ResourceMapper {

    public ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(resource.iri());
    }
}
