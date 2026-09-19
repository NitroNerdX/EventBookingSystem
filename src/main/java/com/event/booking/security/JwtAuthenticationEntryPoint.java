package com.event.booking.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Sends a 401 JSON response for unauthenticated requests.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setContentType("application/json");
        log.warn("Unauthorized access to {}: {}", request.getRequestURI(), authException == null ? "no exception" : authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");

        String message = authException == null
                ? "Authentication required. Please provide a valid JWT in the Authorization header."
                : authException.getMessage();
        if (message == null || message.isBlank()) {
            message = "Authentication required. Please provide a valid JWT in the Authorization header.";
        }

        try (PrintWriter out = response.getWriter()) {
            out.print("{\"error\":\"Unauthorized\",\"message\":\"" + escapeJson(message) + "\"}");
            out.flush();
        }
    }

    // Minimal JSON string escaper for messages
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
