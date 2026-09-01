package com.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas HTTP con el código correcto.
 * Sin esto, cualquier excepción de un controller/servicio se re-despacha a /error
 * (endpoint protegido) y el cliente recibe siempre 401, tapando el error real.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Token válido pero rol insuficiente (lo lanza @PreAuthorize) -> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta operación");
    }

    // Fallos de autenticación -> 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    // Reglas de negocio (email/DNI duplicado, entidad inexistente, etc.) -> 400
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", mensaje != null ? mensaje : ""
        );
        return ResponseEntity.status(status).body(body);
    }
}
