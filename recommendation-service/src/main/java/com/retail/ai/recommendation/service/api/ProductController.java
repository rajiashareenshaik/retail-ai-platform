package com.retail.ai.recommendation.service.api;

import com.retail.ai.recommendation.service.model.Product;
import com.retail.ai.recommendation.service.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCatalogService productCatalogService;

    @GetMapping
    public List<Product> getProducts() {
        return productCatalogService.getAllProducts();
    }

    @PostMapping("/recommendations")
    public List<Product> getRecommendations(@RequestBody List<String> cartProductIds) {
        return productCatalogService.getRecommendations(cartProductIds);
    }
}
