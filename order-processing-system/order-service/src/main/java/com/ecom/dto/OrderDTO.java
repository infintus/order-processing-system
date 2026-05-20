package com.ecom.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderDTO {

    @Data
    public static class CreateOrderRequest {
        @NotBlank(message = "Customer ID is required")
        private String customerId;

        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotNull @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull @DecimalMin(value = "0.01", message = "Total amount must be positive")
        private BigDecimal totalAmount;
    }

    @Data
    public static class OrderResponse {
        private UUID id;
        private String customerId;
        private String productId;
        private Integer quantity;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
