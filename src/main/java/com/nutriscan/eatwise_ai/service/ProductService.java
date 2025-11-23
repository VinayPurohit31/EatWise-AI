package com.nutriscan.eatwise_ai.service;

import com.nutriscan.eatwise_ai.model.ProductInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private final WebClient webClient;

    public ProductService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetch product from OpenFoodFacts for the provided barcode.
     * Returns Optional.empty() if not found or on error.
     */
    public Optional<ProductInfo> fetchProductByBarcode(String barcode) {
        try {
            Mono<Map> mono = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v0/product/{barcode}.json").build(barcode))
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> response = mono.block(); // block for simplicity in this tutorial code
            if (response == null) return Optional.empty();

            Object status = response.get("status");
            if (status instanceof Integer && (Integer) status == 1) {
                Map<String, Object> product = (Map<String, Object>) response.get("product");
                String productName = (String) product.getOrDefault("product_name", "Unknown product");
                String brand = "Unknown";
                if (product.containsKey("brands")) {
                    brand = (String) product.getOrDefault("brands", "Unknown");
                }
                String ingredientsText = (String) product.getOrDefault("ingredients_text", "");
                Map<String, Object> nutriments = (Map<String, Object>) product.getOrDefault("nutriments", Map.of());

                ProductInfo info = ProductInfo.builder()
                        .barcode(barcode)
                        .productName(productName)
                        .brand(brand)
                        .ingredientsText(ingredientsText)
                        .nutriments(nutriments)
                        .build();

                return Optional.of(info);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            // In production: log details and handle specific errors
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
