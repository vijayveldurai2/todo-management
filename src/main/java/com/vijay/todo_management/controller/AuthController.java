package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.LoginRequest;
import com.vijay.todo_management.dto.LoginResponse;
import com.vijay.todo_management.dto.SignupRequest;
import com.vijay.todo_management.dto.SignupResponse;
import com.vijay.todo_management.dto.VerifyResponse;
import com.vijay.todo_management.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
