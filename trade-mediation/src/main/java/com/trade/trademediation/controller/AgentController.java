package com.trade.trademediation.controller;

import com.trade.trademediation.dto.OrderResponse;
import com.trade.trademediation.dto.ReviewReturnRequest;
import com.trade.trademediation.service.EscrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class AgentController {

    private final EscrowService escrowService;

    @GetMapping("/notifications")
    public List<OrderResponse> notifications() {
        return escrowService.agentNotifications();
    }

    @PostMapping("/orders/{id}/in-transit")
    public OrderResponse inTransit(@PathVariable Long id) {
        return escrowService.markInTransit(id);
    }

    @PostMapping("/orders/{id}/delivered")
    public OrderResponse delivered(@PathVariable Long id) {
        return escrowService.markDelivered(id);
    }

    @PostMapping("/orders/{id}/review-return")
    public OrderResponse reviewReturn(@PathVariable Long id,
                                      @Valid @RequestBody ReviewReturnRequest request) {
        return escrowService.reviewReturn(id, request.decision());
    }
}