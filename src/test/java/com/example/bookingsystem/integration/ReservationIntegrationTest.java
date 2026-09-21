package com.example.bookingsystem.integration;

import com.example.bookingsystem.dto.ReservationRequest;
import com.example.bookingsystem.entity.Reservation;
import com.example.bookingsystem.entity.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationIntegrationTest extends AbstractIntegrationTest {

    private ReservationRequest sampleRequest() {
        return new ReservationRequest(
                testResource.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("49.99"));
    }

    @Test
    void createReservation_asUser_ownerComesFromToken() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", tokenFor("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReservation_withEndBeforeStart_returns400() throws Exception {
        ReservationRequest request = new ReservationRequest(
                testResource.getId(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), // before start -> invalid
                new BigDecimal("10.00"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", tokenFor("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_forUnavailableResource_returns400() throws Exception {
        testResource.setAvailable(false);
        resourceRepository.save(testResource);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", tokenFor("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void user_cannotAccessAnotherUsersReservation() throws Exception {
        Reservation aliceReservation = reservationRepository.save(Reservation.builder()
                .resource(testResource)
                .user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00"))
                .status(ReservationStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/reservations/" + aliceReservation.getId())
                        .header("Authorization", tokenFor("bob")))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_canAccessOwnReservation() throws Exception {
        Reservation aliceReservation = reservationRepository.save(Reservation.builder()
                .resource(testResource)
                .user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00"))
                .status(ReservationStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/reservations/" + aliceReservation.getId())
                        .header("Authorization", tokenFor("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void admin_canAccessAnyReservation() throws Exception {
        Reservation aliceReservation = reservationRepository.save(Reservation.builder()
                .resource(testResource)
                .user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00"))
                .status(ReservationStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/reservations/" + aliceReservation.getId())
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void user_listReservations_onlySeesOwnReservations() throws Exception {
        reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00")).status(ReservationStatus.PENDING).build());

        reservationRepository.save(Reservation.builder()
                .resource(testResource).user(secondUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("30.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(get("/api/reservations").header("Authorization", tokenFor("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    @Test
    void admin_listReservations_seesAll() throws Exception {
        reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00")).status(ReservationStatus.PENDING).build());

        reservationRepository.save(Reservation.builder()
                .resource(testResource).user(secondUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("30.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(get("/api/reservations").header("Authorization", tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void listReservations_filterByStatusAndPriceRange() throws Exception {
        reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("15.00")).status(ReservationStatus.CANCELLED).build());

        reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("55.00")).status(ReservationStatus.CONFIRMED).build());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", tokenFor("admin"))
                        .param("status", "CONFIRMED")
                        .param("minPrice", "50")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    void listReservations_pagination() throws Exception {
        for (int i = 0; i < 15; i++) {
            reservationRepository.save(Reservation.builder()
                    .resource(testResource).user(normalUser)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .price(new BigDecimal("10.00")).status(ReservationStatus.PENDING).build());
        }

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", tokenFor("admin"))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void user_cannotCancelAnotherUsersReservation() throws Exception {
        Reservation aliceReservation = reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(patch("/api/reservations/" + aliceReservation.getId() + "/cancel")
                        .header("Authorization", tokenFor("bob")))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_canCancelOwnReservation() throws Exception {
        Reservation aliceReservation = reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(patch("/api/reservations/" + aliceReservation.getId() + "/cancel")
                        .header("Authorization", tokenFor("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void user_cannotDeleteReservation_onlyAdminCan() throws Exception {
        Reservation aliceReservation = reservationRepository.save(Reservation.builder()
                .resource(testResource).user(normalUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(new BigDecimal("20.00")).status(ReservationStatus.PENDING).build());

        mockMvc.perform(delete("/api/reservations/" + aliceReservation.getId())
                        .header("Authorization", tokenFor("alice")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reservations/" + aliceReservation.getId())
                        .header("Authorization", tokenFor("admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void ownerIdIsIgnored_evenIfClientTriesToSpoofIt() throws Exception {
        // ReservationRequest has no userId field at all, so identity can only come from the JWT.
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", tokenFor("bob"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"));
    }
}
