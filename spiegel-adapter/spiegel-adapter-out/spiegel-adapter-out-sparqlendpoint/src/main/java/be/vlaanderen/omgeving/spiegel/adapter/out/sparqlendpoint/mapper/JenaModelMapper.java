package be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint.mapper;

import org.apache.commons.rdf.api.Graph;
import org.apache.jena.rdf.model.Model;

/**
 * Maps Jena models to used domain model Rdf Graph.
 */
public class JenaModelMapper {
    /**
     * Maps the description of a resource to the domain model.
     *
     * @param model the rdf description
     * @return the Graph
     */
    public Graph toRdfGraph(Model model) {
        return null; // todo;
    }
}
