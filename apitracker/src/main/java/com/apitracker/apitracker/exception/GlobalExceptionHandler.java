package com.apitracker.apitracker.exception;

import com.apitracker.apitracker.dto.ErrorResponse;
import com.apitracker.apitracker.dto.ValidationErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ValidationErrorDetail(err.getField(), err.getDefaultMessage()))
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Erreur de validation des données fournies",
                request.getRequestURI(),
                details
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Corps de requête JSON invalide ou valeur énumérée incorrecte",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Paramètre invalide : " + ex.getName();
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Erreur interne non gérée sur {}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue",
                request.getRequestURI(),
                List.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<ValidationErrorDetail> validationErrors
    ) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
