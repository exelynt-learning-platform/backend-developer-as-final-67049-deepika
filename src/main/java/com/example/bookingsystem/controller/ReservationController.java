package com.example.bookingsystem.controller;

import com.example.bookingsystem.dto.ReservationRequest;
import com.example.bookingsystem.dto.ReservationResponse;
import com.example.bookingsystem.dto.ReservationUpdateRequest;
import com.example.bookingsystem.entity.ReservationStatus;
import com.example.bookingsystem.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Resource bookings")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Create a reservation. The owner is always taken from the JWT, never from the request body.")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                        Authentication authentication) {
        ReservationResponse response = reservationService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "List reservations. ADMIN sees all; USER sees only their own. " +
            "Supports filtering by status/minPrice/maxPrice, pagination, and sorting.")
    public ResponseEntity<Page<ReservationResponse>> list(
            @Parameter(description = "Filter by reservation status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            Authentication authentication) {

        boolean isAdmin = isAdmin(authentication);
        Page<ReservationResponse> result = reservationService.list(
                status, minPrice, maxPrice, authentication.getName(), isAdmin, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Get a reservation by id. USER can only access their own; ADMIN can access any.")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.getById(id, authentication.getName(), isAdmin));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fully update a reservation, including status (ADMIN only)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.adminUpdate(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Cancel a reservation. USER can cancel only their own; ADMIN can cancel any.")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.cancel(id, authentication.getName(), isAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }
}
