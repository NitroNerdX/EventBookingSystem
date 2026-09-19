package com.event.booking.service;

import com.event.booking.dto.request.LoginRequest;
import com.event.booking.dto.request.RegisterRequest;
import com.event.booking.dto.response.AuthResponse;
import com.event.booking.entity.User;
import com.event.booking.exception.EmailAlreadyExistsException;
import com.event.booking.mapper.EntityMapper;
import com.event.booking.repository.UserRepository;
import com.event.booking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt for email={}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .createdAt(OffsetDateTime.now())
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        log.info("Registered new user id={} email={}", saved.getId(), saved.getEmail());
        return new AuthResponse(token, EntityMapper.toUserResponse(saved));
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt email={}", request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        // authenticate() throws BadCredentialsException on failure —
        // caught by Step 8's GlobalExceptionHandler, never reaches here on bad creds

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(); // unreachable in practice — authenticate() already validated existence

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, EntityMapper.toUserResponse(user));
    }
}