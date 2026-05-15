package com.skybooker.payment.controller;

import com.skybooker.payment.dto.*;
import com.skybooker.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // existing — kept for backward compatibility
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    // Razorpay: step 1 — create order, returns orderId + keyId to frontend
    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@RequestBody RazorpayOrderRequest request) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(request));
    }

    // Razorpay: step 2 — verify signature + save payment as PAID
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verify(@RequestBody RazorpayVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyAndSavePayment(request));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<PaymentResponse>> getByUser(@PathVariable String email) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(email));
    }

    @PostMapping("/refund/{bookingId}")
    public ResponseEntity<PaymentResponse> refund(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.processRefund(bookingId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @PostMapping("/bookings/total")
    public ResponseEntity<Double> getRevenueForBookings(@RequestBody List<Long> bookingIds) {
        return ResponseEntity.ok(paymentService.getRevenueForBookings(bookingIds));
    }
}
