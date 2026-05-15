package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;
    private final RestTemplate restTemplate;

    @Value("${booking.service.url:http://localhost:8083}")
    private String bookingServiceUrl;

    @Override
    public PassengerResponse addPassenger(PassengerRequest request) {
        log.info("Adding passenger — bookingId: {}, name: {} {}", request.getBookingId(), request.getFirstName(), request.getLastName());
        validatePassengerData(request);

        PassengerInfo passenger = new PassengerInfo();
        passenger.setBookingId(request.getBookingId());
        passenger.setTitle(request.getTitle());
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setGender(request.getGender());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setNationality(request.getNationality());
        passenger.setPassportExpiry(request.getPassportExpiry());
        passenger.setPassengerType(request.getPassengerType());
        passenger.setAddons(request.getAddons());
        passenger.setFlightId(request.getFlightId());
        if (request.getSeatId() != null) {
            boolean seatTaken = passengerRepository.existsBySeatIdAndBookingIdNot(
                    request.getSeatId(), request.getBookingId());
            if (seatTaken)
                throw new RuntimeException("Seat " + request.getSeatNumber() + " is already booked by another passenger.");
        }
        passenger.setSeatId(request.getSeatId());
        passenger.setSeatNumber(request.getSeatNumber());
        passenger.setTicketNumber(generateTicketNumber());
        passenger.setCreatedAt(LocalDateTime.now());

        PassengerInfo saved = passengerRepository.save(passenger);
        log.info("Passenger added — ticketNumber: {}, bookingId: {}", saved.getTicketNumber(), saved.getBookingId());

        return mapToResponse(saved, "Passenger added successfully");
    }

    @Override
    public PassengerResponse getPassengerById(Long passengerId) {
        PassengerInfo p = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));
        return mapToResponse(p, "Success");
    }

    @Override
    public List<PassengerResponse> getPassengersByBooking(String bookingId) {
        return passengerRepository.findByBookingId(bookingId).stream()
                .map(p -> mapToResponse(p, "Success")).collect(Collectors.toList());
    }

    @Override
    public List<PassengerResponse> getPassengersByFlight(Long flightId) {
        log.info("Fetching passengers for flightId: {}", flightId);
        return passengerRepository.findByFlightId(flightId).stream()
                .map(p -> mapToResponse(p, "Success")).collect(Collectors.toList());
    }

    @Override
    public PassengerResponse getByPassportNumber(String passportNumber) {
        PassengerInfo p = passengerRepository.findByPassportNumber(passportNumber)
                .orElseThrow(() -> new RuntimeException("Passenger not found with passport: " + passportNumber));
        return mapToResponse(p, "Success");
    }

    @Override
    public PassengerResponse getByTicketNumber(String ticketNumber) {
        PassengerInfo p = passengerRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));
        return mapToResponse(p, "Success");
    }

    @Override
    public PassengerResponse updatePassenger(Long passengerId, PassengerRequest request) {
        log.info("Updating passenger — id: {}", passengerId);
        PassengerInfo passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with id: " + passengerId));
        passenger.setTitle(request.getTitle());
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setGender(request.getGender());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setNationality(request.getNationality());
        passenger.setPassportExpiry(request.getPassportExpiry());
        passenger.setPassengerType(request.getPassengerType());
        PassengerInfo updated = passengerRepository.save(passenger);
        return mapToResponse(updated, "Passenger updated successfully");
    }


    public void deletePassenger(Long passengerId) {
        if (!passengerRepository.existsById(passengerId))
            throw new RuntimeException("Passenger not found with id: " + passengerId);
        passengerRepository.deleteById(passengerId);
        log.info("Passenger deleted — id: {}", passengerId);
    }

    @Override
    public void deleteByBookingId(String bookingId) {
        passengerRepository.deleteByBookingId(bookingId);
        log.info("All passengers deleted for bookingId: {}", bookingId);
    }

    @Override
    public int getPassengerCount(String bookingId) {
        return passengerRepository.countByBookingId(bookingId);
    }

    @Override
    public int backfillFlightIds() {
        List<PassengerInfo> nullFlight = passengerRepository.findByFlightIdIsNull();
        log.info("Backfill: {} passengers with null flightId", nullFlight.size());
        int updated = 0;
        for (PassengerInfo p : nullFlight) {
            try {
                String url = bookingServiceUrl + "/bookings/" + p.getBookingId();
                Map booking = restTemplate.getForObject(url, Map.class);
                if (booking != null && booking.get("flightId") != null) {
                    p.setFlightId(((Number) booking.get("flightId")).longValue());
                    passengerRepository.save(p);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("Backfill failed for passengerId: {}, bookingId: {} — {}",
                        p.getPassengerId(), p.getBookingId(), e.getMessage());
            }
        }
        log.info("Backfill complete: {}/{} passengers updated", updated, nullFlight.size());
        return updated;
    }

    private String generateTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validatePassengerData(PassengerRequest request) {
        if (request.getPassportExpiry() != null && request.getPassportExpiry().isBefore(LocalDate.now()))
            throw new RuntimeException("Passport is expired for passenger: " + request.getFirstName() + " " + request.getLastName());
        if ("INFANT".equalsIgnoreCase(request.getPassengerType()) && request.getDateOfBirth() != null
                && request.getDateOfBirth().isBefore(LocalDate.now().minusYears(2)))
            throw new RuntimeException("Infant must be under 2 years of age");
        if ("CHILD".equalsIgnoreCase(request.getPassengerType()) && request.getDateOfBirth() != null
                && request.getDateOfBirth().isBefore(LocalDate.now().minusYears(12)))
            throw new RuntimeException("Child passenger must be under 12 years of age");
    }

    private PassengerResponse mapToResponse(PassengerInfo p, String message) {
        PassengerResponse res = new PassengerResponse();
        res.setPassengerId(p.getPassengerId());
        res.setBookingId(p.getBookingId());
        res.setTitle(p.getTitle());
        res.setFirstName(p.getFirstName());
        res.setLastName(p.getLastName());
        res.setDateOfBirth(p.getDateOfBirth());
        res.setGender(p.getGender());
        res.setPassportNumber(p.getPassportNumber());
        res.setNationality(p.getNationality());
        res.setPassportExpiry(p.getPassportExpiry());
        res.setSeatNumber(p.getSeatNumber());
        res.setSeatId(p.getSeatId());
        res.setTicketNumber(p.getTicketNumber());
        res.setPassengerType(p.getPassengerType());
        res.setAddons(p.getAddons());
        res.setMessage(message);
        return res;
    }
}
