package com.himusharier.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequestDto {
    @NotBlank(message = "Product name can not be blank.")
    private String name;

    private String description;

    @NotNull(message = "Product price can not be blank.")
    private Double price;

    @NotNull(message = "Product quantity can not be blank.")
    private int quantity;
}
