package com.skybooker.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String razorpayOrderId;  // order_XXXXXXXXXX
    private double amount;           // in INR
    private String currency;
    private String keyId;            // sent to frontend to open checkout
    private Long bookingId;
    private String userEmail;
}
