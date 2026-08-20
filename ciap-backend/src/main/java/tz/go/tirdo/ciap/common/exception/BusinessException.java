package tz.go.tirdo.ciap.common.exception;

/**
 * Thrown when a domain/business rule is violated (e.g. illegal workflow transition).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
