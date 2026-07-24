package com.abernathy.front.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Le front-end est un client OAuth2/OIDC. L'utilisateur s'authentifie auprès de
 * Keycloak via le flow Authorization Code + PKCE ; Spring conserve le token en
 * session et le relaie ensuite vers la gateway (voir {@link RestClientConfig}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(login -> login
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(pkceAuthorizationRequestResolver(clientRegistrationRepository))))
            .logout(logout -> logout
                .logoutSuccessHandler(keycloakLogoutSuccessHandler()));

        return http.build();
    }

    /**
     * Force l'utilisation de PKCE (S256) même si le client est confidentiel.
     */
    private OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /**
     * Déconnexion RP-initiée : termine aussi la session côté Keycloak puis
     * renvoie l'utilisateur sur la page d'accueil.
     */
    private LogoutSuccessHandler keycloakLogoutSuccessHandler() {
        return new KeycloakLogoutSuccessHandler(keycloakLogoutUri);
    }
}
