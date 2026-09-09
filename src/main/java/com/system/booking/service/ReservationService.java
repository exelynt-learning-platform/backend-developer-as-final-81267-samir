package com.system.booking.service;

import com.system.booking.dto.request.ReservationRequest;
import com.system.booking.dto.response.ReservationResponse;
import com.system.booking.exception.ResourceNotFoundException;
import com.system.booking.model.entity.Reservation;
import com.system.booking.model.entity.Resource;
import com.system.booking.model.entity.User;
import com.system.booking.model.enums.ReservationStatus;
import com.system.booking.model.enums.Role;
import com.system.booking.repository.ReservationRepository;
import com.system.booking.repository.ResourceRepository;
import com.system.booking.repository.UserRepository;
import com.system.booking.specification.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be strictly after start time");
        }

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.getResourceId()));

        boolean isOverlapping = reservationRepository.existsOverlappingReservation(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (isOverlapping) {
            throw new IllegalArgumentException("Resource is already booked for the selected time window");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.CONFIRMED)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created successfully with ID: {} for user: {}", saved.getId(), username);

        return ReservationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        Long filterUserId = null;
        if (!isAdmin) {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", currentUsername));
            filterUserId = currentUser.getId();
        }

        Specification<Reservation> spec = ReservationSpecification.filterReservations(
                filterUserId,
                status,
                minPrice,
                maxPrice
        );

        return reservationRepository.findAll(spec, pageable)
                .map(ReservationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (!isAdmin && !reservation.getUser().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You do not have permission to access this reservation");
        }

        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (!isAdmin && !reservation.getUser().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You do not have permission to cancel this reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation with ID: {} was cancelled by: {}", id, currentUsername);

        return ReservationResponse.fromEntity(updated);
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatus newStatus) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        reservation.setStatus(newStatus);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation with ID: {} status updated to: {}", id, newStatus);

        return ReservationResponse.fromEntity(updated);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return authentication.getName();
    }
}
