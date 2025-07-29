package com.himusharier.inventory.controller;

import com.himusharier.inventory.dto.request.ProductRequestDto;
import com.himusharier.inventory.dto.response.ProductResponseDto;
import com.himusharier.inventory.exception.ProductSubmissionException;
import com.himusharier.inventory.exception.ResourceNotFoundException;
import com.himusharier.inventory.model.Product;
import com.himusharier.inventory.service.ProductService;
import com.himusharier.inventory.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts() {
        List<ProductResponseDto> products = productService.getAllProducts();

        ApiResponse<List<ProductResponseDto>> response = new ApiResponse<>(
                true,
                "Products retrieved successfully.",
                products
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable UUID id) {
        try {
            ProductResponseDto product = productService.getProductById(id);

            ApiResponse<ProductResponseDto> response = new ApiResponse<>(
                    true,
                    "Product retrieved successfully.",
                    product
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductRequestDto productRequestDto) {
        try {
            Product product = Product.builder()
                    .name(productRequestDto.getName())
                    .description(productRequestDto.getDescription())
                    .price(productRequestDto.getPrice())
                    .quantity(productRequestDto.getQuantity())
                    .build();

            ProductResponseDto saveProduct = productService.createProduct(product);

            ApiResponse<ProductResponseDto> response = new ApiResponse<>(
                    true,
                    "Product saved successfully.",
                    saveProduct
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new ProductSubmissionException(e.getMessage());
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDto productRequestDto
    ) {
        try {
            Product product = Product.builder()
                    .name(productRequestDto.getName())
                    .description(productRequestDto.getDescription())
                    .price(productRequestDto.getPrice())
                    .quantity(productRequestDto.getQuantity())
                    .build();

            ProductResponseDto updateProduct = productService.updateProduct(id, product);

            ApiResponse<ProductResponseDto> response = new ApiResponse<>(
                    true,
                    "Product updated successfully.",
                    updateProduct
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new ProductSubmissionException(e.getMessage());
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable UUID id) {
        try {
            boolean deleteProduct = productService.deleteProduct(id);

            ApiResponse<String> response = new ApiResponse<>(
                    deleteProduct, //true
                    "Product deleted successfully."
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

}
