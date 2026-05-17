package com.skybooker.flight;

import com.skybooker.flight.dto.FlightRequest;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.repository.FlightRepository;
import com.skybooker.flight.service.FlightServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    // RestTemplate bhi mock karo — autoGenerateSeats HTTP call karta hai
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FlightServiceImpl flightServiceImpl;

    // Ek ready-made Flight entity banana ka helper
    private Flight banaoFlight() {
        Flight f = new Flight();
        f.setId(1L);
        f.setFlightNumber("6E-101");
        f.setAirline("IndiGo");
        f.setSource("DEL");
        f.setDestination("BOM");
        f.setDepartureDate(LocalDate.now().plusDays(5));
        f.setDepartureTime("10:00");
        f.setArrivalDate(LocalDate.now().plusDays(5));
        f.setArrivalTime("12:00");
        f.setTotalSeats(180);
        f.setAvailableSeats(180);
        f.setPrice(5000.0);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        return f;
    }

    // Ek ready-made FlightRequest banana ka helper
    private FlightRequest banaoRequest() {
        FlightRequest req = new FlightRequest();
        req.setFlightNumber("6E-101");
        req.setAirline("IndiGo");
        req.setSource("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.now().plusDays(5));
        req.setDepartureTime("10:00");
        req.setArrivalDate(LocalDate.now().plusDays(5));
        req.setArrivalTime("12:00");
        req.setTotalSeats(6); // kam seats rakho taaki autoGenerateSeats jaldi khatam ho
        req.setPrice(5000.0);
        return req;
    }

    // ---------------------------------------------------------------
    // ADD FLIGHT TESTS
    // ---------------------------------------------------------------

    @Test
    void addFlight_WithValidData_ShouldSaveAndGenerateSeats() {
        FlightRequest req = banaoRequest();
        Flight savedFlight = banaoFlight();
        
        org.springframework.test.util.ReflectionTestUtils.setField(flightServiceImpl, "jwtSecret", "my-secret-key-123456789012345678901234567890");
        org.springframework.test.util.ReflectionTestUtils.setField(flightServiceImpl, "seatServiceUrl", "http://localhost:8086");
        
        when(flightRepository.save(any(Flight.class))).thenReturn(savedFlight);
        when(restTemplate.postForObject(anyString(), any(), eq(Object.class))).thenReturn(null);

        FlightResponse res = flightServiceImpl.addFlight(req);

        assertNotNull(res);
        assertEquals("6E-101", res.getFlightNumber());
        verify(flightRepository, times(1)).save(any(Flight.class));
        verify(restTemplate, atLeastOnce()).postForObject(anyString(), any(), eq(Object.class));
    }

    @Test
    void addFlight_WithPastDepartureDate_ShouldThrowException() {
        FlightRequest req = banaoRequest();
        req.setDepartureDate(LocalDate.now().minusDays(1));
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightServiceImpl.addFlight(req));
        assertTrue(ex.getMessage().contains("Departure date cannot be in the past"));
    }

    @Test
    void addFlight_WithSameDayButPastTime_ShouldThrowException() {
        FlightRequest req = banaoRequest();
        req.setDepartureDate(LocalDate.now());
        req.setDepartureTime(LocalDateTime.now().minusMinutes(10).toLocalTime().toString().substring(0,5));
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightServiceImpl.addFlight(req));
        assertTrue(ex.getMessage().contains("Departure time has already passed for today"));
    }

    @Test
    void addFlight_WithArrivalDateBeforeDepartureDate_ShouldThrowException() {
        FlightRequest req = banaoRequest();
        req.setArrivalDate(LocalDate.now().plusDays(2));
        req.setDepartureDate(LocalDate.now().plusDays(5));
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightServiceImpl.addFlight(req));
        assertTrue(ex.getMessage().contains("Arrival date cannot be before departure date"));
    }

    @Test
    void addFlight_WithSameDayArrivalBeforeDepartureTime_ShouldThrowException() {
        FlightRequest req = banaoRequest();
        req.setDepartureDate(LocalDate.now().plusDays(5));
        req.setArrivalDate(LocalDate.now().plusDays(5));
        req.setDepartureTime("14:00");
        req.setArrivalTime("10:00");
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightServiceImpl.addFlight(req));
        assertTrue(ex.getMessage().contains("arrival time must be after departure time"));
    }

    // ---------------------------------------------------------------
    // GET FLIGHT BY ID TESTS
    // ---------------------------------------------------------------

    // Test 1: Sahi ID se flight mile
    @Test
    void getFlightById_WhenFlightExists_ShouldReturnFlight() {
        Flight flight = banaoFlight();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        FlightResponse res = flightServiceImpl.getFlightById(1L);

        assertNotNull(res);
        assertEquals("6E-101", res.getFlightNumber());
        assertEquals("DEL", res.getSource());
        assertEquals("BOM", res.getDestination());
    }

    // Test 2: Galat ID se exception aaye
    @Test
    void getFlightById_WhenFlightNotFound_ShouldThrowException() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.getFlightById(99L));

        assertTrue(ex.getMessage().contains("Flight not found"));
    }

    // ---------------------------------------------------------------
    // GET ALL FLIGHTS TESTS
    // ---------------------------------------------------------------

    // Test 3: Saari flights milein
    @Test
    void getAllFlights_ShouldReturnAllFlights() {
        Flight f1 = banaoFlight();
        Flight f2 = banaoFlight();
        f2.setId(2L);
        f2.setFlightNumber("AI-202");

        when(flightRepository.findAll()).thenReturn(List.of(f1, f2));

        List<FlightResponse> result = flightServiceImpl.getAllFlights();

        assertEquals(2, result.size());
    }

    // Test 4: Koi flight nahi hai toh empty list aaye
    @Test
    void getAllFlights_WhenNoFlights_ShouldReturnEmptyList() {
        when(flightRepository.findAll()).thenReturn(List.of());

        List<FlightResponse> result = flightServiceImpl.getAllFlights();

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // SEARCH FLIGHTS TESTS
    // ---------------------------------------------------------------

    // Test 5: Source, destination aur date se flights milein
    @Test
    void searchFlights_WithValidParams_ShouldReturnMatchingFlights() {
        Flight flight = banaoFlight();
        LocalDate date = LocalDate.now().plusDays(5);

        when(flightRepository.findBySourceAndDestinationAndDepartureDate("DEL", "BOM", date))
                .thenReturn(List.of(flight));

        List<FlightResponse> result = flightServiceImpl.searchFlights("DEL", "BOM", date);

        assertEquals(1, result.size());
        assertEquals("DEL", result.get(0).getSource());
        assertEquals("BOM", result.get(0).getDestination());
    }

    // Test 6: Koi matching flight nahi ho toh empty list aaye
    @Test
    void searchFlights_WhenNoMatch_ShouldReturnEmptyList() {
        LocalDate date = LocalDate.now().plusDays(5);

        when(flightRepository.findBySourceAndDestinationAndDepartureDate("DEL", "CCU", date))
                .thenReturn(List.of());

        List<FlightResponse> result = flightServiceImpl.searchFlights("DEL", "CCU", date);

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // REDUCE SEATS TESTS
    // ---------------------------------------------------------------

    // Test 7: Seats sahi se reduce ho
    @Test
    void reduceSeats_WhenEnoughSeatsAvailable_ShouldSucceed() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(50);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        String result = flightServiceImpl.reduceSeats(1L, 2);

        assertEquals("Seats reduced successfully", result);
        // 50 - 2 = 48 hone chahiye
        assertEquals(48, flight.getAvailableSeats());
    }

    // Test 8: Jitne seats maange utne available na ho toh exception aaye
    @Test
    void reduceSeats_WhenNotEnoughSeats_ShouldThrowException() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(1); // sirf 1 seat hai

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        // 5 seats mangna — impossible
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.reduceSeats(1L, 5));

        assertTrue(ex.getMessage().contains("Not enough seats"));
    }

    // Test 9: Flight nahi mili toh seats reduce karne pe exception aaye
    @Test
    void reduceSeats_WhenFlightNotFound_ShouldThrowException() {
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.reduceSeats(999L, 1));

        assertTrue(ex.getMessage().contains("Flight not found"));
    }

    // Test 10: Price aur seats response mein sahi aayein
    @Test
    void getFlightById_ResponseShouldHaveCorrectPriceAndSeats() {
        Flight flight = banaoFlight();
        flight.setPrice(7500.0);
        flight.setAvailableSeats(100);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        FlightResponse res = flightServiceImpl.getFlightById(1L);

        assertEquals(7500.0, res.getPrice());
        assertEquals(100, res.getAvailableSeats());
    }

    // Test 11: Seats restore ho jayein
    @Test
    void restoreSeats_ShouldIncreaseAvailableSeats() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(100);
        flight.setTotalSeats(180);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        String result = flightServiceImpl.restoreSeats(1L, 10);

        assertEquals("Seats restored successfully", result);
        assertEquals(110, flight.getAvailableSeats());
    }

    // Test 12: Restore totalSeats se zyada nahi ho sakta
    @Test
    void restoreSeats_ShouldNotExceedTotalSeats() {
        Flight flight = banaoFlight();
        flight.setAvailableSeats(175);
        flight.setTotalSeats(180);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        flightServiceImpl.restoreSeats(1L, 20); // 175 + 20 = 195 > 180, should cap at 180

        assertEquals(180, flight.getAvailableSeats());
    }

    // Test 13: Flight status update ho jaye
    @Test
    void updateStatus_WithValidStatus_ShouldSucceed() {
        Flight flight = banaoFlight();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> {
            Flight f = i.getArgument(0);
            f.setStatus("DELAYED");
            return f;
        });

        FlightResponse res = flightServiceImpl.updateStatus(1L, "DELAYED");

        assertEquals("DELAYED", res.getStatus());
    }

    // Test 14: Invalid status se exception aaye
    @Test
    void updateStatus_WithInvalidStatus_ShouldThrowException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightServiceImpl.updateStatus(1L, "UNKNOWN"));

        assertTrue(ex.getMessage().contains("Invalid status"));
    }
}
