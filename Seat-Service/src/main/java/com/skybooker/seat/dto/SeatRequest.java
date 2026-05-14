package com.skybooker.seat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatRequest {

    private Long flightId;
    private String seatNumber;
    private String seatClass;
    private int row;
    private String column;
    @JsonProperty("window")
    private boolean isWindow;
    @JsonProperty("aisle")
    private boolean isAisle;
    private boolean hasExtraLegroom;
    private double priceMultiplier;
}
