package be.vlaanderen.omgeving.spiegel.model;

import java.util.Map;

/**
 * A structured identifier of a resource published by SPieGeL..
 * tenant + pad + localid + resourceType are a combined alternate key of an endpoint configuration
 */
public record ResourceObject(String tenant, String pad, String localid, String resourceType, Map<String, String> variables) {

    public ResourceObject {
        if (tenant == null) {
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
