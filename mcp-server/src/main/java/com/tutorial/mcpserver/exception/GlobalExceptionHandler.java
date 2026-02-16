package com.tutorial.mcpserver.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Global exception handler + custom error controller.
 * Prevents "Missing result context" errors from async MCP SSE dispatch.
 */
@RestControllerAdvice
@RestController
public class GlobalExceptionHandler implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleMissingResultContext(IllegalStateException ex) {
        // Silently ignore "Missing result context" from async MCP SSE dispatch.
        // This is expected when MCP clients close SSE connections.
        if ("Missing result context".equals(ex.getMessage())) {
            log.trace("SSE connection closed by client (expected): {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        log.warn("Unhandled IllegalStateException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // Skip logging for SSE-related converter errors
        if (ex.getMessage() != null && ex.getMessage().contains("text/event-stream")) {
            log.trace("SSE converter error (expected): {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", ex.getMessage()));
        }
        log.warn("Unhandled exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", ex.getMessage()));
    }

    /**
     * Custom error controller to handle Tomcat error dispatch gracefully.
     * Prevents "Missing result context" when async MCP requests fail.
     */
    @RequestMapping("/error")
    public ResponseEntity<Map<String, String>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");

        int status = statusCode != null ? statusCode : 500;
        String msg = message != null && !message.isEmpty() ? message : "Unknown error";

        return ResponseEntity.status(status)
                .body(Map.of("error", HttpStatus.valueOf(status).getReasonPhrase(), "message", msg));
    }
}
