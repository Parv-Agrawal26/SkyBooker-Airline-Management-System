package com.skybooker.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.skybooker.payment.config.RabbitMQConfig;
import com.skybooker.payment.event.NotificationEvent;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── Existing endpoint — kept for backward compatibility ───────────────────
    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Payment request — bookingId: {}, user: {}, amount: {}, mode: {}",
                request.getBookingId(), request.getUserEmail(),
                request.getAmount(), request.getPaymentMode());

        if (paymentRepository.findByBookingId(request.getBookingId()).isPresent()) {
            log.warn("Duplicate payment attempt — bookingId: {}", request.getBookingId());
            throw new RuntimeException("Payment already done for booking: " + request.getBookingId());
        }

        Payment payment = new Payment();
        payment.setBookingId(request.getBookingId());
        payment.setUserEmail(request.getUserEmail());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setPaymentMode(request.getPaymentMode());
        payment.setStatus("PAID");
        payment.setTransactionId(generateTransactionId(request.getPaymentMode()));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment saved — bookingId: {}, txnId: {}", saved.getBookingId(), saved.getTransactionId());
        return mapToResponse(saved, "Payment successful");
    }

    // ── Razorpay: Step 1 — create order ──────────────────────────────────────
    @Override
    public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request) {
        log.info("Creating Razorpay order — bookingId: {}, amount: {}", request.getBookingId(), request.getAmount());
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject options = new JSONObject();
            options.put("amount", (int)(request.getAmount() * 100)); // paise
            options.put("currency", "INR");
            options.put("receipt", "booking_" + request.getBookingId());
            options.put("payment_capture", 1);
            Order order = client.orders.create(options);
            log.info("Razorpay order created — orderId: {}", (String) order.get("id").toString());
            return new RazorpayOrderResponse(
                    order.get("id").toString(),
                    request.getAmount(),
                    "INR",
                    razorpayKeyId,
                    request.getBookingId(),
                    request.getUserEmail()
            );
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Could not create payment order: " + e.getMessage());
        }
    }

    // ── Razorpay: Step 2 — verify signature + save payment ───────────────────
    @Override
    public PaymentResponse verifyAndSavePayment(RazorpayVerifyRequest request) {
        log.info("Verifying Razorpay payment — orderId: {}, paymentId: {}",
                request.getRazorpayOrderId(), request.getRazorpayPaymentId());

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        if (!verifySignature(payload, request.getRazorpaySignature())) {
            log.error("Signature verification FAILED — orderId: {}", request.getRazorpayOrderId());
            throw new RuntimeException("Payment verification failed. Invalid signature.");
        }

        if (paymentRepository.findByBookingId(request.getBookingId()).isPresent()) {
            log.warn("Duplicate payment — bookingId: {}", request.getBookingId());
            throw new RuntimeException("Payment already recorded for booking: " + request.getBookingId());
        }

        Payment payment = new Payment();
        payment.setBookingId(request.getBookingId());
        payment.setUserEmail(request.getUserEmail());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setPaymentMode(mapRazorpayMethod(request.getPaymentMode()));
        payment.setStatus("PAID");
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Razorpay payment saved — bookingId: {}, paymentId: {}, amount: ₹{}",
                saved.getBookingId(), saved.getTransactionId(), saved.getAmount());

        publishPaymentConfirmed(saved);

        return mapToResponse(saved, "Payment successful");
    }

    @Override
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        return mapToResponse(
                paymentRepository.findByBookingId(bookingId)
                        .orElseThrow(() -> new RuntimeException("No payment found for booking: " + bookingId)),
                "Success");
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(String userEmail) {
        return paymentRepository.findByUserEmail(userEmail).stream()
                .map(p -> mapToResponse(p, "Success")).collect(Collectors.toList());
    }

    @Override
    public PaymentResponse processRefund(Long bookingId) {
        log.info("Refund request — bookingId: {}", bookingId);
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("No payment found for booking: " + bookingId));
        if (!"PAID".equals(payment.getStatus()))
            throw new RuntimeException("Refund not possible. Payment status is: " + payment.getStatus());
        payment.setStatus("REFUNDED");
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundedAt(LocalDateTime.now());
        Payment updated = paymentRepository.save(payment);
        log.info("Refund processed — bookingId: {}, amount: ₹{}", bookingId, payment.getAmount());

        publishRefundConfirmed(updated);

        return mapToResponse(updated, "Refund processed successfully. Amount will be credited in 5-7 working days.");
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status).stream()
                .map(p -> mapToResponse(p, "Success")).collect(Collectors.toList());
    }

    @Override
    public double getRevenueForBookings(List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return 0.0;
        return paymentRepository.findByBookingIdIn(bookingIds).stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void publishPaymentConfirmed(Payment p) {
        try {
            NotificationEvent e = new NotificationEvent();
            e.setType("PAYMENT_CONFIRMED");
            e.setToEmail(p.getUserEmail());
            e.setBookingId(p.getBookingId());
            e.setPaymentId(p.getId());
            e.setAmount(p.getAmount());
            e.setTransactionId(p.getTransactionId());
            e.setPaymentMode(p.getPaymentMode());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_CONFIRMED_KEY, e);
        } catch (Exception ex) {
            log.warn("Failed to publish PAYMENT_CONFIRMED event: {}", ex.getMessage());
        }
    }

    private void publishRefundConfirmed(Payment p) {
        try {
            NotificationEvent e = new NotificationEvent();
            e.setType("PAYMENT_REFUNDED");
            e.setToEmail(p.getUserEmail());
            e.setBookingId(p.getBookingId());
            e.setAmount(p.getRefundAmount());
            e.setTransactionId(p.getTransactionId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_REFUNDED_KEY, e);
        } catch (Exception ex) {
            log.warn("Failed to publish PAYMENT_REFUNDED event: {}", ex.getMessage());
        }
    }

    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String mapRazorpayMethod(String method) {
        if (method == null) return "CARD";
        return switch (method.toLowerCase()) {
            case "upi"        -> "UPI";
            case "netbanking" -> "NETBANKING";
            case "wallet"     -> "WALLET";
            default           -> "CARD";
        };
    }

    private String generateTransactionId(String mode) {
        String prefix = switch (mode.toUpperCase()) {
            case "UPI"        -> "UPI";
            case "CARD"       -> "CRD";
            case "NETBANKING" -> "NET";
            case "WALLET"     -> "WLT";
            default           -> "PAY";
        };
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentResponse mapToResponse(Payment payment, String message) {
        PaymentResponse res = new PaymentResponse();
        res.setPaymentId(payment.getId());
        res.setBookingId(payment.getBookingId());
        res.setUserEmail(payment.getUserEmail());
        res.setAmount(payment.getAmount());
        res.setCurrency(payment.getCurrency());
        res.setPaymentMode(payment.getPaymentMode());
        res.setStatus(payment.getStatus());
        res.setTransactionId(payment.getTransactionId());
        res.setRefundAmount(payment.getRefundAmount());
        res.setPaidAt(payment.getPaidAt());
        res.setRefundedAt(payment.getRefundedAt());
        res.setMessage(message);
        return res;
    }
}
