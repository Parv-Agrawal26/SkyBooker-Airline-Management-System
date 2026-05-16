package com.skybooker.seat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.seat.controller.SeatController;
import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;
import com.skybooker.seat.service.SeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = SeatController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.seat.security.JwtFilter.class
    )
)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeatService seatService;

    @MockBean
    private com.skybooker.seat.security.JwtUtil jwtUtil;

    private SeatResponse buildResponse(String status) {
        SeatResponse r = new SeatResponse();
        r.setId(1L);
        r.setFlightId(101L);
        r.setSeatNumber("12A");
        r.setSeatClass("ECONOMY");
        r.setRow(12);
        r.setColumn("A");
        r.setWindow(true);
        r.setAisle(false);
        r.setStatus(status);
        r.setPriceMultiplier(1.0);
        r.setMessage("Success");
        return r;
    }

    @Test
    @WithMockUser
    void addSeat_ShouldReturn200() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setFlightId(101L);
        req.setSeatNumber("12A");
        req.setSeatClass("ECONOMY");
        req.setRow(12);
        req.setColumn("A");
        req.setPriceMultiplier(1.0);

        when(seatService.addSeat(any(SeatRequest.class))).thenReturn(buildResponse("AVAILABLE"));

        mockMvc.perform(post("/seats")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatNumber").value("12A"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser
    void getSeatsByFlight_ShouldReturn200WithList() throws Exception {
        when(seatService.getSeatsByFlight(101L)).thenReturn(List.of(buildResponse("AVAILABLE")));

        mockMvc.perform(get("/seats/flight/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].seatNumber").value("12A"));
    }

    @Test
    @WithMockUser
    void getAvailableSeats_ShouldReturn200() throws Exception {
        when(seatService.getAvailableSeats(101L)).thenReturn(List.of(buildResponse("AVAILABLE")));

        mockMvc.perform(get("/seats/flight/101/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser
    void getSeatsByClass_ShouldReturn200() throws Exception {
        when(seatService.getSeatsByClass(101L, "ECONOMY")).thenReturn(List.of(buildResponse("AVAILABLE")));

        mockMvc.perform(get("/seats/flight/101/class/ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatClass").value("ECONOMY"));
    }

    @Test
    @WithMockUser
    void holdSeat_ShouldReturn200WithHeldStatus() throws Exception {
        when(seatService.holdSeat(101L, "12A")).thenReturn(buildResponse("HELD"));

        mockMvc.perform(put("/seats/flight/101/hold/12A").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    @WithMockUser
    void confirmSeat_ShouldReturn200WithConfirmedStatus() throws Exception {
        when(seatService.confirmSeat(101L, "12A")).thenReturn(buildResponse("CONFIRMED"));

        mockMvc.perform(put("/seats/flight/101/confirm/12A").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    void releaseSeat_ShouldReturn200WithAvailableStatus() throws Exception {
        when(seatService.releaseSeat(101L, "12A")).thenReturn(buildResponse("AVAILABLE"));

        mockMvc.perform(put("/seats/flight/101/release/12A").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser
    void getAvailableCount_ShouldReturn200() throws Exception {
        when(seatService.getAvailableCount(101L)).thenReturn(150);

        mockMvc.perform(get("/seats/flight/101/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }
}
