package be.vlaanderen.omgeving.spiegel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration in YAML, and its mapping to the domain model
 */
@ConfigurationProperties(prefix = "spiegel")
public class ApplicationProperties {
}
