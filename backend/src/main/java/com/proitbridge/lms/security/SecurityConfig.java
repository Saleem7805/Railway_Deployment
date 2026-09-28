package com.proitbridge.lms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${lms.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${lms.sso.enabled:false}")
    private boolean ssoEnabled;

    @Value("${lms.csrf.enabled:true}")
    private boolean csrfEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter,
                                           SsoSuccessHandler ssoSuccessHandler) throws Exception {
        http
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/forgot", "/api/auth/reset",
                                 "/api/auth/sso/start", "/api/health", "/api/public/faqs",
                                 "/oauth2/**", "/login/oauth2/**").permitAll()
                /* the check lists rooms and who can get in, so it is staff only, and it
                   has to sit above the join rule or the wildcard swallows it */
                .requestMatchers("/api/slots/join-check").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/video/**", "/api/videos/**", "/api/faqs/**", "/api/slots/*/join").authenticated()
                .requestMatchers("/api/learner/**").hasRole("LEARNER")
                .requestMatchers("/api/staff/study/**").hasAnyRole("MENTOR", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/mentor/**").hasAnyRole("MENTOR", "ADMIN", "SUPER_ADMIN")
                /* the shared week: every host sees it, and the controller decides per row
                   what they may change */
                .requestMatchers("/api/week/**").hasAnyRole("MENTOR", "ADMIN", "SUPER_ADMIN")
                /* analytics: scoped in the service, so a mentor sees their own learners */
                .requestMatchers("/api/analytics/**").hasAnyRole("MENTOR", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                /* the course is edited by both, so an admin is not blocked on one person
                   to fix a title. People, roles and toggles stay super admin only. */
                .requestMatchers("/api/super/catalogue/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                /* the track feature toggles: an admin decides what a track includes, so
                   they own this even though it sits under the super namespace */
                .requestMatchers("/api/super/features/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/super/features").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/super/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated())
            /*
             * Without this, Spring answers an unauthenticated request with 403, because
             * the anonymous authentication counts as "present but not allowed". The
             * client cannot tell that apart from a genuine permission problem, so a
             * learner whose session was replaced would sit there watching every request
             * fail with a token it never thought to discard. 401 is the honest answer,
             * and it is what tells the client to sign out.
             */
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        /*
         * CSRF protection. Every POST, PUT, PATCH and DELETE must carry the X-XSRF-TOKEN
         * header, matching the XSRF-TOKEN cookie the server hands out. api/client.js
         * does this for every request. GET, HEAD and OPTIONS are never checked, which is
         * why nothing that only reads data has to change.
         *
         * The bearer token already protects most calls, since another site cannot make
         * a browser add an Authorization header. This covers what the bearer token does
         * not: the sign-in, forgot and reset forms, which run before there is a token,
         * and any request that arrives with a session cookie instead of a bearer header.
         *
         * The OIDC callback is exempt: it is protected by the OAuth2 state parameter,
         * and some providers deliver it as a cross-site form POST.
         */
        if (csrfEnabled) {
            CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            csrfRepository.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
            http.csrf(c -> c
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers("/login/oauth2/**"));
        } else {
            http.csrf(c -> c.disable());
        }

        if (ssoEnabled) {
            http.oauth2Login(o -> o.successHandler(ssoSuccessHandler));
        }
        return http.build();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("X-Session-Ended"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
