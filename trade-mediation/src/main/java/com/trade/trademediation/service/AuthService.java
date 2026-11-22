package com.trade.trademediation.service;

import com.trade.trademediation.dto.AuthResponse;
import com.trade.trademediation.dto.LoginRequest;
import com.trade.trademediation.dto.RegisterRequest;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.entity.Wallet;
import com.trade.trademediation.enums.Role;
import com.trade.trademediation.exception.ApiException;
import com.trade.trademediation.repository.UserRepository;
import com.trade.trademediation.repository.WalletRepository;
import com.trade.trademediation.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.AGENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Agent accounts cannot be self-registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = userRepository.save(User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build());

        walletRepository.save(Wallet.builder().user(user).build());

        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}