package com.system.booking.service;

import com.system.booking.dto.request.ReservationRequest;
import com.system.booking.dto.response.ReservationResponse;
import com.system.booking.model.entity.Reservation;
import com.system.booking.model.entity.Resource;
import com.system.booking.model.entity.User;
import com.system.booking.model.enums.ReservationStatus;
import com.system.booking.model.enums.Role;
import com.system.booking.repository.ReservationRepository;
import com.system.booking.repository.ResourceRepository;
import com.system.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .password("encoded_pass")
                .role(Role.ROLE_USER)
                .build();

        testResource = Resource.builder()
                .id(10L)
                .name("Conference Room A")
                .description("4K Display Room")
                .type("CONFERENCE_ROOM")
                .build();
    }

    @Test
    @DisplayName("Should extract authenticated user identity from SecurityContext and create reservation")
    void createReservation_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservation(10L, start, end)).thenReturn(false);

        Reservation savedReservation = Reservation.builder()
                .id(100L)
                .user(testUser)
                .resource(testResource)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("150.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("john_doe", response.getUsername());
        assertEquals(1L, response.getUserId());
        assertEquals(10L, response.getResourceId());
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());

        verify(userRepository, times(1)).findByUsername("john_doe");
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when endTime is before or equal to startTime")
    void createReservation_InvalidDateOrder_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusHours(1);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(request)
        );

        assertTrue(exception.getMessage().contains("End time must be strictly after start time"));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when resource is already booked for the time window")
    void createReservation_OverlappingReservation_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("200.00"))
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservation(10L, start, end)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(request)
        );

        assertTrue(exception.getMessage().contains("already booked"));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }
}
