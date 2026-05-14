package com.skybooker.payment.dto;

import lombok.Data;

@Data
public class RazorpayVerifyRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private Long bookingId;
    private String userEmail;
    private double amount;
    private String paymentMode;  // card/upi/netbanking/wallet — from Razorpay response
}
