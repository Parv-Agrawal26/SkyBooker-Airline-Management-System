package com.skybooker.payment.service;

import com.skybooker.payment.dto.*;

import java.util.List;

public interface PaymentService {

    // existing — kept for backward compatibility
    PaymentResponse initiatePayment(PaymentRequest request);

    PaymentResponse getPaymentByBooking(Long bookingId);

    List<PaymentResponse> getPaymentsByUser(String userEmail);

    PaymentResponse processRefund(Long bookingId);

    List<PaymentResponse> getPaymentsByStatus(String status);

    // Revenue for a list of booking IDs (for staff flight revenue display)
    double getRevenueForBookings(List<Long> bookingIds);

    // Razorpay — step 1: create order
    RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request);

    // Razorpay — step 2: verify signature + save payment
    PaymentResponse verifyAndSavePayment(RazorpayVerifyRequest request);
}
