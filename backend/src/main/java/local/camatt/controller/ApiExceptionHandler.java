package local.camatt.controller;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> badRequest(Exception e) { return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, readable(e))); }
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<ApiError> notFound(Exception e) { return ResponseEntity.status(404).body(new ApiError(Instant.now(), 404, e.getMessage())); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<ApiError> tooLarge() { return ResponseEntity.status(413).body(new ApiError(Instant.now(), 413, "Photos are too large")); }
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<ApiError> unavailable(Exception e) { return ResponseEntity.status(503).body(new ApiError(Instant.now(), 503, e.getMessage())); }
    private String readable(Exception e) { if (e instanceof MethodArgumentNotValidException validation && validation.getBindingResult().getFieldError() != null) return validation.getBindingResult().getFieldError().getDefaultMessage(); return e.getMessage(); }
    record ApiError(Instant timestamp, int status, String message) {}
}
