//package com.skybooker.booking.service;
//
//import com.skybooker.booking.dto.BookingRequest;
//import com.skybooker.booking.dto.BookingResponse;
//import com.skybooker.booking.entity.Booking;
//import com.skybooker.booking.repository.BookingRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class BookingServiceImpl implements BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final RestTemplate restTemplate;
//
//    @Override
//    public BookingResponse bookFlight(BookingRequest request) {
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", request.getToken());
//        HttpEntity<Void> entity = new HttpEntity<>(headers);
//
//        // Step 1: Flight details fetch karo
//        String source = "", destination = "", depDate = "", depTime = "", airline = "";
//        try {
//            String flightUrl = "http://localhost:8082/flights/" + request.getFlightId();
//            ResponseEntity<Map> flightRes = restTemplate.exchange(
//                    flightUrl, HttpMethod.GET, entity, Map.class);
//            if (flightRes.getStatusCode().is2xxSuccessful() && flightRes.getBody() != null) {
//                Map body = flightRes.getBody();
//                source      = str(body.get("source"));
//                destination = str(body.get("destination"));
//                depDate     = str(body.get("departureDate"));
//                depTime     = str(body.get("departureTime"));
//                airline     = str(body.get("airline"));
//            }
//        } catch (Exception e) {
//            System.err.println("Could not fetch flight details: " + e.getMessage());
//        }
//
//        // Step 2: Reduce seats
//        String reduceUrl = "http://localhost:8082/flights/" + request.getFlightId()
//                + "/reduce-seats?seats=" + request.getSeats();
//        ResponseEntity<String> response = restTemplate.exchange(
//                reduceUrl, HttpMethod.PUT, entity, String.class);
//
//        if (!response.getStatusCode().is2xxSuccessful()) {
//            throw new RuntimeException("Seat reduction failed: " + response.getStatusCode());
//        }
//
//        // Step 3: Booking save karo with flight details
//        Booking booking = new Booking();
//        booking.setFlightId(request.getFlightId());
//        booking.setUserEmail(request.getUserEmail());
//        booking.setSeatsBooked(request.getSeats());
//        booking.setTotalPrice(0.0);
//        booking.setStatus("CONFIRMED");
//        booking.setBookingTime(LocalDateTime.now());
//        booking.setSource(source);
//        booking.setDestination(destination);
//        booking.setDepartureDate(depDate);
//        booking.setDepartureTime(depTime);
//        booking.setAirline(airline);
//
//        Booking saved = bookingRepository.save(booking);
//
//        return new BookingResponse(
//                saved.getId(), "Booking successful", true,
//                source, destination, depDate, depTime, airline
//        );
//    }
//
//    @Override
//    public BookingResponse getBookingById(Long bookingId) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
//        return new BookingResponse(
//                booking.getId(), "Success", true,
//                booking.getSource(), booking.getDestination(),
//                booking.getDepartureDate(), booking.getDepartureTime(),
//                booking.getAirline()
//        );
//    }
//
//    private String str(Object o) {
//        return o != null ? o.toString() : "";
//    }
//}

package com.skybooker.booking.service;

import com.skybooker.booking.config.RabbitMQConfig;
import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.event.NotificationEvent;
import com.skybooker.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @org.springframework.beans.factory.annotation.Value("${flight.service.url:http://localhost:8082}")
    private String flightServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${passenger.service.url:http://localhost:8084}")
    private String passengerServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${seat.service.url:http://localhost:8086}")
    private String seatServiceUrl;

    @Override
    public BookingResponse bookFlight(BookingRequest request) {
        log.info("Booking request — flightId: {}, user: {}, seats: {}",
                request.getFlightId(), request.getUserEmail(), request.getSeats());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", request.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String source = "", destination = "", depDate = "", depTime = "", airline = "";
        try {
            String flightUrl = flightServiceUrl + "/flights/" + request.getFlightId();
            ResponseEntity<Map> flightRes = restTemplate.exchange(
                    flightUrl, HttpMethod.GET, entity, Map.class);
            if (flightRes.getStatusCode().is2xxSuccessful() && flightRes.getBody() != null) {
                Map body = flightRes.getBody();
                source      = str(body.get("source"));
                destination = str(body.get("destination"));
                depDate     = str(body.get("departureDate"));
                depTime     = str(body.get("departureTime"));
                airline     = str(body.get("airline"));
                log.info("Flight details fetched — {} → {}, date: {}", source, destination, depDate);
            }
        } catch (Exception e) {
            log.error("Could not fetch flight details for flightId: {} — {}", request.getFlightId(), e.getMessage());
        }

        String reduceUrl = flightServiceUrl + "/flights/" + request.getFlightId()
                + "/reduce-seats?seats=" + request.getSeats();
        ResponseEntity<String> response = restTemplate.exchange(
                reduceUrl, HttpMethod.PUT, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Seat reduction failed — flightId: {}, status: {}",
                    request.getFlightId(), response.getStatusCode());
            throw new RuntimeException("Seat reduction failed: " + response.getStatusCode());
        }

        Booking booking = new Booking();
        booking.setFlightId(request.getFlightId());
        booking.setUserEmail(request.getUserEmail());
        booking.setSeatsBooked(request.getSeats());
        booking.setTotalPrice(0.0);
        booking.setStatus("CONFIRMED");
        booking.setBookingTime(LocalDateTime.now());
        booking.setSource(source);
        booking.setDestination(destination);
        booking.setDepartureDate(depDate);
        booking.setDepartureTime(depTime);
        booking.setAirline(airline);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking saved — bookingId: {}, user: {}, route: {} → {}",
                saved.getId(), request.getUserEmail(), source, destination);

        try {
            NotificationEvent event = new NotificationEvent();
            event.setType("BOOKING_CREATED");
            event.setToEmail(request.getUserEmail());
            event.setBookingId(saved.getId());
            event.setSource(source);
            event.setDestination(destination);
            event.setDepartureDate(depDate);
            event.setDepartureTime(depTime);
            event.setAirline(airline);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.BOOKING_CREATED_KEY, event);
        } catch (Exception e) {
            log.warn("Failed to publish BOOKING_CREATED event: {}", e.getMessage());
        }

        return new BookingResponse(saved.getId(), "Booking successful", true,
                saved.getFlightId(), source, destination, depDate, depTime, airline);
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        log.debug("Fetching booking — bookingId: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found — bookingId: {}", bookingId);
                    return new RuntimeException("Booking not found: " + bookingId);
                });
        return new BookingResponse(booking.getId(), "Success", true,
                booking.getFlightId(),
                booking.getSource(), booking.getDestination(),
                booking.getDepartureDate(), booking.getDepartureTime(),
                booking.getAirline());
    }

    @Override
    public void cancelBooking(Long bookingId) {
        log.info("Cancelling booking — bookingId: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Step 1: Fetch passengers and release their seats in seat-service
        int seatsToRestore = 0;
        try {
            String passengersUrl = passengerServiceUrl + "/passengers/booking/" + bookingId;
            org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map>> type =
                    new org.springframework.core.ParameterizedTypeReference<>() {};
            var passengers = restTemplate.exchange(
                    passengersUrl, HttpMethod.GET, HttpEntity.EMPTY, type).getBody();
            if (passengers != null && booking.getFlightId() != null) {
                for (java.util.Map p : passengers) {
                    Object seatNum = p.get("seatNumber");
                    if (seatNum != null) {
                        try {
                            String releaseUrl = seatServiceUrl + "/seats/flight/"
                                    + booking.getFlightId() + "/release/" + seatNum;
                            restTemplate.put(releaseUrl, HttpEntity.EMPTY);
                            seatsToRestore++;
                            log.info("Seat released on cancel — seat: {}, flightId: {}", seatNum, booking.getFlightId());
                        } catch (Exception se) {
                            log.warn("Could not release seat {} on cancel: {}", seatNum, se.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch passengers for seat release on cancel: {}", e.getMessage());
        }

        // Step 2: Restore availableSeats in flight-service
        if (seatsToRestore > 0 && booking.getFlightId() != null) {
            try {
                String restoreUrl = flightServiceUrl + "/flights/" + booking.getFlightId()
                        + "/restore-seats?seats=" + seatsToRestore;
                restTemplate.put(restoreUrl, HttpEntity.EMPTY);
                log.info("Seats restored in flight-service — flightId: {}, count: {}", booking.getFlightId(), seatsToRestore);
            } catch (Exception e) {
                log.warn("Could not restore seats in flight-service: {}", e.getMessage());
            }
        }

        // Step 3: Delete all passengers for this booking
        try {
            String deletePassengersUrl = passengerServiceUrl + "/passengers/booking/" + bookingId;
            restTemplate.delete(deletePassengersUrl);
            log.info("Passengers deleted for bookingId: {}", bookingId);
        } catch (Exception e) {
            log.warn("Could not delete passengers for bookingId {}: {}", bookingId, e.getMessage());
        }

        // Step 4: Delete the booking record
        bookingRepository.delete(booking);
        log.info("Booking deleted — bookingId: {}", bookingId);
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
}