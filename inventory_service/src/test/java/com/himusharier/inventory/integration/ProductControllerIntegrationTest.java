package com.himusharier.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.himusharier.inventory.dto.request.ProductRequestDto;
import com.himusharier.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private UUID testProductId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        productRepository.deleteAll();
        
        // Create a test product
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setName("Test Product");
        requestDto.setDescription("Test Description");
        requestDto.setPrice(99.99);
        requestDto.setQuantity(10);

        String response = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the product ID from response
        testProductId = UUID.fromString(
                objectMapper.readTree(response)
                        .get("data")
                        .get("productId")
                        .asText()
        );
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Products retrieved successfully.")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Test Product")))
                .andExpect(jsonPath("$.data[0].description", is("Test Description")))
                .andExpect(jsonPath("$.data[0].price", is(99.99)))
                .andExpect(jsonPath("$.data[0].quantity", is(10)));
    }

    @Test
    void getAllProducts_ShouldReturnEmptyList_WhenNoProducts() throws Exception {
        productRepository.deleteAll();

        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Products retrieved successfully.")))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenProductExists() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", testProductId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Product retrieved successfully.")))
                .andExpect(jsonPath("$.data.name", is("Test Product")))
                .andExpect(jsonPath("$.data.description", is("Test Description")))
                .andExpect(jsonPath("$.data.price", is(99.99)))
                .andExpect(jsonPath("$.data.quantity", is(10)));
    }

    @Test
    void getProductById_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/products/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Product not found with the id: " + nonExistentId)));
    }

    @Test
    void createProduct_ShouldCreateAndReturnProduct_WhenValidRequest() throws Exception {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setName("New Product");
        requestDto.setDescription("New Description");
        requestDto.setPrice(199.99);
        requestDto.setQuantity(15);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Product saved successfully.")))
                .andExpect(jsonPath("$.data.name", is("New Product")))
                .andExpect(jsonPath("$.data.description", is("New Description")))
                .andExpect(jsonPath("$.data.price", is(199.99)))
                .andExpect(jsonPath("$.data.quantity", is(15)))
                .andExpect(jsonPath("$.data.productId", notNullValue()));
    }

    @Test
    void createProduct_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        ProductRequestDto requestDto = new ProductRequestDto();
        // Missing required fields

        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void updateProduct_ShouldUpdateAndReturnProduct_WhenValidRequest() throws Exception {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setName("Updated Product");
        requestDto.setDescription("Updated Description");
        requestDto.setPrice(299.99);
        requestDto.setQuantity(25);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put("/api/v1/products/{id}", testProductId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Product updated successfully.")))
                .andExpect(jsonPath("$.data.name", is("Updated Product")))
                .andExpect(jsonPath("$.data.description", is("Updated Description")))
                .andExpect(jsonPath("$.data.price", is(299.99)))
                .andExpect(jsonPath("$.data.quantity", is(25)));
    }

    @Test
    void updateProduct_ShouldReturnBadRequest_WhenProductDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setName("Updated Product");
        requestDto.setDescription("Updated Description");
        requestDto.setPrice(299.99);
        requestDto.setQuantity(25);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(put("/api/v1/products/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void deleteProduct_ShouldDeleteProduct_WhenProductExists() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", testProductId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Product deleted successfully.")));

        mockMvc.perform(get("/api/v1/products/{id}", testProductId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Product not found with the id: " + nonExistentId)));
    }
}
