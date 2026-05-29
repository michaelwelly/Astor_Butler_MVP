package museon_online.astor_butler.api.common;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;
    private final Map<String, Object> details;

    public ApiException(HttpStatus status, ErrorCode code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
