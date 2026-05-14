package com.skybooker.passenger.controller;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    public ResponseEntity<PassengerResponse> addPassenger(@RequestBody PassengerRequest request) {
        return ResponseEntity.ok(passengerService.addPassenger(request));
    }

    @GetMapping("/{passengerId}")
    public ResponseEntity<PassengerResponse> getById(@PathVariable Long passengerId) {
        return ResponseEntity.ok(passengerService.getPassengerById(passengerId));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PassengerResponse>> getByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(passengerService.getPassengersByBooking(bookingId));
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<PassengerResponse>> getByFlight(@PathVariable Long flightId) {
        return ResponseEntity.ok(passengerService.getPassengersByFlight(flightId));
    }

    @GetMapping("/passport/{passportNumber}")
    public ResponseEntity<PassengerResponse> getByPassport(@PathVariable String passportNumber) {
        return ResponseEntity.ok(passengerService.getByPassportNumber(passportNumber));
    }

    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<PassengerResponse> getByTicket(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(passengerService.getByTicketNumber(ticketNumber));
    }

    @PutMapping("/{passengerId}")
    public ResponseEntity<PassengerResponse> updatePassenger(
            @PathVariable Long passengerId,
            @RequestBody PassengerRequest request) {
        return ResponseEntity.ok(passengerService.updatePassenger(passengerId, request));
    }

    @DeleteMapping("/{passengerId}")
    public ResponseEntity<String> deletePassenger(@PathVariable Long passengerId) {
        passengerService.deletePassenger(passengerId);
        return ResponseEntity.ok("Passenger deleted successfully");
    }

    @DeleteMapping("/booking/{bookingId}")
    public ResponseEntity<String> deleteByBooking(@PathVariable String bookingId) {
        passengerService.deleteByBookingId(bookingId);
        return ResponseEntity.ok("All passengers for booking " + bookingId + " deleted");
    }

    @GetMapping("/count/{bookingId}")
    public ResponseEntity<Integer> getCount(@PathVariable String bookingId) {
        return ResponseEntity.ok(passengerService.getPassengerCount(bookingId));
    }

    @PostMapping("/admin/backfill-flight-ids")
    public ResponseEntity<String> backfillFlightIds() {
        int count = passengerService.backfillFlightIds();
        return ResponseEntity.ok("Backfill complete. Updated " + count + " passengers.");
    }
}
