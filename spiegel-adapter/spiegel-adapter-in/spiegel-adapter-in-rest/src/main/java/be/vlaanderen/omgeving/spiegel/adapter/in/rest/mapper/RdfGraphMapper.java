package be.vlaanderen.omgeving.spiegel.adapter.in.rest.mapper;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Maps domain rdf description model (commons rdf) to rdf description model used in rest adapter (jena model)
 */
@Component
public class RdfGraphMapper {

    public Model toJenaModel(Graph rdfdescription) {
        Model jenaModel = ModelFactory.createDefaultModel();
        rdfdescription.iterate().forEach(triple -> addTriple(jenaModel, triple));
        return jenaModel;
    }

    private void addTriple(Model jenaModel, Triple triple) {
        jenaModel.add(createResource(jenaModel, triple.getSubject()),
                createProperty(jenaModel, triple.getPredicate()), createRdfNode(jenaModel, triple.getObject()));
    }

    private Resource createResource(Model jenaModel, BlankNodeOrIRI rdfTerm) {
        if (rdfTerm instanceof IRI iri) {
            return jenaModel.createResource(iri.getIRIString());
        }
        else if (rdfTerm instanceof BlankNode bnode) {
            return jenaModel.createResource(AnonId.create(bnode.uniqueReference()));
        }
        else {
            throw new IllegalArgumentException("Unknown BlankNodeOrIRI type: " + rdfTerm.getClass().getName());
        }
    }

    private Property createProperty(Model jenaModel, IRI predicate) {
        return jenaModel.createProperty(predicate.getIRIString());
    }

    private RDFNode createRdfNode(Model jenaModel, RDFTerm object) {
        if (object instanceof BlankNodeOrIRI blanknodeOrIri) {
            return createResource(jenaModel, blanknodeOrIri);
        }
        else if (object instanceof Literal literal) {
            Optional<String> languageTag = literal.getLanguageTag();
            IRI datatype = literal.getDatatype();
            return languageTag.map(language -> jenaModel.createLiteral(literal.getLexicalForm(), language))
                    .orElse(jenaModel.createTypedLiteral(literal.getLexicalForm(), TypeMapper.getInstance().getSafeTypeByName(datatype.getIRIString())));
        }
        else {
            throw new IllegalArgumentException("Unknown RDF term type: " + object.getClass().getName());
        }
    }

}
