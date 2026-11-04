package io.github.maynowak.drmays.ai.expertise;

import static io.github.maynowak.drmays.ai.expertise.ExpertiseTestFixtures.SG;
import static io.github.maynowak.drmays.ai.expertise.ExpertiseTestFixtures.partialRequest;
import static io.github.maynowak.drmays.ai.expertise.ExpertiseTestFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.ai.privacy.ApprovalStatus;
import io.github.maynowak.drmays.ai.privacy.PrivacyGate;
import io.github.maynowak.drmays.ai.privacy.PrivacyPolicy;
import io.github.maynowak.drmays.ai.privacy.PrivacyProcessor;
import io.github.maynowak.drmays.ai.provider.ModelInvocationRequest;
import io.github.maynowak.drmays.ai.provider.ModelInvocationResult;
import io.github.maynowak.drmays.ai.provider.ModelProvider;
import io.github.maynowak.drmays.ai.provider.ModelProviderDescriptor;
import io.github.maynowak.drmays.ai.provider.ProviderEnablement;
import io.github.maynowak.drmays.ai.provider.ProviderExecutionStatus;
import io.github.maynowak.drmays.ai.provider.ProviderModelPolicy;
import io.github.maynowak.drmays.ai.provider.ProviderRegistry;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PrivacyFirstExpertiseEngineTest {

  private final PrivacyFirstExpertiseEngine engine = new PrivacyFirstExpertiseEngine();

  @Test
  void previewContainsOnlyAliasesAndNoProviderCall() {
    var provider =
        new TestProvider(request ->
            ModelInvocationResult.success(
                "mock", "deterministic-v1", validJson(request), request.payloadJson().length()));
    var custom = engine(provider);

    var preview = custom.preview(request(), "fake", "m", "standard");

    assertThat(preview.payloadJson())
        .doesNotContain("123456789012")
        .doesNotContain("sg-0abc1234")
        .doesNotContain("vpc-0def5678");
    assertThat(preview.approvalRequired()).isTrue();
    assertThat(provider.calls()).isZero();
  }

  @Test
  void explainsMockPreviewWithVerifiedResourceAndEvidence() {
    var preview = engine.preview(request(), "mock", "deterministic-v1", "standard");

    var report = engine.explain(preview.previewId(), null);

    assertThat(report.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(report.explanations()).hasSize(1);
    assertThat(report.explanations().getFirst().resource()).isEqualTo(SG);
    assertThat(report.explanations().getFirst().evidence()).hasSize(1);
    assertThat(report.facts()).anyMatch(fact -> fact.contains("NET-PUBLIC-001"));
    assertThat(report.privacyReceipt().approvalStatus()).isEqualTo(ApprovalStatus.NOT_REQUIRED);
  }

  @Test
  void approvalTokenWorksForMockPreview() {
    var preview = engine.preview(request(), "mock", "deterministic-v1", "standard");
    var token = engine.approve(preview.previewId()).orElseThrow();

    assertThat(engine.explain(preview.previewId(), token).status())
        .isEqualTo(ProviderExecutionStatus.SUCCESS);
  }

  @Test
  void secondExecutionIsRejected() {
    var preview = engine.preview(request(), "mock", "deterministic-v1", "standard");
    engine.explain(preview.previewId(), null);

    assertThat(engine.explain(preview.previewId(), null).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
  }

  @Test
  void unknownPreviewIsRejected() {
    assertThat(engine.explain("unknown", null).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
  }

  @Test
  void rejectsUnknownProvider() {
    assertThatThrownBy(() -> engine.preview(request(), "unknown", "m", "standard"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsModelNotInAllowlistBeforePreviewRegistration() {
    assertThatThrownBy(() -> engine.preview(request(), "mock", "gpt-4o", "standard"))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(engine.preview(request(), "mock", "deterministic-v1", "standard")).isNotNull();
  }

  @Test
  void rejectsUnknownExpertise() {
    assertThatThrownBy(() -> engine.preview(request(), "mock", "m", "unknown"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void preservesPartialDiscoveryUncertainty() {
    var preview = engine.preview(partialRequest(), "mock", "deterministic-v1", "standard");

    var report = engine.explain(preview.previewId(), null);

    assertThat(report.uncertainties()).anyMatch(value -> value.contains("partial"));
  }

  @Test
  void providerFailurePreservesFacts() {
    var provider =
        new TestProvider(request ->
            ModelInvocationResult.failure(
                ProviderExecutionStatus.UNAVAILABLE, "mock", "deterministic-v1", "TEST"));
    var custom = engine(provider);
    var preview = custom.preview(request(), "fake", "m", "standard");
    var token = custom.approve(preview.previewId()).orElseThrow();

    var report = custom.explain(preview.previewId(), token);

    assertThat(report.status()).isEqualTo(ProviderExecutionStatus.UNAVAILABLE);
    assertThat(report.facts()).isNotEmpty();
  }

  @Test
  void malformedProviderOutputIsInvalidResponse() {
    var provider =
        new TestProvider(request ->
            ModelInvocationResult.success(
                "mock", "deterministic-v1", "not json", request.payloadJson().length()));
    var custom = engine(provider);
    var preview = custom.preview(request(), "fake", "m", "standard");
    var token = custom.approve(preview.previewId()).orElseThrow();

    assertThat(custom.explain(preview.previewId(), token).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void hallucinatedResourceAliasIsRejected() {
    var provider =
        new TestProvider(request ->
            ModelInvocationResult.success(
                "mock",
                "deterministic-v1",
                "{\"summary\":\"ok\",\"explanations\":[{\"ruleId\":\"NET-PUBLIC-001\","
                    + "\"resourceAlias\":\"res_unknown\",\"explanation\":\"ok\","
                    + "\"evidence\":[{\"source\":\"resource-attribute\"}]}],"
                    + "\"recommendations\":[],\"assumptions\":[],"
                    + "\"missingInformation\":[],\"uncertainties\":[]}",
                request.payloadJson().length()));
    var custom = engine(provider);
    var preview = custom.preview(request(), "fake", "m", "standard");
    var token = custom.approve(preview.previewId()).orElseThrow();

    assertThat(custom.explain(preview.previewId(), token).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  private static PrivacyFirstExpertiseEngine engine(ModelProvider provider) {
    var registry = new ProviderRegistry();
    registry.register(provider);
    return new PrivacyFirstExpertiseEngine(
        registry,
        new ExpertiseCatalog(),
        new PrivacyProcessor(PrivacyPolicy.defaultPolicy()),
        new PrivacyGate(),
        new ExpertiseParser(),
        new ExpertiseValidator(),
        ProviderModelPolicy.of(Map.of("fake", Set.of("m"))),
        ProviderEnablement.of(registry, Set.of("fake")));
  }

  private static String validJson(ModelInvocationRequest request) {
    var finding = request.payload().findings().getFirst();
    return "{\"summary\":\"ok\",\"explanations\":[{\"ruleId\":\""
        + finding.ruleId()
        + "\",\"resourceAlias\":\""
        + finding.resourceAlias()
        + "\",\"explanation\":\"The finding is HIGH.\",\"evidence\":[{\"source\":\""
        + finding.evidenceSource()
        + "\"}]}],\"recommendations\":[{\"title\":\"Review\","
        + "\"action\":\"Apply the deterministic remediation.\",\"severity\":\"HIGH\"}],"
        + "\"assumptions\":[],\"missingInformation\":[],\"uncertainties\":[]}";
  }

  private static final class TestProvider implements ModelProvider {

    private final Function<ModelInvocationRequest, ModelInvocationResult> response;
    private int calls;

    private TestProvider(Function<ModelInvocationRequest, ModelInvocationResult> response) {
      this.response = response;
    }

    @Override
    public ModelProviderDescriptor descriptor() {
      return new ModelProviderDescriptor(
          "fake",
          "Fake provider",
          io.github.maynowak.drmays.ai.provider.ProviderKind.EXTERNAL_MODEL,
          new io.github.maynowak.drmays.ai.provider.ProviderCapabilities(
              true, false, false, 32_000, 64_000),
          false,
          true,
          false);
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
      calls++;
      return response.apply(request);
    }

    private int calls() {
      return calls;
    }
  }
}
