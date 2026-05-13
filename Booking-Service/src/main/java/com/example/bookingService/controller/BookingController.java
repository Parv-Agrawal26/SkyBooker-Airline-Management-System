package com.example.bookingService.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.example.bookingService.dto.BookingRequestDTO;
import com.example.bookingService.dto.BookingResponseDTO;
import com.example.bookingService.dto.FareSummaryDTO;
import com.example.bookingService.dto.PaymentResponseDTO;
import com.example.bookingService.service.BookingService;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // CREATE BOOKING
    @PostMapping
    public BookingResponseDTO createBooking(
            @RequestBody BookingRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if ((request.getUserId() == null || request.getUserId() <= 0) && userId != null && !userId.isBlank()) {
            request.setUserId(Long.valueOf(userId));
        }
        return bookingService.createBooking(request);
    }

    // GET BY ID
    @GetMapping("/{bookingId}")
    public BookingResponseDTO getById(@PathVariable UUID bookingId) {
        return bookingService.getBookingById(bookingId);
    }

    // GET BY PNR
    @GetMapping("/pnr/{pnr}")
    public BookingResponseDTO getByPnr(@PathVariable String pnr) {
        return bookingService.getBookingByPnr(pnr);
    }

    // GET ALL
    @GetMapping("/all")
    public List<BookingResponseDTO> getAll() {
        return bookingService.getAllBookings();
    }

    // GET BY USER
    @GetMapping("/user/{userId}")
    public List<BookingResponseDTO> getByUser(@PathVariable Long userId) {
        return bookingService.getBookingsByUser(userId);
    }

    // GET BY FLIGHT
    @GetMapping("/flight/{flightId}")
    public List<BookingResponseDTO> getByFlight(@PathVariable Long flightId) {
        return bookingService.getBookingsByFlight(flightId);
    }

    // CANCEL
    @PutMapping("/{bookingId}/cancel")
    public BookingResponseDTO cancelBooking(@PathVariable UUID bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    // REFUND REQUEST
    @PutMapping("/{bookingId}/refund/request")
    public BookingResponseDTO requestRefund(@PathVariable UUID bookingId) {
        return bookingService.requestRefund(bookingId);
    }

    @PostMapping("/{bookingId}/refund/request")
    public BookingResponseDTO requestRefundPost(@PathVariable UUID bookingId) {
        return bookingService.requestRefund(bookingId);
    }

    @PutMapping("/refund/request/{bookingId}")
    public BookingResponseDTO requestRefundAlt(@PathVariable UUID bookingId) {
        return bookingService.requestRefund(bookingId);
    }

    // REFUND APPROVE (admin)
    @PutMapping("/{bookingId}/refund/approve")
    public BookingResponseDTO approveRefund(@PathVariable UUID bookingId) {
        return bookingService.approveRefund(bookingId);
    }

    @PostMapping("/{bookingId}/refund/approve")
    public BookingResponseDTO approveRefundPost(@PathVariable UUID bookingId) {
        return bookingService.approveRefund(bookingId);
    }

    @PutMapping("/refund/approve/{bookingId}")
    public BookingResponseDTO approveRefundAlt(@PathVariable UUID bookingId) {
        return bookingService.approveRefund(bookingId);
    }

    // REFUND REJECT (admin)
    @PutMapping("/{bookingId}/refund/reject")
    public BookingResponseDTO rejectRefund(@PathVariable UUID bookingId) {
        return bookingService.rejectRefund(bookingId);
    }

    @PostMapping("/{bookingId}/refund/reject")
    public BookingResponseDTO rejectRefundPost(@PathVariable UUID bookingId) {
        return bookingService.rejectRefund(bookingId);
    }

    @PutMapping("/refund/reject/{bookingId}")
    public BookingResponseDTO rejectRefundAlt(@PathVariable UUID bookingId) {
        return bookingService.rejectRefund(bookingId);
    }

    // REFUND LIST
    @GetMapping("/refund/requests")
    public List<BookingResponseDTO> getRefundRequests() {
        return bookingService.getRefundRequests();
    }

    // UPDATE STATUS
    @PutMapping("/{bookingId}/status")
    public BookingResponseDTO updateStatus(@PathVariable UUID bookingId,
                                           @RequestParam String status) {
        return bookingService.updateBookingStatus(bookingId, status);
    }

    // DELETE
    @DeleteMapping("/{bookingId}")
    public String deleteBooking(@PathVariable UUID bookingId) {
        bookingService.deleteBooking(bookingId);
        return "Booking deleted successfully";
    }

    // FARE
    @GetMapping("/fare")
    public FareSummaryDTO calculateFare(@RequestParam Long flightId,
                                        @RequestParam(required = false) Integer luggageKg,
                                        @RequestParam(required = false, defaultValue = "ONE_WAY") String tripType,
                                        @RequestParam(required = false, defaultValue = "NO_MEAL") String mealPreference,
                                        @RequestParam(required = false, defaultValue = "NORMAL") String seatClass) {
        return bookingService.calculateFare(flightId, luggageKg, tripType, mealPreference, seatClass);
    }

    // ADD ONS
    @PutMapping("/{bookingId}/addon")
    public BookingResponseDTO addAddOn(@PathVariable UUID bookingId,
                                       @RequestParam String meal,
                                       @RequestParam Integer luggageKg) {
        return bookingService.addAddOn(bookingId, meal, luggageKg);
    }

    // UPCOMING
    @GetMapping("/upcoming/{userId}")
    public List<BookingResponseDTO> getUpcoming(@PathVariable Long userId) {
        return bookingService.getUpcomingBookings(userId);
    }

    // START PAYMENT
    @PostMapping("/{bookingId}/pay")
    public PaymentResponseDTO pay(@PathVariable UUID bookingId,
                                  @RequestParam String method) {
        return bookingService.startPayment(bookingId, method);
    }

    // PAYMENT COMPLETE (IMPORTANT: uses paymentId)
    @PostMapping("/payments/complete")
    public BookingResponseDTO completePayment(@RequestParam UUID paymentId,
                                              @RequestParam String transactionId,
                                              @RequestParam String status) {
        return bookingService.completePayment(paymentId, transactionId, status);
    }

    // Optional debug endpoint
    @GetMapping("/test")
    public String test(@RequestHeader("X-User") String user,
                       @RequestHeader("X-User-Id") String userId,
                       @RequestHeader("X-Role") String role) {
        return user + " | " + userId + " | " + role;
    }
}
