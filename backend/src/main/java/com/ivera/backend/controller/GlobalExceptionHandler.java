package com.ivera.backend.controller;

import com.ivera.backend.exception.ReporteIncompletoException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String AUTORIZACION_ALMACEN_ACTIVA_CONSTRAINT = "uk_autorizacion_almacen_activa";
    private static final String AUTORIZACION_DUPLICADA_MESSAGE =
            "Ya existe una autorización activa para este trabajador, almacén y acción";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> error = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> 
            error.put(err.getField(), err.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> error = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                error.put("error", violation.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ReporteIncompletoException.class)
    public ResponseEntity<Map<String, String>> handleReporteIncompleto(ReporteIncompletoException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        if (esAutorizacionAlmacenActivaDuplicada(ex)) {
            error.put("error", AUTORIZACION_DUPLICADA_MESSAGE);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        error.put("error", "No se pudo completar la operación.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({PessimisticLockException.class, LockTimeoutException.class,
            PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<Map<String, String>> handleLockConflict(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "La operación no pudo completarse por una actualización concurrente. Inténtelo nuevamente.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    private boolean esAutorizacionAlmacenActivaDuplicada(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof java.sql.SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().contains(AUTORIZACION_ALMACEN_ACTIVA_CONSTRAINT)) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
