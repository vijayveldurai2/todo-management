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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String device = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ip, device));
    }

    /** Validates the stored token and returns the current user's profile.
     *  Called by the frontend on app startup to rehydrate session state. */
    @GetMapping("/me")
    public ResponseEntity<com.vijay.todo_management.dto.UserDto> me() {
        UserPrincipal principal = CurrentUserProvider.requireCurrentUserPrincipal();
        return ResponseEntity.ok(authService.me(principal.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        UserPrincipal principal = CurrentUserProvider.requireCurrentUserPrincipal();
        authService.logout(principal.getId(), principal.getJti());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll() {
        UserPrincipal principal = CurrentUserProvider.requireCurrentUserPrincipal();
        authService.logoutAll(principal.getId());
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
