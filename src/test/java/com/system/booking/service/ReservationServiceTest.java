package com.system.booking.service;

import com.system.booking.dto.request.ReservationRequest;
import com.system.booking.dto.response.ReservationResponse;
import com.system.booking.exception.ResourceConflictException;
import com.system.booking.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private User anotherUser;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .password("encoded_pass")
                .role(Role.ROLE_USER)
                .build();

        anotherUser = User.builder()
                .id(2L)
                .username("jane_doe")
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
    @DisplayName("Should throw ResourceConflictException when resource is already booked for the time window")
    void createReservation_OverlappingReservation_ThrowsResourceConflictException() {
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

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> reservationService.createReservation(request)
        );

        assertTrue(exception.getMessage().contains("already booked"));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should retrieve reservation by ID for the reservation owner")
    void getReservationById_AsOwner_Success() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .user(testUser)
                .resource(testResource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        SecurityContextHolder.setContext(securityContext);

        ReservationResponse response = reservationService.getReservationById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("john_doe", response.getUsername());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner standard user tries to view reservation")
    void getReservationById_AsNonOwner_ThrowsAccessDeniedException() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .user(anotherUser)
                .resource(testResource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        SecurityContextHolder.setContext(securityContext);

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.getReservationById(100L)
        );
    }

    @Test
    @DisplayName("Should cancel reservation successfully when requested by owner")
    void cancelReservation_AsOwner_Success() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .user(testUser)
                .resource(testResource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("john_doe");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        SecurityContextHolder.setContext(securityContext);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(100L);

        assertNotNull(response);
        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    @DisplayName("Should update reservation status directly for admin")
    void updateReservationStatus_Success() {
        Reservation reservation = Reservation.builder()
                .id(100L)
                .user(testUser)
                .resource(testResource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(100L, ReservationStatus.CONFIRMED);

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }
}
