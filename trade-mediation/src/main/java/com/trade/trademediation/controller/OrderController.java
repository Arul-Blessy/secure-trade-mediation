package com.trade.trademediation.controller;

import com.trade.trademediation.dto.OrderResponse;
import com.trade.trademediation.dto.PlaceOrderRequest;
import com.trade.trademediation.service.EscrowService;
import com.trade.trademediation.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final EscrowService escrowService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request,
                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request, auth));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('BUYER','SELLER')")
    public List<OrderResponse> mine(Authentication auth) {
        return orderService.myOrders(auth);
    }

    @PostMapping("/{id}/seller-agree")
    @PreAuthorize("hasRole('SELLER')")
    public OrderResponse sellerAgree(@PathVariable Long id, Authentication auth) {
        return orderService.sellerAgree(id, auth);
    }

    @PostMapping("/{id}/seller-cancel")
    @PreAuthorize("hasRole('SELLER')")
    public OrderResponse sellerCancel(@PathVariable Long id, Authentication auth) {
        return escrowService.sellerCancel(id, auth);
    }

    @PostMapping("/{id}/buyer-cancel")
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse buyerCancel(@PathVariable Long id, Authentication auth) {
        return escrowService.buyerCancel(id, auth);
    }

    @PostMapping("/{id}/confirm-receipt")
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse confirmReceipt(@PathVariable Long id, Authentication auth) {
        return escrowService.confirmReceipt(id, auth);
    }

    @PostMapping("/{id}/return-request")
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse returnRequest(@PathVariable Long id, Authentication auth) {
        return escrowService.requestReturn(id, auth);
    }
}