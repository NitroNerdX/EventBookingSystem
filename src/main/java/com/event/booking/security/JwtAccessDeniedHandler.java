package com.event.booking.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Sends a 403 JSON response for access-denied situations.
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String message = accessDeniedException == null
                ? "Access denied. You do not have the required role or authority to perform this action."
                : accessDeniedException.getMessage();
        log.warn("Access denied to {}: {}", request.getRequestURI(), message);
        if (message == null || message.isBlank()) {
            message = "Access denied. You do not have the required role or authority to perform this action.";
        }

        if ("Access Denied".equalsIgnoreCase(message) || "Access denied".equalsIgnoreCase(message)) {
            message = "Access denied. You do not have the required role or authority to perform this action.";
        }

        try (PrintWriter out = response.getWriter()) {
            out.print("{\"error\":\"Forbidden\",\"message\":\"" + escapeJson(message) + "\"}");
            out.flush();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
