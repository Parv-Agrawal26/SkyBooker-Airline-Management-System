package com.skybooker.flight.controller;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    public FlightResponse addFlight(@RequestBody FlightRequest request) {
        return flightService.addFlight(request);
    }

    @GetMapping
    public List<FlightResponse> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{id}")
    public FlightResponse getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    @GetMapping("/search")
    public List<FlightResponse> searchFlights(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false) String date) {
        if (date == null || date.isBlank())
            return java.util.Collections.emptyList();
        try {
            return flightService.searchFlights(source, destination, LocalDate.parse(date));
        } catch (java.time.format.DateTimeParseException e) {
            return java.util.Collections.emptyList();
        }
    }

    @PutMapping("/{id}/reduce-seats")
    public String reduceSeats(@PathVariable Long id, @RequestParam int seats) {
        return flightService.reduceSeats(id, seats);
    }

    @PutMapping("/{id}/restore-seats")
    public String restoreSeats(@PathVariable Long id, @RequestParam int seats) {
        return flightService.restoreSeats(id, seats);
    }

    @PutMapping("/{id}/status")
    public FlightResponse updateStatus(@PathVariable Long id, @RequestParam String status) {
        return flightService.updateStatus(id, status);
    }
}