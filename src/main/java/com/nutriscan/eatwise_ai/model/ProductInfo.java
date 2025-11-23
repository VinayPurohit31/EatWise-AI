package com.nutriscan.eatwise_ai.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProductInfo {
    private String barcode;
    private String productName;
    private String brand;
    private String ingredientsText;
    private Map<String, Object> nutriments; // raw nutriments JSON map (e.g. sugar_100g, salt_100g, energy-kcal)
}