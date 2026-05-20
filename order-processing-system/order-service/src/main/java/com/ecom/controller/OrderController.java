package com.ecom.controller;

import com.ecom.dto.OrderDTO;
import com.ecom.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO.OrderResponse> placeOrder(
            @Valid @RequestBody OrderDTO.CreateOrderRequest request) {
        log.info("Received order request for customer: {}", request.getCustomerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO.OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDTO.OrderResponse>> getOrdersByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDTO.OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running");
    }
}
