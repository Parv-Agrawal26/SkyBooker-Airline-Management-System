package com.skybooker.notification.listener;

import com.skybooker.notification.config.RabbitMQConfig;
import com.skybooker.notification.event.NotificationEvent;
import com.skybooker.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    public void onUserRegistered(NotificationEvent event) {
        log.info("Received USER_REGISTERED event for: {}", event.getToEmail());
        emailService.sendWelcomeEmail(event);
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CREATED_QUEUE)
    public void onBookingCreated(NotificationEvent event) {
        log.info("Received BOOKING_CREATED event — bookingId: {}", event.getBookingId());
        emailService.sendBookingCreatedEmail(event);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CONFIRMED_QUEUE)
    public void onPaymentConfirmed(NotificationEvent event) {
        log.info("Received PAYMENT_CONFIRMED event — bookingId: {}", event.getBookingId());
        emailService.sendBookingConfirmationEmail(event);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUNDED_QUEUE)
    public void onPaymentRefunded(NotificationEvent event) {
        log.info("Received PAYMENT_REFUNDED event — bookingId: {}", event.getBookingId());
        emailService.sendRefundEmail(event);
    }
}
