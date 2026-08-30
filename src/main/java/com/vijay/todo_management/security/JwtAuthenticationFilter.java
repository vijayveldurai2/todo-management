package com.vijay.todo_management.security;

import com.vijay.todo_management.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public static final String AUTH_TRANSPORT_ATTR = "AUTH_TRANSPORT";
    public static final String TRANSPORT_HEADER = "HEADER";
    public static final String TRANSPORT_COOKIE = "COOKIE";

    private final JwtService jwtService;
    private final RedisSessionService redisSessionService;

    public JwtAuthenticationFilter(JwtService jwtService, RedisSessionService redisSessionService) {
        this.jwtService = jwtService;
        this.redisSessionService = redisSessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ExtractedToken extracted = extractTokenWithTransport(request);

        if (extracted != null && extracted.token() != null && !extracted.token().isBlank()) {
            try {
                Claims claims = jwtService.parseClaims(extracted.token());
                UserPrincipal principal = UserPrincipal.fromClaims(claims);

                if (principal.getId() != null && principal.getJti() != null) {
                    if (redisSessionService.isValidSession(principal.getId(), principal.getJti())) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        request.setAttribute(AUTH_TRANSPORT_ATTR, extracted.transport());

                        // Update lastSeenAt asynchronously / non-blocking
                        String clientIp = getClientIp(request);
                        redisSessionService.touchSession(principal.getId(), principal.getJti(), clientIp);
                    } else {
                        log.debug("Session revoked or expired in Redis for user: {}, jti: {}", principal.getId(), principal.getJti());
                        SecurityContextHolder.clearContext();
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Failed to validate JWT token: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private ExtractedToken extractTokenWithTransport(HttpServletRequest request) {
        // 1. Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return new ExtractedToken(authHeader.substring(7).trim(), TRANSPORT_HEADER);
        }

        // 2. Check Cookies (access_token, jwt, or token)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName()) || "jwt".equals(cookie.getName()) || "token".equals(cookie.getName())) {
                    if (cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        return new ExtractedToken(cookie.getValue().trim(), TRANSPORT_COOKIE);
                    }
                }
            }
        }

        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record ExtractedToken(String token, String transport) {}
}
