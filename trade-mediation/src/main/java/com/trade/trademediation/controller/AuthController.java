package com.trade.trademediation.controller;

import com.trade.trademediation.dto.AuthResponse;
import com.trade.trademediation.dto.LoginRequest;
import com.trade.trademediation.dto.RegisterRequest;
import com.trade.trademediation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
        return Map.of("username", authentication.getName(), "role", role);
    }
}