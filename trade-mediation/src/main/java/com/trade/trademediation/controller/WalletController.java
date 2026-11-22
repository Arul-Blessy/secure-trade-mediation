package com.trade.trademediation.controller;

import com.trade.trademediation.dto.AddMoneyRequest;
import com.trade.trademediation.dto.TransactionResponse;
import com.trade.trademediation.dto.WalletResponse;
import com.trade.trademediation.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public WalletResponse me(Authentication auth) {
        return walletService.myWallet(auth);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(Authentication auth) {
        return walletService.myTransactions(auth);
    }

    @PostMapping("/add-money")
    public WalletResponse addMoney(@Valid @RequestBody AddMoneyRequest request, Authentication auth) {
        return walletService.addMoney(request.amount(), auth);
    }
}