package com.skybooker.passenger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.passenger.controller.PassengerController;
import com.skybooker.passenger.dto.PassengerRequest;
import com.skybooker.passenger.dto.PassengerResponse;
import com.skybooker.passenger.service.PassengerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = PassengerController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.passenger.security.JwtFilter.class
    )
)
class PassengerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PassengerService passengerService;

    @MockBean
    private com.skybooker.passenger.security.JwtUtil jwtUtil;

    private PassengerResponse buildResponse() {
        PassengerResponse r = new PassengerResponse();
        r.setPassengerId(1L);
        r.setBookingId("BK-001");
        r.setFirstName("Rahul");
        r.setLastName("Sharma");
        r.setPassportNumber("A1234567");
        r.setPassengerType("ADULT");
        r.setTicketNumber("TKT-ABCD1234");
        r.setMessage("Passenger added successfully");
        return r;
    }

    @Test
    @WithMockUser
    void addPassenger_ShouldReturn200() throws Exception {
        PassengerRequest req = new PassengerRequest();
        req.setBookingId("BK-001");
        req.setFirstName("Rahul");
        req.setLastName("Sharma");
        req.setPassengerType("ADULT");
        req.setPassportExpiry(LocalDate.now().plusYears(3));

        when(passengerService.addPassenger(any(PassengerRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/passengers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Rahul"))
                .andExpect(jsonPath("$.ticketNumber").value("TKT-ABCD1234"));
    }

    @Test
    @WithMockUser
    void getPassengerById_ShouldReturn200() throws Exception {
        when(passengerService.getPassengerById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/passengers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengerId").value(1))
                .andExpect(jsonPath("$.bookingId").value("BK-001"));
    }

    @Test
    @WithMockUser
    void getPassengersByBooking_ShouldReturn200WithList() throws Exception {
        when(passengerService.getPassengersByBooking("BK-001")).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/passengers/booking/BK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Rahul"));
    }

    @Test
    @WithMockUser
    void getPassengersByFlight_ShouldReturn200() throws Exception {
        when(passengerService.getPassengersByFlight(10L)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/passengers/flight/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser
    void deletePassenger_ShouldReturn200() throws Exception {
        doNothing().when(passengerService).deletePassenger(1L);

        mockMvc.perform(delete("/passengers/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Passenger deleted successfully"));
    }

    @Test
    @WithMockUser
    void getPassengerCount_ShouldReturn200() throws Exception {
        when(passengerService.getPassengerCount("BK-001")).thenReturn(3);

        mockMvc.perform(get("/passengers/count/BK-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @WithMockUser
    void getByTicket_ShouldReturn200() throws Exception {
        when(passengerService.getByTicketNumber("TKT-ABCD1234")).thenReturn(buildResponse());

        mockMvc.perform(get("/passengers/ticket/TKT-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value("TKT-ABCD1234"));
    }
}
