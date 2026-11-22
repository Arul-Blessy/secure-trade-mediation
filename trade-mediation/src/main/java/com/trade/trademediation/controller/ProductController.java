package com.trade.trademediation.controller;

import com.trade.trademediation.dto.ProductRequest;
import com.trade.trademediation.dto.ProductResponse;
import com.trade.trademediation.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> remove(@PathVariable Long id, Authentication auth) {
        productService.remove(id, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                  Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request, auth));
    }
}