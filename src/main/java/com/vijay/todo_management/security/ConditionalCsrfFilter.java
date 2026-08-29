package com.vijay.todo_management.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Component
public class ConditionalCsrfFilter extends OncePerRequestFilter {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME_ALT = "X-CSRF-TOKEN";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Ensure XSRF-TOKEN cookie is set on GET/OPTIONS requests or if missing
        ensureCsrfCookiePresent(request, response);

        String authTransport = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_TRANSPORT_ATTR);
        String method = request.getMethod().toUpperCase();

        // If safe method or header-based auth or unauthenticated, skip CSRF verification
        if (SAFE_METHODS.contains(method) || JwtAuthenticationFilter.TRANSPORT_HEADER.equals(authTransport)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If authenticated via COOKIE on a state-changing method (POST, PUT, PATCH, DELETE), enforce CSRF check
        if (JwtAuthenticationFilter.TRANSPORT_COOKIE.equals(authTransport)) {
            String csrfCookie = getCsrfCookie(request);
            String csrfHeader = getCsrfHeader(request);

            if (csrfCookie == null || csrfCookie.isBlank() || csrfHeader == null || !csrfCookie.equals(csrfHeader)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Invalid or missing CSRF token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void ensureCsrfCookiePresent(HttpServletRequest request, HttpServletResponse response) {
        String existingToken = getCsrfCookie(request);
        if (existingToken == null || existingToken.isBlank()) {
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String newToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

            ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE_NAME, newToken)
                    .path("/")
                    .httpOnly(false) // Readable by JavaScript for double-submit
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .maxAge(86400) // 1 day
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }

    private String getCsrfCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String getCsrfHeader(HttpServletRequest request) {
        String header = request.getHeader(CSRF_HEADER_NAME);
        if (header == null || header.isBlank()) {
            header = request.getHeader(CSRF_HEADER_NAME_ALT);
        }
        return header != null ? header.trim() : null;
    }
}
