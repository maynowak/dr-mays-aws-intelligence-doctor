package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CredentialReferenceTest {

  private final CredentialReferenceValidator validator = new CredentialReferenceValidator();

  @Test
  void noCredentialIsValidForMock() {
    var reference = ProviderCredentialReference.none("mock");

    var result = validator.validate(reference, ModelProviderDescriptor.mock());

    assertThat(result.status()).isEqualTo(CredentialValidationStatus.NOT_REQUIRED);
    assertThat(result.allowed()).isTrue();
  }

  @Test
  void credentialIsInvalidForMock() {
    var reference = ProviderCredentialReference.environment("mock", "DR_MAYS_MOCK_KEY");

    assertThat(validator.validate(reference, ModelProviderDescriptor.mock()).allowed()).isFalse();
  }

  @Test
  void remoteProviderRequiresReference() {
    assertThat(validator.validate(null, ModelProviderDescriptor.openCode()).allowed()).isFalse();
    assertThat(
            validator
                .validate(
                    ProviderCredentialReference.none("opencode"),
                    ModelProviderDescriptor.openCode())
                .allowed())
        .isFalse();
  }

  @Test
  void structurallyValidEnvironmentReferenceIsAccepted() {
    var reference = ProviderCredentialReference.environment("opencode", "DR_MAYS_OPENCODE_KEY");

    assertThat(validator.validate(reference, ModelProviderDescriptor.openCode()).allowed())
        .isTrue();
  }

  @Test
  void referenceFromAnotherProviderIsRejected() {
    var reference = ProviderCredentialReference.environment("other", "DR_MAYS_OPENCODE_KEY");

    assertThat(validator.validate(reference, ModelProviderDescriptor.openCode()).allowed())
        .isFalse();
  }

  @Test
  void rejectsUnsafeEnvironmentName() {
    assertThatThrownBy(() -> ProviderCredentialReference.environment("opencode", "OPENAI_API_KEY"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidProviderId() {
    assertThatThrownBy(() -> ProviderCredentialReference.none("Invalid"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
