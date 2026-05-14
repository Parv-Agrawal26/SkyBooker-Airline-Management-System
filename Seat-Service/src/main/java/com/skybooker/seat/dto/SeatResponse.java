package com.skybooker.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {

    private Long id;
    private Long flightId;
    private String seatNumber;
    private String seatClass;
    private int row;
    private String column;
    @com.fasterxml.jackson.annotation.JsonProperty("isWindow")
    private boolean isWindow;
    @com.fasterxml.jackson.annotation.JsonProperty("isAisle")
    private boolean isAisle;
    private boolean hasExtraLegroom;
    private String status;
    private double priceMultiplier;
    private LocalDateTime holdExpiresAt;
    private String message;
}
