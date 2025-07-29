package com.himusharier.inventory.service;

import com.himusharier.inventory.dto.response.ProductResponseDto;
import com.himusharier.inventory.exception.ResourceNotFoundException;
import com.himusharier.inventory.model.Product;
import com.himusharier.inventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponseDto> getAllProducts() {
        List<Product> getAllProducts = productRepository.findAll();

        return getAllProducts.stream()
                .map(this::mapProductToRespondDto)
                .collect(Collectors.toList());
    }

    public ProductResponseDto getProductById(UUID id) {
        Product product = productRepository.findByProductId(id).orElseThrow(() ->
                new ResourceNotFoundException("Product not found with the id: " + id));

        return mapProductToRespondDto(product);
    }

    @Transactional
    public ProductResponseDto createProduct(Product product) {
        Product createProduct = productRepository.save(product);

        return mapProductToRespondDto(createProduct);
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID id, Product product) {
        Product existingProduct = productRepository.findByProductId(id).orElseThrow(() ->
                new ResourceNotFoundException("Product not found with the id: " + id));

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setQuantity(product.getQuantity());

        Product savedProduct = productRepository.save(existingProduct);
        return mapProductToRespondDto(savedProduct);
    }

    @Transactional
    public boolean deleteProduct(UUID id) {
        if (!productRepository.existsByProductId(id)) {
            throw new ResourceNotFoundException("Product not found with the id: " + id);
        }
        productRepository.deleteByProductId(id);
        return true;
    }

    private ProductResponseDto mapProductToRespondDto(Product product) {
        return ProductResponseDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .build();
    }

}
