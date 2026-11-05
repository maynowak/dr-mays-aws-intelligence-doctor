package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void hidesUnexpectedExceptionDetails() {
    var problem = handler.handleUnexpected(new RuntimeException("secret internal detail"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getTitle()).isEqualTo("Internal server error");
    assertThat(problem.getDetail())
        .doesNotContain("secret")
        .isEqualTo("An unexpected error occurred.");
  }

  @Test
  void mapsIllegalArgumentToGenericBadRequest() {
    var problem = handler.handleIllegalArgument(new IllegalArgumentException("secret field"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).doesNotContain("secret");
  }

  @Test
  void mapsReportLimitToPayloadTooLarge() {
    var problem = handler.handleReportLimit(new ReportLimitExceededException());

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    assertThat(problem.getTitle()).isEqualTo("Report too large");
    assertThat(problem.getDetail()).doesNotContain("stack");
  }

  @Test
  void mapsUnsupportedMediaType() {
    var problem =
        handler.handleUnsupportedMediaType(new HttpMediaTypeNotSupportedException("text/plain"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    assertThat(problem.getDetail()).contains(MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
  void mapsUnsupportedMethod() {
    var problem =
        handler.handleUnsupportedMethod(new HttpRequestMethodNotSupportedException("TRACE"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
  }

  @Test
  void mapsConstraintViolationsToFieldErrors() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var violations = validator.validate(new DiagnosisRequest(null));
    var problem = handler.handleConstraintViolation(new ConstraintViolationException(violations));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getProperties()).containsKey("errors");
  }

  @Test
  void mapsEmptyConstraintViolationsToGenericError() {
    var problem = handler.handleConstraintViolation(new ConstraintViolationException(Set.of()));

    assertThat(problem.getProperties())
        .containsEntry("errors", java.util.Map.of("request", "is invalid"));
  }

  @Test
  void sanitizesNullAndBlankConstraintMessages() {
    var nullViolation = violation(null, "nullField");
    var blankViolation = violation(" ", "blankField");
    var problem =
        handler.handleConstraintViolation(
            new ConstraintViolationException(Set.of(nullViolation, blankViolation)));

    assertThat(problem.getProperties())
        .extracting("errors")
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
        .containsEntry("nullField", "is invalid")
        .containsEntry("blankField", "is invalid");
  }

  @Test
  void mapsEmptyBodyFieldErrorsToGenericError() {
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    var problem =
        handler.handleMethodArgumentNotValid(
            new MethodArgumentNotValidException(null, bindingResult));

    assertThat(problem.getProperties()).containsKey("errors");
  }

  @Test
  void mapsUnreadableMessageWithoutParserDetails() {
    var problem = handler.handleUnreadableMessage(null);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).doesNotContain("Json").doesNotContain("parser");
  }

  private static ConstraintViolation<?> violation(String message, String field) {
    @SuppressWarnings("unchecked")
    var violation = (ConstraintViolation<?>) mock(ConstraintViolation.class);
    var path = mock(Path.class);
    when(path.toString()).thenReturn(field);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getPropertyPath()).thenReturn(path);
    return violation;
  }
}
