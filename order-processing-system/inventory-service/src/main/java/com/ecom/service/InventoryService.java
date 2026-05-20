package com.ecom.service;

import com.ecom.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InventoryService {

    // In-memory stock: productId -> available quantity
    // In production this would be a DB / Redis
    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
            "PROD-001", 100,
            "PROD-002", 50,
            "PROD-003", 25
    ));

    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Inventory received event: type={}, orderId={}", event.getEventType(), event.getOrderId());

        if ("ORDER_PLACED".equals(event.getEventType())) {
            processReservation(event);
        } else if ("ORDER_CANCELLED".equals(event.getEventType())) {
            releaseStock(event);
        }
    }

    private void processReservation(OrderEvent event) {
        String productId = event.getProductId();
        int requested = event.getQuantity();

        stock.compute(productId, (key, available) -> {
            if (available == null || available < requested) {
                log.warn("Insufficient stock for product={}, requested={}, available={}",
                        productId, requested, available);
                return available; // stock unchanged; order-service would need a compensation event in full saga
            }
            int remaining = available - requested;
            log.info("Stock reserved: product={}, reserved={}, remaining={}", productId, requested, remaining);
            return remaining;
        });
    }

    private void releaseStock(OrderEvent event) {
        stock.merge(event.getProductId(), event.getQuantity(), Integer::sum);
        log.info("Stock released for product={}, qty={}", event.getProductId(), event.getQuantity());
    }

    public Map<String, Integer> getStock() {
        return Map.copyOf(stock);
    }
}
