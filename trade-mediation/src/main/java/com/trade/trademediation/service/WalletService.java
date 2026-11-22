package com.trade.trademediation.service;

import com.trade.trademediation.dto.TransactionResponse;
import com.trade.trademediation.dto.WalletResponse;
import com.trade.trademediation.entity.Order;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.entity.Wallet;
import com.trade.trademediation.entity.WalletTransaction;
import com.trade.trademediation.enums.WalletBucket;
import com.trade.trademediation.exception.ApiException;
import com.trade.trademediation.repository.WalletRepository;
import com.trade.trademediation.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @Transactional(readOnly = true)
    public WalletResponse myWallet(Authentication auth) {
        User user = currentUserService.get(auth);
        return view(user, walletOf(user));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> myTransactions(Authentication auth) {
        User user = currentUserService.get(auth);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(TransactionResponse::from).toList();
    }

    // Demo only: lets a tester add play money. Disabled when app.demo-mode=false.
    @Transactional
    public WalletResponse addMoney(BigDecimal amount, Authentication auth) {
        if (!demoMode) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Demo top-ups are disabled");
        }
        User user = currentUserService.get(auth);
        Wallet wallet = walletOf(user);
        wallet.setRegularBalance(wallet.getRegularBalance().add(amount));
        record(user, null, WalletBucket.REGULAR, amount, "TOP_UP");
        return view(user, wallet);
    }

    // Moves money from the user's regular wallet into their escrow wallet.
    // MANDATORY = this can only run inside another service's transaction,
    // so a half-finished money move is impossible.
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(User user, BigDecimal amount, Order order, String reason) {
        Wallet wallet = walletOf(user);
        if (wallet.getRegularBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in regular wallet");
        }
        wallet.setRegularBalance(wallet.getRegularBalance().subtract(amount));
        wallet.setEscrowBalance(wallet.getEscrowBalance().add(amount));
        record(user, order, WalletBucket.REGULAR, amount.negate(), reason);
        record(user, order, WalletBucket.ESCROW, amount, reason);
    }

    // Moves money OUT of 'from' user's escrow and INTO 'to' user's regular wallet.
    // If from and to are the same user, this simply releases or refunds escrow money.
    @Transactional(propagation = Propagation.MANDATORY)
    public void settle(User from, User to, BigDecimal amount, Order order, String reason) {
        if (amount.signum() == 0) {
            return;
        }
        if (amount.signum() < 0) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid settlement amount");
        }
        Wallet source = walletOf(from);
        if (source.getEscrowBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Escrow balance is inconsistent");
        }
        source.setEscrowBalance(source.getEscrowBalance().subtract(amount));

        Wallet target = walletOf(to);
        target.setRegularBalance(target.getRegularBalance().add(amount));

        record(from, order, WalletBucket.ESCROW, amount.negate(), reason);
        record(to, order, WalletBucket.REGULAR, amount, reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void addPenalty(User user, BigDecimal amount) {
        Wallet wallet = walletOf(user);
        wallet.setPenaltiesTotal(wallet.getPenaltiesTotal().add(amount));
    }

    private Wallet walletOf(User user) {
        return walletRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    private void record(User user, Order order, WalletBucket bucket, BigDecimal amount, String reason) {
        transactionRepository.save(WalletTransaction.builder()
                .user(user).order(order).bucket(bucket).amount(amount).reason(reason)
                .build());
    }

    private WalletResponse view(User user, Wallet wallet) {
        return new WalletResponse(user.getUsername(), user.getRole().name(),
                wallet.getRegularBalance(), wallet.getEscrowBalance(),
                wallet.getPenaltiesTotal(), user.getReputation());
    }
}