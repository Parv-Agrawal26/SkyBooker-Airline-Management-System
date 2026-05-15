package com.skybooker.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.skybooker.notification.event.NotificationEvent;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendWelcomeEmail(NotificationEvent event) {
        Context ctx = new Context();
        ctx.setVariable("userName", event.getUserName());
        ctx.setVariable("email", event.getToEmail());
        send(event.getToEmail(), "Welcome to SkyBooker! ✈", "welcome", ctx);
    }

    public void sendBookingCreatedEmail(NotificationEvent event) {
        Context ctx = new Context();
        ctx.setVariable("bookingId",     event.getBookingId());
        ctx.setVariable("source",        event.getSource());
        ctx.setVariable("destination",   event.getDestination());
        ctx.setVariable("departureDate", event.getDepartureDate());
        ctx.setVariable("departureTime", event.getDepartureTime());
        ctx.setVariable("airline",       event.getAirline());
        send(event.getToEmail(), "Booking Created — #" + event.getBookingId() + " ✈", "booking-created", ctx);
    }

    public void sendBookingConfirmationEmail(NotificationEvent event) {
        Context ctx = new Context();
        ctx.setVariable("bookingId",     event.getBookingId());
        ctx.setVariable("amount",        event.getAmount());
        ctx.setVariable("transactionId", event.getTransactionId());
        ctx.setVariable("paymentMode",   event.getPaymentMode());
        ctx.setVariable("source",        event.getSource());
        ctx.setVariable("destination",   event.getDestination());
        ctx.setVariable("departureDate", event.getDepartureDate());
        ctx.setVariable("departureTime", event.getDepartureTime());
        ctx.setVariable("airline",       event.getAirline());
        send(event.getToEmail(), "Booking Confirmed — #" + event.getBookingId() + " ✈", "booking-confirmation", ctx);
    }

    public void sendRefundEmail(NotificationEvent event) {
        Context ctx = new Context();
        ctx.setVariable("bookingId",     event.getBookingId());
        ctx.setVariable("amount",        event.getAmount());
        ctx.setVariable("transactionId", event.getTransactionId());
        send(event.getToEmail(), "Refund Initiated — Booking #" + event.getBookingId(), "refund-confirmation", ctx);
    }

    public void sendPasswordResetOtpEmail(NotificationEvent event) {
        Context ctx = new Context();
        ctx.setVariable("userName", event.getUserName());
        ctx.setVariable("otp", event.getOtp());
        send(event.getToEmail(), "SkyBooker Password Reset Code", "password-reset-otp", ctx);
    }

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent — to: {}, subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to: {}, reason: {}", to, e.getMessage());
        }
    }
}
