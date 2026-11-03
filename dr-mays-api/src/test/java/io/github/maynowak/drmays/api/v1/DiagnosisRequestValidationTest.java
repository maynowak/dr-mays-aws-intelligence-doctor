package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiagnosisRequestValidationTest {

  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void acceptsAValidRequest() {
    assertThat(validator.validate(validRequest())).isEmpty();
  }

  @Test
  void rejectsMissingOrEmptyResources() {
    assertThat(validator.validate(new DiagnosisRequest(null))).isNotEmpty();
    assertThat(validator.validate(new DiagnosisRequest(List.of()))).isNotEmpty();
  }

  @Test
  void rejectsMoreThanOneHundredResources() {
    var resources = new ArrayList<ResourceRequest>();
    for (int i = 0; i < 101; i++) {
      resources.add(validResource());
    }

    assertThat(validator.validate(new DiagnosisRequest(resources)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("resources");
  }

  @Test
  void rejectsBlankIdentityFields() {
    var identity = new ResourceIdentityRequest(" ", "account", "region", "network", "net-1");

    assertThat(validator.validate(new ResourceRequest(identity, Map.of(), Map.of()))).isNotEmpty();
  }

  @Test
  void rejectsOversizedIdentityFields() {
    var identity =
        new ResourceIdentityRequest("a".repeat(65), "account", "region", "network", "net-1");

    assertThat(validator.validate(new ResourceRequest(identity, Map.of(), Map.of()))).isNotEmpty();
  }

  @Test
  void rejectsTooManyTagsOrAttributes() {
    var tags = new LinkedHashMap<String, String>();
    var attributes = new LinkedHashMap<String, String>();
    for (int i = 0; i < 51; i++) {
      tags.put("tag-" + i, "value");
      attributes.put("attribute-" + i, "value");
    }

    assertThat(validator.validate(new ResourceRequest(validIdentity(), tags, attributes)))
        .isNotEmpty();
  }

  @Test
  void rejectsOversizedTagValue() {
    var resource =
        new ResourceRequest(validIdentity(), Map.of("owner", "x".repeat(1025)), Map.of());

    assertThat(validator.validate(resource)).isNotEmpty();
  }

  @Test
  void treatsMissingMapsAsEmpty() {
    var resource = new ResourceRequest(validIdentity(), null, null);

    assertThat(validator.validate(resource)).isEmpty();
    assertThat(resource.tags()).isEmpty();
    assertThat(resource.attributes()).isEmpty();
  }

  private static DiagnosisRequest validRequest() {
    return new DiagnosisRequest(List.of(validResource()));
  }

  private static ResourceRequest validResource() {
    return new ResourceRequest(validIdentity(), Map.of("owner", "platform"), Map.of());
  }

  private static ResourceIdentityRequest validIdentity() {
    return new ResourceIdentityRequest("aws", "123456789012", "eu-central-1", "network", "net-1");
  }
}
