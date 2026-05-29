package museon_online.astor_butler.api.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(error(
                exception.code(),
                exception.getMessage(),
                exception.details(),
                request
        ));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(
                ErrorCode.BAD_REQUEST,
                "Request payload or parameters are invalid",
                Map.of("reason", exception.getMessage()),
                request
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error(
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed",
                Map.of("errors", exception.getBindingResult().getFieldErrors()),
                request
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(
                ErrorCode.INTERNAL_ERROR,
                "Unexpected server error",
                Map.of("exception", exception.getClass().getSimpleName()),
                request
        ));
    }

    private ApiErrorResponse error(ErrorCode code, String message, Map<String, Object> details, HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return new ApiErrorResponse(code.name(), message, traceId, details, Instant.now());
    }
}
