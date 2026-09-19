package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.LoginRequest;
import com.vijay.todo_management.dto.LoginResponse;
import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;
import com.vijay.todo_management.dto.VerifyResponse;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.vijay.todo_management.dto.AuthSession;
import com.vijay.todo_management.dto.RefreshSession;
import com.vijay.todo_management.dto.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.auth.cookie.name:refresh_token}")
    private String cookieName;

    @Value("${app.auth.cookie.path:/api/auth}")
    private String cookiePath;

    @Value("${app.auth.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${app.jwt.refresh-expiration-sec:604800}")
    private long refreshExpirationSec;

    @Value("${app.auth.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    private ResponseCookie buildRefreshCookie(String token, long maxAge, HttpServletRequest request) {
        boolean secure = cookieSecure || request.isSecure();
        return ResponseCookie.from(cookieName, token != null ? token : "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(maxAge)
                .build();
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        return new ResponseEntity<>(authService.signup(request), HttpStatus.CREATED);
    }

    /** Used by the link in the verification email (?token=...). */
    @GetMapping("/verify")
    public ResponseEntity<VerifyResponse> verifyGet(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    /** Same verify flow for API clients that POST the token. */
    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verifyPost(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.verifyEmail(body.get("token")));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ip = getClientIp(httpRequest);
        String device = httpRequest.getHeader("User-Agent");
        AuthSession session = authService.loginSession(request, ip, device);

        ResponseCookie cookie = buildRefreshCookie(session.rawRefreshToken(), refreshExpirationSec, httpRequest);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(session.loginResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue(name = "${app.auth.cookie.name:refresh_token}", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            ResponseCookie clearCookie = buildRefreshCookie("", 0, httpRequest);
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String ip = getClientIp(httpRequest);
            String device = httpRequest.getHeader("User-Agent");
            RefreshSession session = authService.refresh(refreshToken, ip, device);

            ResponseCookie cookie = buildRefreshCookie(session.rawRefreshToken(), refreshExpirationSec, httpRequest);
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(session.response());
        } catch (Exception ex) {
            ResponseCookie clearCookie = buildRefreshCookie("", 0, httpRequest);
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /** Validates the stored token and returns the current user's profile.
     *  Called by the frontend on app startup to rehydrate session state. */
    @GetMapping("/me")
    public ResponseEntity<com.vijay.todo_management.dto.UserDto> me() {
        UserPrincipal principal = CurrentUserProvider.requireCurrentUserPrincipal();
        return ResponseEntity.ok(authService.me(principal.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "${app.auth.cookie.name:refresh_token}", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Optional<UserPrincipal> principal = CurrentUserProvider.getCurrentUserPrincipal();
        UUID userId = principal.map(UserPrincipal::getId).orElse(null);
        String jti = principal.map(UserPrincipal::getJti).orElse(null);

        authService.logout(userId, jti, refreshToken);

        ResponseCookie clearCookie = buildRefreshCookie("", 0, httpRequest);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @CookieValue(name = "${app.auth.cookie.name:refresh_token}", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Optional<UserPrincipal> principal = CurrentUserProvider.getCurrentUserPrincipal();
        UUID userId = principal.map(UserPrincipal::getId).orElse(null);

        authService.logoutAll(userId, refreshToken);

        ResponseCookie clearCookie = buildRefreshCookie("", 0, httpRequest);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
