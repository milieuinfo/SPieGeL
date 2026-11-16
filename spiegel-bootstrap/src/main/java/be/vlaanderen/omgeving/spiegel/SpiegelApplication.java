package be.vlaanderen.omgeving.spiegel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point of SPieGeL. Lives in the root package so that component scanning covers every
 * adapter module.
 */
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class SpiegelApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpiegelApplication.class, args);
    }
}
