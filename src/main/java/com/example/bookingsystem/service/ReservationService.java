package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.ReservationRequest;
import com.example.bookingsystem.dto.ReservationResponse;
import com.example.bookingsystem.dto.ReservationUpdateRequest;
import com.example.bookingsystem.entity.Reservation;
import com.example.bookingsystem.entity.ReservationStatus;
import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.exception.BadRequestException;
import com.example.bookingsystem.exception.ResourceNotFoundException;
import com.example.bookingsystem.repository.ReservationRepository;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.specification.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;

    @Transactional
    public ReservationResponse create(ReservationRequest request, String username) {
        User user = getUserOrThrow(username);
        Resource resource = resourceService.findEntity(request.getResourceId());

        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is not available for booking");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(user) // identity always taken from JWT-derived user, never from request body
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id, String username, boolean isAdmin) {
        Reservation reservation = findEntity(id);
        assertOwnershipOrAdmin(reservation, username, isAdmin);
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> list(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                           String username, boolean isAdmin, Pageable pageable) {
        Long userId = null;
        if (!isAdmin) {
            userId = getUserOrThrow(username).getId();
        }

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }

        return reservationRepository
                .findAll(ReservationSpecification.filter(userId, status, minPrice, maxPrice), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ReservationResponse adminUpdate(Long id, ReservationUpdateRequest request) {
        Reservation reservation = findEntity(id);
        Resource resource = resourceService.findEntity(request.getResourceId());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(request.getStatus());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long id, String username, boolean isAdmin) {
        Reservation reservation = findEntity(id);
        assertOwnershipOrAdmin(reservation, username, isAdmin);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = findEntity(id);
        reservationRepository.delete(reservation);
    }

    private void assertOwnershipOrAdmin(Reservation reservation, String username, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have access to this reservation");
        }
    }

    private Reservation findEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource().getId())
                .resourceName(r.getResource().getName())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .price(r.getPrice())
                .status(r.getStatus())
                .build();
    }
}
