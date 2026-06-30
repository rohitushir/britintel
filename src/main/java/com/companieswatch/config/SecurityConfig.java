package com.companieswatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Simple session-based security (mvp-scope.md: "Spring Security, simple").
 *
 * <ul>
 *   <li>Public: the static dashboard, health, and self-service registration.</li>
 *   <li>Everything else under {@code /api/**} requires an authenticated session.</li>
 *   <li>Login/logout use Spring's built-in form login (default login page) — zero custom UI.</li>
 *   <li>CSRF protection is on, with a JS-readable {@code XSRF-TOKEN} cookie so the single-page
 *       dashboard can send the {@code X-XSRF-TOKEN} header on state-changing calls.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/app.js", "/style.css",
                                "/favicon.ico", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/register").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler)
                        // Registration is a public bootstrap call made before any token exists.
                        .ignoringRequestMatchers("/api/register"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .formLogin(form -> form
                        // Use our own single page as the login page (no default Spring page).
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/?loggedout")
                        .permitAll())
                // Return 401 (not a redirect) when an unauthenticated request hits the JSON API.
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) ->
                                        response.sendError(401, "Unauthorized"),
                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
