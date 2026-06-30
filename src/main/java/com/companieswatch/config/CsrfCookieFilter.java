package com.companieswatch.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forces the {@link CsrfToken} to be rendered on each request so the {@code XSRF-TOKEN} cookie is
 * actually written for the single-page dashboard to read. Without this, Spring Security 6 defers
 * token loading and the cookie may never be set for a JS client. (Documented SPA pattern.)
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // triggers the cookie to be written
        }
        filterChain.doFilter(request, response);
    }
}
