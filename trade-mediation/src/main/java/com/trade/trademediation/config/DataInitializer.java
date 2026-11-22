package com.trade.trademediation.config;

import org.springframework.beans.factory.annotation.Value;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.entity.Wallet;
import com.trade.trademediation.enums.Role;
import com.trade.trademediation.repository.UserRepository;
import com.trade.trademediation.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @Override
    @Transactional
    public void run(String... args) {
        createIfMissing("Blessy_buyer", Role.BUYER);
        createIfMissing("Blessy_seller", Role.SELLER);
        createIfMissing("Blessy_agent", Role.AGENT);
    }

    private void createIfMissing(String username, Role role) {

        if (!demoMode) {
            return;
        }

        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode("12345678"))
                .role(role)
                .build());

        walletRepository.save(Wallet.builder()
                .user(user)
                .regularBalance(new BigDecimal("30000.00"))
                .build());
    }
}