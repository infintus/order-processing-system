package com.ecom.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;       // CREATED | CONFIRMED | FAILED | CANCELLED
    private String eventType;    // ORDER_PLACED | ORDER_CONFIRMED | ORDER_FAILED | ORDER_CANCELLED
    private LocalDateTime timestamp;
}
