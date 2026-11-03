package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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
  void mapsUnreadableMessageWithoutParserDetails() {
    var problem = handler.handleUnreadableMessage(null);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).doesNotContain("Json").doesNotContain("parser");
  }
}
