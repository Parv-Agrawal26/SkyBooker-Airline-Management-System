package com.skybooker.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.booking.controller.BookingController;
import com.skybooker.booking.dto.BookingRequest;
import com.skybooker.booking.dto.BookingResponse;
import com.skybooker.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = BookingController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.booking.security.JwtFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private com.skybooker.booking.security.JwtUtil jwtUtil;

    private BookingResponse buildResponse(Long id) {
        return new BookingResponse(id, "Booking successful", true,
                10L, "Delhi", "Mumbai", "2025-12-01", "10:00", "IndiGo");
    }

    @Test
    void bookFlight_ShouldReturn200() throws Exception {
        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);
        req.setUserEmail("rahul@gmail.com");
        req.setSeats(2);

        when(bookingService.bookFlight(any(BookingRequest.class))).thenReturn(buildResponse(1L));

        mockMvc.perform(post("/bookings")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.source").value("Delhi"))
                .andExpect(jsonPath("$.destination").value("Mumbai"));
    }

    @Test
    void getBookingById_ShouldReturn200() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(buildResponse(1L));

        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.airline").value("IndiGo"));
    }

    @Test
    void getBookingById_WhenNotFound_ShouldReturnErrorStatus() throws Exception {
        when(bookingService.getBookingById(999L))
                .thenThrow(new RuntimeException("Booking not found: 999"));

        mockMvc.perform(get("/bookings/999"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void cancelBooking_ShouldReturn200() throws Exception {
        doNothing().when(bookingService).cancelBooking(1L);

        mockMvc.perform(delete("/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking cancelled successfully"));
    }
}
