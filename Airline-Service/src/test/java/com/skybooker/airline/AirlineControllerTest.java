package com.skybooker.airline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.airline.controller.AirlineController;
import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.dto.AirlineResponse;
import com.skybooker.airline.dto.AirportResponse;
import com.skybooker.airline.service.AirlineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AirlineController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.airline.security.JwtFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class AirlineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AirlineService airlineService;

    @MockBean
    private com.skybooker.airline.security.JwtUtil jwtUtil;

    private AirlineResponse buildAirlineResponse() {
        AirlineResponse r = new AirlineResponse();
        r.setId(1L);
        r.setName("IndiGo");
        r.setIataCode("6E");
        r.setCountry("India");
        r.setActive(true);
        r.setMessage("Success");
        return r;
    }

    private AirportResponse buildAirportResponse() {
        AirportResponse r = new AirportResponse();
        r.setId(1L);
        r.setName("Indira Gandhi International Airport");
        r.setIataCode("DEL");
        r.setCity("Delhi");
        r.setCountry("India");
        r.setMessage("Success");
        return r;
    }

    @Test
    void addAirline_ShouldReturn200() throws Exception {
        AirlineRequest req = new AirlineRequest();
        req.setName("IndiGo");
        req.setIataCode("6E");
        req.setCountry("India");

        when(airlineService.addAirline(any(AirlineRequest.class))).thenReturn(buildAirlineResponse());

        mockMvc.perform(post("/airlines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("6E"))
                .andExpect(jsonPath("$.name").value("IndiGo"));
    }

    @Test
    void getAirlineById_ShouldReturn200() throws Exception {
        when(airlineService.getAirlineById(1L)).thenReturn(buildAirlineResponse());

        mockMvc.perform(get("/airlines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IndiGo"));
    }

    @Test
    void getAirlineByIata_ShouldReturn200() throws Exception {
        when(airlineService.getAirlineByIata("6E")).thenReturn(buildAirlineResponse());

        mockMvc.perform(get("/airlines/iata/6E"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("6E"));
    }

    @Test
    void getAllAirlines_ShouldReturn200WithList() throws Exception {
        when(airlineService.getAllAirlines()).thenReturn(List.of(buildAirlineResponse()));

        mockMvc.perform(get("/airlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getActiveAirlines_ShouldReturn200() throws Exception {
        when(airlineService.getActiveAirlines()).thenReturn(List.of(buildAirlineResponse()));

        mockMvc.perform(get("/airlines/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void toggleAirlineStatus_ShouldReturn200() throws Exception {
        AirlineResponse toggled = buildAirlineResponse();
        toggled.setActive(false);
        toggled.setMessage("Airline deactivated successfully");
        when(airlineService.toggleAirlineStatus(1L)).thenReturn(toggled);

        mockMvc.perform(put("/airlines/1/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void searchAirports_ShouldReturn200() throws Exception {
        when(airlineService.searchAirports("Delhi")).thenReturn(List.of(buildAirportResponse()));

        mockMvc.perform(get("/airports/search").param("keyword", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Delhi"));
    }

    @Test
    void getAirportByIata_ShouldReturn200() throws Exception {
        when(airlineService.getAirportByIata("DEL")).thenReturn(buildAirportResponse());

        mockMvc.perform(get("/airports/iata/DEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("DEL"));
    }
}
