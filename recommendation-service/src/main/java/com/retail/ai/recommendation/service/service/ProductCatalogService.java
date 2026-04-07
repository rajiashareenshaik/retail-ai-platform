package com.retail.ai.recommendation.service.service;

import com.retail.ai.recommendation.service.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductCatalogService {

    private final Map<String, Product> products = new LinkedHashMap<>();

    @PostConstruct
    void loadCatalog() {
        products.put("sku-100", Product.builder().id("sku-100").name("Performance Running Shoes").category("footwear").price(new BigDecimal("89.99")).tags(List.of("running", "sport", "comfort")).description("Lightweight daily trainer built for comfort.").build());
        products.put("sku-101", Product.builder().id("sku-101").name("Sports Crew Socks").category("accessories").price(new BigDecimal("12.99")).tags(List.of("running", "bundle", "comfort")).description("Cushioned socks that pair well with training shoes.").build());
        products.put("sku-102", Product.builder().id("sku-102").name("Hydration Bottle").category("accessories").price(new BigDecimal("18.50")).tags(List.of("fitness", "training", "bundle")).description("Insulated bottle for workouts and everyday use.").build());
        products.put("sku-103", Product.builder().id("sku-103").name("Yoga Mat").category("fitness").price(new BigDecimal("29.00")).tags(List.of("fitness", "home", "wellness")).description("Non-slip mat for home workouts and stretching.").build());
        products.put("sku-104", Product.builder().id("sku-104").name("Wireless Earbuds").category("electronics").price(new BigDecimal("59.99")).tags(List.of("music", "training", "portable")).description("Sweat-resistant earbuds for workouts.").build());
        products.put("sku-105", Product.builder().id("sku-105").name("Compression Shirt").category("apparel").price(new BigDecimal("34.99")).tags(List.of("sport", "running", "apparel")).description("Breathable training shirt with a close fit.").build());
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public List<Product> getRecommendations(List<String> cartProductIds) {
        Set<String> cartSet = new HashSet<>(Optional.ofNullable(cartProductIds).orElseGet(List::of));
        Set<String> cartTags = cartSet.stream()
                .map(products::get)
                .filter(Objects::nonNull)
                .flatMap(product -> product.getTags().stream())
                .collect(Collectors.toSet());

        return products.values().stream()
                .filter(product -> !cartSet.contains(product.getId()))
                .sorted(Comparator.comparingInt((Product product) -> overlap(product.getTags(), cartTags)).reversed())
                .limit(3)
                .toList();
    }

    private int overlap(List<String> left, Set<String> right) {
        int score = 0;
        for (String tag : left) {
            if (right.contains(tag)) {
                score++;
            }
        }
        return score;
    }
}
