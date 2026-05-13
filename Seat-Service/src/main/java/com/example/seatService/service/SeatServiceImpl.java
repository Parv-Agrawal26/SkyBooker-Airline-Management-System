package com.example.seatService.service;

import com.example.seatService.dto.SeatRequestDTO;
import com.example.seatService.dto.SeatResponseDTO;
import com.example.seatService.entity.Seat;
import com.example.seatService.entity.SeatClass;
import com.example.seatService.entity.SeatStatus;
import com.example.seatService.repository.SeatRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    public SeatServiceImpl(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    // ADD SEATS
    @Override
    public List<SeatResponseDTO> addSeats(List<SeatRequestDTO> seatDTOs) {

        List<Seat> seats = new ArrayList<>();

        for (SeatRequestDTO dto : seatDTOs) {

            Seat seat = new Seat();
            seat.setFlightId(dto.getFlightId());
            seat.setSeatNumber(dto.getSeatNumber());
            seat.setSeatClass(dto.getSeatClass());
            seat.setRowNumber(dto.getRowNumber());
            seat.setColumnLetter(dto.getColumnLetter());

            seat.setWindow(dto.isWindow());
            seat.setAisle(dto.isAisle());
            seat.setHasExtraLegroom(dto.isHasExtraLegroom());

            seat.setPriceMultiplier(dto.getPriceMultiplier());
            seat.setStatus(SeatStatus.AVAILABLE);

            seats.add(seat);
        }

        List<Seat> saved = seatRepository.saveAll(seats);

        return mapToDTOList(saved);
    }

    // GET BY FLIGHT
    @Override
    public List<SeatResponseDTO> getSeatsByFlight(Long flightId) {
        return mapToDTOList(seatRepository.findByFlightId(flightId));
    }

    // AVAILABLE SEATS
    @Override
    public List<SeatResponseDTO> getAvailableSeats(Long flightId) {
        return mapToDTOList(seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE));
    }

    // BY CLASS
    @Override
    public List<SeatResponseDTO> getByClass(Long flightId, SeatClass seatClass) {
        return mapToDTOList(seatRepository.findByFlightIdAndSeatClass(flightId, seatClass));
    }

    // HOLD SEAT
    @Override
    public SeatResponseDTO holdSeat(Long flightId, String seatNumber, String userId) {
    	System.out.println("REQ flightId = " + flightId);
        System.out.println("REQ seatNumber = [" + seatNumber + "]");
        System.out.println("REQ userId = " + userId);
        Seat seat = seatRepository
                .findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        
        
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new RuntimeException("Seat not available");
        }
        System.out.println("DB STATUS = " + seat.getStatus());
        seat.setStatus(SeatStatus.HELD);

        return mapToDTO(seatRepository.save(seat));
    }

    // RELEASE SEAT
    @Override
    public SeatResponseDTO releaseSeat(Long flightId, String seatNumber) {

        Seat seat = seatRepository
                .findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (seat.getStatus() == SeatStatus.CONFIRMED) {
            throw new RuntimeException("Cannot release booked seat");
        }

        seat.setStatus(SeatStatus.AVAILABLE);

        return mapToDTO(seatRepository.save(seat));
    }

    // CONFIRM SEAT
    @Override
    public SeatResponseDTO confirmSeat(Long seatId) {

        Seat seat = getSeatOrThrow(seatId);

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new RuntimeException("Seat must be HELD before confirmation");
        }

        seat.setStatus(SeatStatus.CONFIRMED);
        return mapToDTO(seatRepository.save(seat));
    }

    //DELETE BY FLIGHT
    @Override
    @Transactional
    public void deleteSeatsByFlight(Long flightId) {
        seatRepository.deleteByFlightId(flightId);
    }

    //HELPERS

    private Seat getSeatOrThrow(Long seatId) {
        Optional<Seat> seat = seatRepository.findById(seatId);
        if (seat.isEmpty()) {
            throw new RuntimeException("Seat not found with id: " + seatId);
        }
        return seat.get();
    }

    private SeatResponseDTO mapToDTO(Seat seat) {
        SeatResponseDTO dto = new SeatResponseDTO();

        dto.setSeatId(seat.getSeatId());
        dto.setFlightId(seat.getFlightId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatClass(seat.getSeatClass());
        dto.setRowNumber(seat.getRowNumber());
        dto.setColumnLetter(seat.getColumnLetter());
        dto.setStatus(seat.getStatus());

        return dto;
    }

    private List<SeatResponseDTO> mapToDTOList(List<Seat> seats) {

        List<SeatResponseDTO> list = new ArrayList<>();

        for (Seat seat : seats) {
            list.add(mapToDTO(seat));
        }

        return list;
    }
    
    @Override
    public SeatResponseDTO lockSeat(Long flightId, String seatNumber) {

        Seat seat = seatRepository
                .findByFlightIdAndSeatNumber(flightId, seatNumber)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new RuntimeException("Seat must be HELD before confirmation");
        }

        seat.setStatus(SeatStatus.CONFIRMED);

        return mapToDTO(seatRepository.save(seat));
    }

	
}