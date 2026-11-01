package be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper;

import org.apache.jena.rdf.model.Model;

/**
 * Maps Jena models returned by Virtuoso to domain resources.
 */
public class VirtuosoMapper {

    /**
     * Maps the description of a resource to the domain model.
     *
     * @param model the description returned by Virtuoso
     * @return the resource
     */
    public Object toResource(Model model) {
        return null; // todo;
    }
}
