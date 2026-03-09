package com.bank.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the OpenAPI 3 specification exposed at {@code /v3/api-docs} and the
 * Swagger UI available at {@code /swagger-ui.html}.
 *
 * <p>The interactive UI allows developers to explore and manually test all API
 * endpoints without any additional tooling. It is enabled by default in all
 * environments; it should be disabled or access-restricted in production deployments
 * via the {@code springdoc.swagger-ui.enabled} property.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Builds and returns the {@link OpenAPI} bean that Springdoc uses to generate
     * the API specification.
     *
     * @return the configured {@link OpenAPI} descriptor
     */
    @Bean
    public OpenAPI coreBankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Banking API")
                        .version("1.0.0")
                        .description("""
                                A robust, monolithic Core Banking API built with Java 21 and Spring Boot 3.
                                Demonstrates Hexagonal Architecture, idempotent transfers via Redis,
                                Transactional Outbox Pattern with RabbitMQ, and full observability.
                                """)
                        .contact(new Contact()
                                .name("Core Banking Team")
                                .url("https://github.com/fabiankaraben/core-banking-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }
}
