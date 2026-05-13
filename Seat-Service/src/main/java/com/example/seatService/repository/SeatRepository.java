package com.example.seatService.repository;

import com.example.seatService.entity.Seat;
import com.example.seatService.entity.SeatClass;
import com.example.seatService.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlightId(Long flightId);

    List<Seat> findByFlightIdAndSeatClass(Long flightId, SeatClass seatClass);

    List<Seat> findByFlightIdAndStatus(Long flightId, SeatStatus status);

    Optional<Seat> findBySeatNumberAndFlightId(String seatNumber, Long flightId);

    long countByFlightIdAndSeatClassAndStatus(Long flightId, SeatClass seatClass, SeatStatus status);

    void deleteByFlightId(Long flightId);
    
    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);
}