package com.example.bookingsystem.specification;

import com.example.bookingsystem.entity.Reservation;
import com.example.bookingsystem.entity.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> filter(Long userId,
                                                      ReservationStatus status,
                                                      BigDecimal minPrice,
                                                      BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Ownership scoping: null userId means "no restriction" (ADMIN)
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
