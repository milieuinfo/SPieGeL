package be.vlaanderen.omgeving.spiegel.config;

import be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.mapper.VirtuosoMapper;
import be.vlaanderen.omgeving.spiegel.adapter.out.virtuoso.repository.VirtuosoResourceRepository;
import be.vlaanderen.omgeving.spiegel.application.port.in.GetResourceUseCase;
import be.vlaanderen.omgeving.spiegel.application.port.out.ResourceRepository;
import be.vlaanderen.omgeving.spiegel.application.service.GetResourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free application services and outbound adapters into the Spring context.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    ResourceRepository resourceRepository() {
        return new VirtuosoResourceRepository(new VirtuosoMapper());
    }

    @Bean
    GetResourceUseCase getResourceUseCase(ResourceRepository resourceRepository) {
        return new GetResourceService(resourceRepository);
    }
}
