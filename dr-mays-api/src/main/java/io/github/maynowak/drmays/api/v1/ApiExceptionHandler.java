package io.github.maynowak.drmays.api.v1;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps API failures to safe RFC 9457 problem responses without internal details. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /**
   * Handles bean-validation failures on request bodies.
   *
   * @param exception validation failure
   * @return a 400 problem with field errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
    var errors = new LinkedHashMap<String, String>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(
            error -> errors.putIfAbsent(error.getField(), safeMessage(error.getDefaultMessage())));
    exception
        .getBindingResult()
        .getGlobalErrors()
        .forEach(error ->
            errors.putIfAbsent(error.getObjectName(), safeMessage(error.getDefaultMessage())));
    if (errors.isEmpty()) {
      errors.put("request", "is invalid");
    }
    return badRequest("Request validation failed", errors);
  }

  /**
   * Handles constraint violations raised outside request-body binding.
   *
   * @param exception validation failure
   * @return a 400 problem with field errors
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
    var errors = new LinkedHashMap<String, String>();
    exception
        .getConstraintViolations()
        .forEach(violation ->
            errors.putIfAbsent(
                violation.getPropertyPath().toString(), safeMessage(violation.getMessage())));
    if (errors.isEmpty()) {
      errors.put("request", "is invalid");
    }
    return badRequest("Request validation failed", errors);
  }

  /**
   * Handles malformed or structurally unsupported JSON.
   *
   * @param exception parse failure
   * @return a 400 problem without parser internals
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
    var problem = problem(HttpStatus.BAD_REQUEST, "Invalid request body");
    problem.setDetail("The request body could not be parsed as a supported JSON payload.");
    return problem;
  }

  /**
   * Handles domain argument validation that reached the service layer.
   *
   * @param exception domain validation failure
   * @return a 400 problem without the raw exception message
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
    var problem = problem(HttpStatus.BAD_REQUEST, "Invalid request");
    problem.setDetail("The request contains an invalid value.");
    return problem;
  }

  /**
   * Handles unsupported content types.
   *
   * @param exception media-type failure
   * @return a 415 problem
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
    var problem = problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    problem.setDetail("Content-Type must be " + MediaType.APPLICATION_JSON_VALUE + ".");
    return problem;
  }

  /**
   * Handles unsupported HTTP methods.
   *
   * @param exception method failure
   * @return a 405 problem
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ProblemDetail handleUnsupportedMethod(HttpRequestMethodNotSupportedException exception) {
    var problem = problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    problem.setDetail("The HTTP method is not supported for this endpoint.");
    return problem;
  }

  /**
   * Handles unexpected failures without exposing stack traces or internal messages.
   *
   * @param exception unexpected failure
   * @return a generic 500 problem
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception exception) {
    LOGGER.error("Unhandled API error", exception);
    var problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    problem.setDetail("An unexpected error occurred.");
    return problem;
  }

  private static ProblemDetail badRequest(String title, Map<String, String> errors) {
    var problem = problem(HttpStatus.BAD_REQUEST, title);
    problem.setDetail("One or more request fields are invalid.");
    problem.setProperty("errors", errors);
    return problem;
  }

  private static ProblemDetail problem(HttpStatus status, String title) {
    var problem = ProblemDetail.forStatus(status);
    problem.setTitle(title);
    return problem;
  }

  private static String safeMessage(String message) {
    return message == null || message.isBlank() ? "is invalid" : message;
  }
}
