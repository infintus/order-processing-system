package com.ecom.service;

import com.ecom.dto.OrderDTO;
import com.ecom.entity.Order;
import com.ecom.event.OrderEvent;
import com.ecom.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Transactional
    public OrderDTO.OrderResponse placeOrder(OrderDTO.CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status(Order.OrderStatus.CREATED)
                .build();

        order = orderRepository.save(order);
        log.info("Order created: {}", order.getId());

        OrderEvent event = buildEvent(order, "ORDER_PLACED");
        kafkaTemplate.send(ORDER_TOPIC, order.getId().toString(), event);
        log.info("OrderEvent published to Kafka: orderId={}", order.getId());

        return toResponse(order);
    }

    @Transactional
    public OrderDTO.OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel a confirmed order");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        OrderEvent event = buildEvent(order, "ORDER_CANCELLED");
        kafkaTemplate.send(ORDER_TOPIC, order.getId().toString(), event);
        log.info("Order cancelled and event published: {}", orderId);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderDTO.OrderResponse getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDTO.OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderEvent buildEvent(Order order, String eventType) {
        return OrderEvent.builder()
                .orderId(order.getId().toString())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private OrderDTO.OrderResponse toResponse(Order order) {
        OrderDTO.OrderResponse response = new OrderDTO.OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setProductId(order.getProductId());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
