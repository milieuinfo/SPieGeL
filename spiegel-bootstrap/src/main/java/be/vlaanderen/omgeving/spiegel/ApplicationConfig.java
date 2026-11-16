package be.vlaanderen.omgeving.spiegel;

import be.vlaanderen.omgeving.spiegel.adapter.out.sparqlendpoint.mapper.JenaModelMapper;
import be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository.VirtuosoResourceRepository;
import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.application.service.GetResourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configures, instantiates, and wires the (framework-free) application services, in- and outbound adapters into the Spring context.
 */
@Configuration
@ComponentScan(basePackages = {"be.vlaanderen.omgeving.spiegel"})
public class ApplicationConfig {

    @Bean
    ResourceRepository resourceRepository() {
        return new VirtuosoResourceRepository(new JenaModelMapper());
    }

    @Bean
    GetResourceUseCase getResourceUseCase(ResourceRepository resourceRepository) {
        return new GetResourceService(resourceRepository);
    }
}
