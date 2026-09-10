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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------
    // Helper: sets up SecurityContextHolder with a standard ROLE_USER principal
    // ---------------------------------------------------------------------------
    private void setupSecurityContextAsUser(String username) {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn(username);
        lenient().doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();
        SecurityContextHolder.setContext(securityContext);
    }

    private void setupSecurityContextAsAdmin(String username) {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn(username);
        lenient().doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        SecurityContextHolder.setContext(securityContext);
    }

    // ---------------------------------------------------------------------------
    // createReservation tests
    // ---------------------------------------------------------------------------

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

        setupSecurityContextAsUser("john_doe");

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        // IMPORTANT: service now uses findByIdWithLock (pessimistic write) not findById
        when(resourceRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testResource));
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

        verify(userRepository).findByUsername("john_doe");
        verify(resourceRepository).findByIdWithLock(10L);
        verify(reservationRepository).existsOverlappingReservation(10L, start, end);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when endTime is before or equal to startTime")
    void createReservation_InvalidDateOrder_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusHours(1); // end BEFORE start

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("100.00"))
                .build();

        setupSecurityContextAsUser("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(request)
        );

        assertTrue(exception.getMessage().contains("End time must be strictly after start time"));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when resource is already booked (double-booking)")
    void createReservation_OverlappingReservation_ThrowsResourceConflictException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .price(new BigDecimal("200.00"))
                .build();

        setupSecurityContextAsUser("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        // Service uses findByIdWithLock to prevent race conditions
        when(resourceRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservation(10L, start, end)).thenReturn(true);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> reservationService.createReservation(request)
        );

        assertTrue(exception.getMessage().contains("already booked"));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    // ---------------------------------------------------------------------------
    // getReservationById tests
    // ---------------------------------------------------------------------------

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
        setupSecurityContextAsUser("john_doe");

        ReservationResponse response = reservationService.getReservationById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("john_doe", response.getUsername());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner tries to view another user's reservation")
    void getReservationById_AsNonOwner_ThrowsAccessDeniedException() {
        // Reservation belongs to anotherUser (jane_doe), but john_doe is trying to access it
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
        setupSecurityContextAsUser("john_doe");

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.getReservationById(100L)
        );
    }

    @Test
    @DisplayName("Admin should be able to view any user's reservation")
    void getReservationById_AsAdmin_Success() {
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
        setupSecurityContextAsAdmin("admin");

        ReservationResponse response = reservationService.getReservationById(100L);
        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    // ---------------------------------------------------------------------------
    // cancelReservation tests
    // ---------------------------------------------------------------------------

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
        setupSecurityContextAsUser("john_doe");
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(100L);

        assertNotNull(response);
        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        verify(reservationRepository).save(reservation);
    }

    // ---------------------------------------------------------------------------
    // updateReservationStatus tests
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Admin should update reservation status directly")
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
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.updateReservationStatus(100L, ReservationStatus.CONFIRMED);

        assertNotNull(response);
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
    }
}
