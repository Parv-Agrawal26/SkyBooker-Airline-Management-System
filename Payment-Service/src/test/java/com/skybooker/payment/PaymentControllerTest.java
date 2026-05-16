package com.skybooker.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.payment.controller.PaymentController;
import com.skybooker.payment.dto.PaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = PaymentController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.skybooker.payment.security.JwtFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private com.skybooker.payment.security.JwtUtil jwtUtil;

    private PaymentResponse buildResponse(String status) {
        PaymentResponse r = new PaymentResponse();
        r.setPaymentId(1L);
        r.setBookingId(101L);
        r.setUserEmail("rahul@gmail.com");
        r.setAmount(5000.0);
        r.setCurrency("INR");
        r.setPaymentMode("UPI");
        r.setStatus(status);
        r.setTransactionId("UPI-ABCD1234");
        r.setPaidAt(LocalDateTime.now());
        r.setMessage("Payment successful");
        return r;
    }

    @Test
    void initiatePayment_ShouldReturn200() throws Exception {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(101L);
        req.setUserEmail("rahul@gmail.com");
        req.setAmount(5000.0);
        req.setPaymentMode("UPI");

        when(paymentService.initiatePayment(any(PaymentRequest.class))).thenReturn(buildResponse("PAID"));

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amount").value(5000.0))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void getPaymentByBooking_ShouldReturn200() throws Exception {
        when(paymentService.getPaymentByBooking(101L)).thenReturn(buildResponse("PAID"));

        mockMvc.perform(get("/payments/booking/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(101))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getPaymentsByUser_ShouldReturn200WithList() throws Exception {
        when(paymentService.getPaymentsByUser("rahul@gmail.com"))
                .thenReturn(List.of(buildResponse("PAID")));

        mockMvc.perform(get("/payments/user/rahul@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void processRefund_ShouldReturn200() throws Exception {
        PaymentResponse refunded = buildResponse("REFUNDED");
        refunded.setRefundAmount(5000.0);
        when(paymentService.processRefund(101L)).thenReturn(refunded);

        mockMvc.perform(post("/payments/refund/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundAmount").value(5000.0));
    }

    @Test
    void getPaymentsByStatus_ShouldReturn200() throws Exception {
        when(paymentService.getPaymentsByStatus("PAID")).thenReturn(List.of(buildResponse("PAID")));

        mockMvc.perform(get("/payments/status/PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }
}
