package com.skybooker.flight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.flight.controller.FlightController;
import com.skybooker.flight.dto.FlightResponse;
import com.skybooker.flight.service.FlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = FlightController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.flight.security.JwtFilter.class
    )
)
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlightService flightService;

    @MockBean
    private com.skybooker.flight.security.JwtUtil jwtUtil;

    private FlightResponse buildResponse() {
        FlightResponse r = new FlightResponse();
        r.setId(1L);
        r.setFlightNumber("6E-101");
        r.setAirline("IndiGo");
        r.setSource("DEL");
        r.setDestination("BOM");
        r.setDepartureDate(LocalDate.now().plusDays(5));
        r.setDepartureTime("10:00");
        r.setArrivalDate(LocalDate.now().plusDays(5));
        r.setArrivalTime("12:00");
        r.setAvailableSeats(180);
        r.setPrice(5000.0);
        r.setStatus("ON_TIME");
        return r;
    }

    @Test
    @WithMockUser
    void getFlightById_ShouldReturn200() throws Exception {
        when(flightService.getFlightById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("6E-101"))
                .andExpect(jsonPath("$.source").value("DEL"))
                .andExpect(jsonPath("$.destination").value("BOM"));
    }

    @Test
    @WithMockUser
    void getAllFlights_ShouldReturn200WithList() throws Exception {
        when(flightService.getAllFlights()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchFlights_WithoutAuth_ShouldReturn200() throws Exception {
        when(flightService.searchFlights(eq("DEL"), eq("BOM"), any(LocalDate.class)))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/flights/search")
                .param("source", "DEL")
                .param("destination", "BOM")
                .param("date", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchFlights_WithBlankDate_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/flights/search")
                .param("source", "DEL")
                .param("destination", "BOM")
                .param("date", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void reduceSeats_ShouldReturn200() throws Exception {
        when(flightService.reduceSeats(1L, 2)).thenReturn("Seats reduced successfully");

        mockMvc.perform(put("/flights/1/reduce-seats").param("seats", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Seats reduced successfully"));
    }

    @Test
    @WithMockUser
    void restoreSeats_ShouldReturn200() throws Exception {
        when(flightService.restoreSeats(1L, 2)).thenReturn("Seats restored successfully");

        mockMvc.perform(put("/flights/1/restore-seats").param("seats", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Seats restored successfully"));
    }

    @Test
    @WithMockUser(roles = "AIRLINE_STAFF")
    void updateStatus_ShouldReturn200() throws Exception {
        FlightResponse updated = buildResponse();
        updated.setStatus("DELAYED");
        when(flightService.updateStatus(1L, "DELAYED")).thenReturn(updated);

        mockMvc.perform(put("/flights/1/status").param("status", "DELAYED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELAYED"));
    }
}
