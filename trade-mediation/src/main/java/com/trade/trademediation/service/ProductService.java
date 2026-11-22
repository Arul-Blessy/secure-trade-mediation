package com.trade.trademediation.service;

import com.trade.trademediation.config.TradeRules;
import com.trade.trademediation.dto.ProductRequest;
import com.trade.trademediation.dto.ProductResponse;
import com.trade.trademediation.entity.Product;
import com.trade.trademediation.entity.User;
import com.trade.trademediation.exception.ApiException;
import com.trade.trademediation.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {
        return productRepository.findByActiveTrue().stream().map(ProductResponse::from).toList();
    }

    @Transactional
    public void remove(Long productId, Authentication auth) {
        User seller = currentUserService.get(auth);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }
        product.setActive(false);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, Authentication auth) {
        User seller = currentUserService.get(auth);

        if (seller.getReputation().compareTo(TradeRules.MIN_SELLER_REPUTATION) < 0) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your reputation is too low to list new products");
        }

        Product product = productRepository.save(Product.builder()
                .seller(seller)
                .name(request.name())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .build());

        return ProductResponse.from(product);
    }
}