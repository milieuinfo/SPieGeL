package be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint.repository;

import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.model.Profile;
import be.vlaanderen.omgeving.spiegel.model.SpResource;
import be.vlaanderen.omgeving.spiegel.model.UserContext;
import org.apache.commons.rdf.api.Graph;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

/**
 * Reads resources from a generic SPARQL endpoint using the standard SPARQL 1.1 Protocol
 * <p>
 * Todo: Configure this repository instance with the appropriate configuration class(es) during bootstrap.
 * Todo: For a given resource and profile get the query-templates to use (i.e. from its configuration instantiated during bootstrap)..
 */
public class SparqlEndpointRepository implements ResourceRepository {

    /**
     * Endpoint that implements the SPARQL 1.1 Query operations of the SPARQL 1.1 protocol
     *
     * @see <http://www.w3.org/TR/sparql11-query/>
     *
     */
    private URI sparqlQueryEndpoint;
    private HttpClient httpClient;

    public SparqlEndpointRepository(URI sparqlQueryEndpoint, HttpClient httpClient) {
        this.sparqlQueryEndpoint = sparqlQueryEndpoint;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<Graph> findByResource(UserContext userContext, SpResource spResource, Profile profile) {
        // todo get query-templates based on resource object and profile and configuration
        // todo resolve query-templates and execute queries
        // todo map and return the resulting jena model to the commons rdf graph model
        return Optional.empty();
    }


    private Model executeConstructQueries(List<String> queries, QuerySolutionMap bindings) {
        Model result = ModelFactory.createDefaultModel();
        queries.forEach(query -> result.add(executeConstructQuery(query, bindings)));
        return result;
    }

    private Model executeConstructQuery(String query, QuerySolutionMap bindings) {
        Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
        return executeConstructQuery(parsedQuery, bindings);
    }

    public Model executeConstructQuery(Query query, QuerySolutionMap bindings) {

        query = buildQuery(query, bindings);
        String sparqlEndpointUrl = sparqlQueryEndpoint.toString();
        try (QueryExecution queryExecution = QueryExecutionHTTP.service(sparqlEndpointUrl).httpClient(httpClient).query(query).timeout(-1).build()) {
//   TODO   if (modelContentType != null) queryExecution.setModelContentType(modelContentType);
            return queryExecution.execConstruct();
        }
        catch (Exception e) {
            throw e;
        }
    }

    private Query buildQuery(Query query, QuerySolutionMap bindings) {
        ParameterizedSparqlString string = new ParameterizedSparqlString(query.toString(), bindings);
        query = string.asQuery();
        return query;
    }

//    protected HttpClient initJavaHttpClient(String user, String password) {
//        java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder();
////        Authenticator authenticator = AuthLib.authenticator(user, password); JENA does also have supporting libs! see https://github.com/apache/jena/blob/main/jena-examples/src/main/java/arq/examples/auth/ExAuth02_QueryExecutionPW.java
//        Authenticator authenticator = new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                PasswordAuthentication passwordAuthentication = new PasswordAuthentication(user, password.toCharArray());
//                return passwordAuthentication;
//            }
//        };
//        builder.authenticator(authenticator);
//        return builder.build();
//    }


}
