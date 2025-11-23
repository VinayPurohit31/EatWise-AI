package com.nutriscan.eatwise_ai.webdto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ScanResponse {
    private String barcode;
    private String productName;
    private String brand;
    private int score;
    private String category;
    private Map<String, Object> nutriments;
    private String ingredientsText;
}