package com.trade.trademediation.dto;

import com.trade.trademediation.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        String seller
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(),
                p.getImageUrl(), p.getSeller().getUsername());
    }
}