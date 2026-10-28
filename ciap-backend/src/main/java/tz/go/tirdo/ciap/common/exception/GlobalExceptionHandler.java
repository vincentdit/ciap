package tz.go.tirdo.ciap.common.exception;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates exceptions into a consistent JSON error contract.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(
            int status,
            String error,
            String message,
            String path,
            Instant timestamp,
            Map<String, String> fieldErrors
    ) {}

    private ErrorResponse build(HttpStatus status, String message, WebRequest request, Map<String, String> fieldErrors) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", ""),
                Instant.now(),
                fieldErrors
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null));
    }

    @ExceptionHandler({BusinessException.class})
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, null));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(RuntimeException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT,
                        "This record was updated by someone else. Reload and try again.", req, null));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT, ex.getMessage(), req, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, "Validation failed", req, fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", req, null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(HttpStatus.UNAUTHORIZED, "Authentication failed", req, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req, null));
    }
}
