package com.proitbridge.lms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(prefix = "lms.sso", name = "enabled", havingValue = "true")
public class SsoOAuthClientConfig {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${lms.sso.issuer-uri:${SSO_ISSUER_URI:}}") String issuer,
            @Value("${lms.sso.client-id:${SSO_CLIENT_ID:}}") String clientId,
            @Value("${lms.sso.client-secret:${SSO_CLIENT_SECRET:}}") String clientSecret) {
        if (issuer == null || issuer.isBlank() || clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "SSO is enabled but SSO_ISSUER_URI, SSO_CLIENT_ID or SSO_CLIENT_SECRET is missing.");
        }
        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuer)
                .registrationId("lms")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
