package com.himusharier.inventory.service;

import com.himusharier.inventory.dto.response.ProductResponseDto;
import com.himusharier.inventory.exception.ResourceNotFoundException;
import com.himusharier.inventory.model.Product;
import com.himusharier.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private UUID testProductId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testProduct = Product.builder()
                .productId(testProductId)
                .name("Test Product")
                .description("Test Description")
                .price(99.99)
                .quantity(10)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllProducts_ShouldReturnListOfProductResponseDto() {
        // Given
        List<Product> products = Arrays.asList(testProduct, 
            Product.builder()
                .productId(UUID.randomUUID())
                .name("Product 2")
                .description("Description 2")
                .price(149.99)
                .quantity(5)
                .build());
        
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<ProductResponseDto> result = productService.getAllProducts();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Product", result.get(0).getName());
        assertEquals("Product 2", result.get(1).getName());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getAllProducts_ShouldReturnEmptyList_WhenNoProductsExist() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<ProductResponseDto> result = productService.getAllProducts();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_ShouldReturnProductResponseDto_WhenProductExists() {
        // Given
        when(productRepository.findByProductId(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        ProductResponseDto result = productService.getProductById(testProductId);

        // Then
        assertNotNull(result);
        assertEquals(testProductId, result.getProductId());
        assertEquals("Test Product", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals(99.99, result.getPrice());
        assertEquals(10, result.getQuantity());
        verify(productRepository, times(1)).findByProductId(testProductId);
    }

    @Test
    void getProductById_ShouldThrowResourceNotFoundException_WhenProductDoesNotExist() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(productRepository.findByProductId(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.getProductById(nonExistentId)
        );
        
        assertTrue(exception.getMessage().contains("Product not found with the id: " + nonExistentId));
        verify(productRepository, times(1)).findByProductId(nonExistentId);
    }

    @Test
    void createProduct_ShouldReturnProductResponseDto_WhenProductIsValid() {
        // Given
        Product newProduct = Product.builder()
                .name("New Product")
                .description("New Description")
                .price(199.99)
                .quantity(15)
                .build();

        Product savedProduct = Product.builder()
                .productId(testProductId)
                .name("New Product")
                .description("New Description")
                .price(199.99)
                .quantity(15)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.save(newProduct)).thenReturn(savedProduct);

        // When
        ProductResponseDto result = productService.createProduct(newProduct);

        // Then
        assertNotNull(result);
        assertEquals(testProductId, result.getProductId());
        assertEquals("New Product", result.getName());
        assertEquals("New Description", result.getDescription());
        assertEquals(199.99, result.getPrice());
        assertEquals(15, result.getQuantity());
        verify(productRepository, times(1)).save(newProduct);
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProductResponseDto_WhenProductExists() {
        // Given
        Product updateRequest = Product.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(299.99)
                .quantity(20)
                .build();

        Product updatedProduct = Product.builder()
                .productId(testProductId)
                .name("Updated Product")
                .description("Updated Description")
                .price(299.99)
                .quantity(20)
                .createdAt(testProduct.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.findByProductId(testProductId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // When
        ProductResponseDto result = productService.updateProduct(testProductId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(testProductId, result.getProductId());
        assertEquals("Updated Product", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(299.99, result.getPrice());
        assertEquals(20, result.getQuantity());
        verify(productRepository, times(1)).findByProductId(testProductId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldThrowResourceNotFoundException_WhenProductDoesNotExist() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        Product updateRequest = Product.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(299.99)
                .quantity(20)
                .build();

        when(productRepository.findByProductId(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.updateProduct(nonExistentId, updateRequest)
        );
        
        assertTrue(exception.getMessage().contains("Product not found with the id: " + nonExistentId));
        verify(productRepository, times(1)).findByProductId(nonExistentId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldReturnTrue_WhenProductExists() {
        // Given
        when(productRepository.existsByProductId(testProductId)).thenReturn(true);
        doNothing().when(productRepository).deleteByProductId(testProductId);

        // When
        boolean result = productService.deleteProduct(testProductId);

        // Then
        assertTrue(result);
        verify(productRepository, times(1)).existsByProductId(testProductId);
        verify(productRepository, times(1)).deleteByProductId(testProductId);
    }

    @Test
    void deleteProduct_ShouldThrowResourceNotFoundException_WhenProductDoesNotExist() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(productRepository.existsByProductId(nonExistentId)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.deleteProduct(nonExistentId)
        );
        
        assertTrue(exception.getMessage().contains("Product not found with the id: " + nonExistentId));
        verify(productRepository, times(1)).existsByProductId(nonExistentId);
        verify(productRepository, never()).deleteByProductId(any(UUID.class));
    }
}
