package com.skybooker.booking.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent implements Serializable {
    private String type;
    private String toEmail;
    private String userName;
    private Long   bookingId;
    private Long   paymentId;
    private Double amount;
    private String transactionId;
    private String paymentMode;
    private String source;
    private String destination;
    private String departureDate;
    private String departureTime;
    private String airline;
    private String passengerName;
    private String seatNumber;
    private String ticketNumber;
}
