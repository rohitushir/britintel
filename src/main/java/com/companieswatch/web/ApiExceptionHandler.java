package com.companieswatch.web;

import com.companieswatch.account.EmailAlreadyExistsException;
import com.companieswatch.companieshouse.rest.CompaniesHouseException;
import com.companieswatch.companieshouse.rest.CompanyNotFoundException;
import com.companieswatch.watchlist.AlreadyWatchingException;
import com.companieswatch.watchlist.WatchLimitExceededException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to clean JSON HTTP responses for the dashboard API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(Instant timestamp, int status, String error, String message) {
        static ApiError of(HttpStatus status, String message) {
            return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message);
        }
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ApiError> notFound(CompanyNotFoundException e) {
        return status(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, AlreadyWatchingException.class})
    public ResponseEntity<ApiError> conflict(RuntimeException e) {
        return status(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(WatchLimitExceededException.class)
    public ResponseEntity<ApiError> limit(WatchLimitExceededException e) {
        return status(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        return status(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Invalid request");
        return status(HttpStatus.BAD_REQUEST, message);
    }

    /** Upstream Companies House failure that isn't a 404 — surface as a 502. */
    @ExceptionHandler(CompaniesHouseException.class)
    public ResponseEntity<ApiError> upstream(CompaniesHouseException e) {
        return status(HttpStatus.BAD_GATEWAY, "Companies House lookup failed");
    }

    private ResponseEntity<ApiError> status(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, message));
    }
}
