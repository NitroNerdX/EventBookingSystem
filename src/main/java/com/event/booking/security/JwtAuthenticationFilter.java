package com.event.booking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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



        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Delegate to the configured AuthenticationEntryPoint for consistent 401 handling
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(
                            "Authentication required. Missing or invalid Authorization header. Expected format: Authorization: Bearer <jwt>"));
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
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
                }
            }

        } catch (io.jsonwebtoken.JwtException ex) {
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException(
                            "Authentication failed. The JWT token is invalid, expired, or malformed.", ex));
            return;
        }

        filterChain.doFilter(request, response);
    }
}