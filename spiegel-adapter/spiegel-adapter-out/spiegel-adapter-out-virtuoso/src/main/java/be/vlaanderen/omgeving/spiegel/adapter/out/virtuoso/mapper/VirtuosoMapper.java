package be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper;

import be.vlaanderen.omgeving.spiegel.model.Resource;
import org.apache.jena.rdf.model.Model;

/**
 * Maps Jena models returned by Virtuoso to domain resources.
 */
public class VirtuosoMapper {

    /**
     * Maps the description of a resource to the domain model.
     *
     * @param iri the IRI of the described resource
     * @param model the description returned by Virtuoso
     * @return the resource
     */
    public Resource toResource(String iri, Model model) {
        return new Resource(iri);
    }
}
