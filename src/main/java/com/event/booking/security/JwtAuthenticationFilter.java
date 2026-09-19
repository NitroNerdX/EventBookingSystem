package com.event.booking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {


        String path = request.getServletPath();
        log.debug("JwtAuthenticationFilter incoming request path={}", path);

        if (path.startsWith("/api/auth/")) {
           log.debug("Skipping JWT filter for public path={}", path);
           filterChain.doFilter(request, response);
           return;
        }


        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header present={}", authHeader != null);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing or invalid Authorization header for path={}", path);
            // Delegate to the configured AuthenticationEntryPoint for consistent 401 handling
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(
                            "Authentication required. Missing or invalid Authorization header. Expected format: Authorization: Bearer <jwt>"));
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            log.debug("Empty bearer token provided for path={}", path);
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(
                            "Authentication failed. Authorization header contains an empty bearer token."));
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set for user email={}", email);
                }
            }

        } catch (io.jsonwebtoken.JwtException ex) {
            log.warn("Invalid JWT token for path={}: {}", path, ex.getMessage());
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(
                            "Authentication failed. The JWT token is invalid, expired, or malformed.", ex));
            return;
        }

        filterChain.doFilter(request, response);
    }
}