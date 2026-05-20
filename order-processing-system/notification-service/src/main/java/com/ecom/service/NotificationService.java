package com.ecom.service;

import com.ecom.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    // In production: send emails/SMS via SES, Twilio, etc.
    // Here we log + store in memory for the /notifications endpoint to surface
    private final List<String> notificationLog = Collections.synchronizedList(new ArrayList<>());

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void handleOrderEvent(OrderEvent event) {
        String message = buildMessage(event);
        notificationLog.add(message);
        log.info("Notification sent: {}", message);
    }

    private String buildMessage(OrderEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" ->
                    String.format("[ORDER_PLACED] Hi customer %s! Your order %s for product %s (qty: %d) has been received.",
                            event.getCustomerId(), event.getOrderId(), event.getProductId(), event.getQuantity());
            case "ORDER_CONFIRMED" ->
                    String.format("[ORDER_CONFIRMED] Order %s confirmed! Your items are being prepared.",
                            event.getOrderId());
            case "ORDER_FAILED" ->
                    String.format("[ORDER_FAILED] Sorry, order %s could not be processed. You will not be charged.",
                            event.getOrderId());
            case "ORDER_CANCELLED" ->
                    String.format("[ORDER_CANCELLED] Order %s has been cancelled as requested.",
                            event.getOrderId());
            default ->
                    String.format("[UNKNOWN] Received event type=%s for order=%s", event.getEventType(), event.getOrderId());
        };
    }

    public List<String> getNotificationLog() {
        return List.copyOf(notificationLog);
    }
}
