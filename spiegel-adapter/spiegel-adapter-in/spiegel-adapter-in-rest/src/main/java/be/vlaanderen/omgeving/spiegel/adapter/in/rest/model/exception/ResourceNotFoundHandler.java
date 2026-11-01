package be.vlaanderen.omgeving.spiegel.adapter.in.rest.model.exception;

import be.vlaanderen.omgeving.spiegel.model.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns {@link ResourceNotFoundException} into a {@code 404} problem response (RFC 9457).
 */
@RestControllerAdvice
public class ResourceNotFoundHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handle(ResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
