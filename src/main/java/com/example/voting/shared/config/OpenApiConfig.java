package com.example.voting.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI votingApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cooperative Voting API")
                        .version("v1")
                        .description(
                                """
                                Gerencia pautas e sessões de votação em assembleias de cooperativas. \
                                Cada associado tem direito a um voto por pauta.

                                Todo erro é devolvido no formato RFC 7807 (`application/problem+json`), \
                                com o motivo no campo `detail`.
                                """)
                        .license(new License().name("MIT")));
    }
}
