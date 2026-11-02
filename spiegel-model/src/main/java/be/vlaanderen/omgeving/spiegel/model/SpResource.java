package be.vlaanderen.omgeving.spiegel.model;

import java.util.Map;

/**
 * A structured identifier of a resource published by SPieGeL.
 * <p>
 * This record represents a resource within a given namespace, identified by a path
 * and a local identifier. Namespace, path, and local id together uniquely define the id of the resource.
 * It also carries a resource type, optional variables
 * (e.g. to support dynamic logic in post processing, building queries,...), and an optional configuration id.
 * <p>
 * The spiegel model used in the spiegel-application (the core of the hexagon) is not aware
 * of the specific configuration files and formats used by the bootstrap.
 * It uses its own domain model and configuration model and must be able to do its job based on this
 * (e.g. selecting the appropriate outbound adapter to get a description of this resource).
 * The bootstrap is responsible for configuring the application with the appropriate configuration classes.
 * The same applies to the outbound adapters: they define their own configuration classes
 * and must be able to do their job based on this and eventually other input (e.g. selecting the appropriate query templates to query the description of this resource).
 * The inbound adapters that drive the application (i.e spring mvc rest adapter) must be able to generate such a record (in their specific context)
 * and pass it to the application for use cases (inbound ports) that require this record.
 *
 * @param namespace    the namespace (tenant) this resource belongs to; must not be {@code null}
 * @param pad          the path under which the resource is published; must not be {@code null}
 * @param localid      the local identifier of the resource within the path; must not be {@code null}
 * @param resourceType <strong>Experimental:</strong> a type of the resource (not necessarily the rdfs:Class); must not be {@code null}.
 *                     Not yet fully defined, but it acts as an extra discriminator to identify a subset of resources
 *                     (e.g. to identify a specific dataset, graph, collection, or class of resources; they might be
 *                     hosted by another data source to get a description (i.e data) of them).
 * @param variables    optional variables (e.g. to support dynamic logic in post processing, building queries,...); may be empty
 * @param configid     <strong>Experimental:</strong> identifies the resolved configuration by a driving input port adapter to use for this resource. It is a temporary hack.
 *                      This field is unstable and will be removed in a future release
 *                      without prior notice. Use with caution.
 */
public record SpResource(String namespace, String pad, String localid, String resourceType, Map<String, String> variables, String configid) {

    public SpResource {
        if (namespace == null) {// SpglResource
            throw new IllegalArgumentException("Tenant cannot be null");
        }
        if (pad == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }
        if (localid == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Roles cannot be null");
        }
        //
    }
}
