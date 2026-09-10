package com.system.booking.repository;

import com.system.booking.model.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"user", "resource"})
    Page<Reservation> findAll(Specification<Reservation> spec, @NonNull Pageable pageable);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"user", "resource"})
    List<Reservation> findAll(Specification<Reservation> spec);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"user", "resource"})
    Optional<Reservation> findById(@NonNull Long id);

    /**
     * Checks for any non-CANCELLED reservation on the same resource whose time range
     * overlaps with the requested [startTime, endTime) window.
     * Used in conjunction with PESSIMISTIC_WRITE lock on Resource to prevent race conditions.
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.resource.id = :resourceId " +
           "AND r.status <> com.system.booking.model.enums.ReservationStatus.CANCELLED " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean existsOverlappingReservation(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
