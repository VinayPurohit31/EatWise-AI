package com.nutriscan.eatwise_ai.service;


import com.nutriscan.eatwise_ai.model.ProductInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
public class ScoringService {

    /**
     * Compute a 0-100 score based on a simple rule-based approach.
     * This is intentionally simple for an MVP and for learning.
     */
    public int computeScore(ProductInfo product) {
        int score = 100;

        Map<String, Object> nutriments = product.getNutriments();
        // Nutrients often come as keys like "sugars_100g", "salt_100g", "fat_100g", "saturated-fat_100g", "energy-kcal_100g"
        // We'll parse common ones and apply penalties.

        double sugarPer100g = getDouble(nutriments, "sugars_100g");
        double saltPer100g = getDouble(nutriments, "salt_100g");
        double satFatPer100g = getDouble(nutriments, "saturated-fat_100g");
        double energyKcalPer100g = getDouble(nutriments, "energy-kcal_100g");

        // Penalty rules (example numbers)
        score -= clamp((int) Math.round(sugarPer100g * 2), 0, 30);      // up to 30 points penalty for sugar
        score -= clamp((int) Math.round(saltPer100g * 3), 0, 20);       // up to 20 for salt
        score -= clamp((int) Math.round(satFatPer100g * 2), 0, 20);     // up to 20 for sat fat
        score -= clamp((int) Math.round((energyKcalPer100g / 50)), 0, 10); // small energy penalty up to 10

        // Ingredients heuristics (very simple)
        String ingredients = product.getIngredientsText().toLowerCase();
        if (ingredients.contains("sodium benzoate") || ingredients.contains("benzoate")) {
            score -= 6;
        }
        if (ingredients.contains("high fructose corn syrup") || ingredients.contains("corn syrup")) {
            score -= 8;
        }
        if (ingredients.contains("artificial sweetener") || ingredients.contains("aspartame") || ingredients.contains("sucralose")) {
            score -= 6;
        }
        if (ingredients.contains("hydrogenated") || ingredients.contains("partially hydrogenated")) {
            score -= 10;
        }

        // Ensure score between 0 and 100
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    public String mapScoreToCategory(int score) {
        if (score >= 80) return "Healthy";
        if (score >= 50) return "Acceptable";
        return "Unhealthy";
    }

    private double getDouble(Map<String, Object> nutriments, String key) {
        try {
            Object v = nutriments.get(key);
            if (v == null) return 0.0;
            if (v instanceof Number) return ((Number) v).doubleValue();
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    @Service
    public static class ProductService {

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
}

