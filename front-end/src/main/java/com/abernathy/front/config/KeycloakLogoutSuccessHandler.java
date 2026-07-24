package com.abernathy.front.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Déconnexion RP-initiée : après avoir vidé la session locale, redirige le
 * navigateur vers l'endpoint de logout de Keycloak (avec {@code id_token_hint}
 * pour éviter la page de confirmation) afin de terminer aussi la session SSO,
 * puis revient sur la page d'accueil du front.
 *
 * <p>Handler maison plutôt que {@code OidcClientInitiatedLogoutSuccessHandler}
 * car la configuration du provider est manuelle (pas d'issuer-uri, donc pas de
 * métadonnée {@code end_session_endpoint} découverte automatiquement).
 */
public class KeycloakLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final String logoutUri;

    public KeycloakLogoutSuccessHandler(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(logoutUri)
                .queryParam("post_logout_redirect_uri", baseUrl(request));

        if (authentication instanceof OAuth2AuthenticationToken token
                && token.getPrincipal() instanceof OidcUser oidcUser) {
            builder.queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue());
        }

        response.sendRedirect(builder.build().toUriString());
    }

    private String baseUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                .replacePath("/")
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
