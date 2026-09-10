package com.system.booking.service;

import com.system.booking.dto.request.LoginRequest;
import com.system.booking.dto.request.RegisterRequest;
import com.system.booking.dto.response.AuthResponse;
import com.system.booking.exception.BadRequestException;
import com.system.booking.model.entity.User;
import com.system.booking.model.enums.Role;
import com.system.booking.repository.UserRepository;
import com.system.booking.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .password("encoded_pass")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("Should successfully authenticate user and return JWT token")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("john_doe")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        when(tokenProvider.generateToken(authentication)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("john_doe", response.getUsername());
        assertEquals("ROLE_USER", response.getRole());
        assertEquals("Bearer", response.getType());
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when authentication fails")
    void login_BadCredentials_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .username("john_doe")
                .password("wrong_password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("new_user")
                .password("secret123")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateToken("new_user", "ROLE_USER")).thenReturn("mock.registered.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.registered.token", response.getToken());
        assertEquals("new_user", response.getUsername());
        assertEquals("ROLE_USER", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when username already exists")
    void register_DuplicateUsername_ThrowsBadRequestException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .password("secret123")
                .build();

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertTrue(exception.getMessage().contains("already taken"));
        verify(userRepository, never()).save(any(User.class));
    }
}
