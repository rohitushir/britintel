package com.companieswatch.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless security. Authentication is delegated to Clerk: the SPA sends the Clerk session token
 * as a bearer token, which we validate as a JWT resource server.
 *
 * <ul>
 *   <li>Public: the marketing landing page ({@code /}), the dashboard SPA shell ({@code /app/**}),
 *       health, and {@code GET /api/config} (publishable key bootstrap).</li>
 *   <li>Everything under {@code /api/**} requires a valid Clerk JWT (401 otherwise).</li>
 *   <li>No sessions, no CSRF — bearer tokens are immune to CSRF and nothing relies on a cookie.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Let the container's internal ERROR dispatch render normally (a 404 must
                        // not be rewritten into a 401 by anyRequest().authenticated()).
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Public marketing landing page + the dashboard SPA shell (its API is guarded).
                        .requestMatchers("/", "/index.html", "/app", "/app/**",
                                "/favicon.ico", "/actuator/health").permitAll()
                        // Public bootstrap: the SPA fetches the Clerk publishable key before sign-in.
                        .requestMatchers(HttpMethod.GET, "/api/config").permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}
