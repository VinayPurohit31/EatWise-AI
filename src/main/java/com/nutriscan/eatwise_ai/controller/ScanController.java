package com.nutriscan.eatwise_ai.controller;


import com.nutriscan.eatwise_ai.service.ProductService;
import com.nutriscan.eatwise_ai.service.ScoringService;
import com.nutriscan.eatwise_ai.webdto.ScanRequest;
import com.nutriscan.eatwise_ai.webdto.ScanResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private final ProductService productService;
    private final ScoringService scoringService;

    public ScanController(ProductService productService, ScoringService scoringService) {
        this.productService = productService;
        this.scoringService = scoringService;
    }

    @PostMapping("/barcode")
    public ResponseEntity<ScanResponse> scanBarcode(@RequestBody ScanRequest request) {
        var productOptional = productService.fetchProductByBarcode(request.getBarcode());
        if (productOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var product = productOptional.get();
        int score = scoringService.computeScore(product);
        String category = scoringService.mapScoreToCategory(score);

        ScanResponse resp = ScanResponse.builder()
                .barcode(request.getBarcode())
                .productName(product.getProductName())
                .brand(product.getBrand())
                .score(score)
                .category(category)
                .nutriments(product.getNutriments())
                .ingredientsText(product.getIngredientsText())
                .build();

        return ResponseEntity.ok(resp);
    }
}

