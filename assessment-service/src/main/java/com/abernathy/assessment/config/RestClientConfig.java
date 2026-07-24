package com.abernathy.assessment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * RestClient utilisé pour rappeler la gateway. L'assessment-service n'a pas
 * d'identité propre : il <b>propage le JWT entrant</b> (celui du praticien
 * connecté) lu dans le SecurityContext, qui dispose déjà des droits de lecture
 * sur les patients et les notes.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient gatewayRestClient(@Value("${services.gateway-url}") String gatewayUrl) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestInterceptor(bearerTokenForwardingInterceptor())
                .build();
    }

    private ClientHttpRequestInterceptor bearerTokenForwardingInterceptor() {
        return (request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
            }
            return execution.execute(request, body);
        };
    }
}
