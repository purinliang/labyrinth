package dev.labyrinth;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Converts service rejections, validation failures, and persistence conflicts
 * into JSON errors while preserving their HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String message) {
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleBusinessError(
            ResponseStatusException exception) {
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = "Request failed";
        }

        return ResponseEntity.status(exception.getStatusCode())
                .headers(exception.getHeaders())
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid argument"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(
            OptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Version conflict"));
    }
}
