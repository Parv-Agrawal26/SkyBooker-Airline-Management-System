package com.skybooker.payment.dto;

import lombok.Data;

@Data
public class RazorpayOrderRequest {
    private Long bookingId;
    private String userEmail;
    private double amount;  // in INR (will be converted to paise)
}
