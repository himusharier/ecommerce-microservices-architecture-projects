package com.himusharier.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ProductResponseDto {
    private UUID productId;
    private String name;
    private String description;
    private Double price;
    private int quantity;
}
