package io.github.maynowak.drmays.core.diagnosis;

import static io.github.maynowak.drmays.core.TestFixtures.NETWORK_ID;
import static io.github.maynowak.drmays.core.TestFixtures.STORAGE_ID;
import static io.github.maynowak.drmays.core.TestFixtures.context;
import static io.github.maynowak.drmays.core.TestFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.DiagnosticRule;
import io.github.maynowak.drmays.core.rule.RuleId;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosisEngineTest {

    private final DiagnosisEngine engine =
            new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule()));

    @Test
    void runsRulesAndProducesDeterministicFindings() {
        var network = resource(NETWORK_ID, Map.of(), Map.of("ingress.public", "true"));
        var storage = resource(STORAGE_ID, Map.of("owner", "platform"), Map.of());

        var result = engine.diagnose(context(network, storage));

        assertThat(result.outcomes())
                .extracting(DiagnosticOutcome::summary)
                .containsExactly(
                        "FINDING NET-PUBLIC-001 @ " + NETWORK_ID.canonicalId(),
                        "FINDING TAG-OWNER-001 @ " + NETWORK_ID.canonicalId(),
                        "NOT_APPLICABLE NET-PUBLIC-001 @ " + STORAGE_ID.canonicalId(),
                        "NO_FINDING TAG-OWNER-001 @ " + STORAGE_ID.canonicalId());
        assertThat(result.findings())
                .extracting(finding -> finding.ruleId().value())
                .containsExactly("NET-PUBLIC-001", "TAG-OWNER-001");
        assertThat(result.findingCount()).isEqualTo(2);
    }

    @Test
    void isIndependentOfResourceInputOrder() {
        var network = resource(NETWORK_ID, Map.of(), Map.of("ingress.public", "true"));
        var storage = resource(STORAGE_ID, Map.of("owner", "platform"), Map.of());

        var first = engine.diagnose(context(network, storage));
        var second = engine.diagnose(context(storage, network));

        assertThat(first.outcomes()).extracting(DiagnosticOutcome::summary)
                .containsExactlyElementsOf(second.outcomes().stream().map(DiagnosticOutcome::summary).toList());
    }

    @Test
    void handlesEmptyResourcesAndEmptyRuleSets() {
        var noResources = engine.diagnose(context());
        var noRules = new DiagnosisEngine(List.of()).diagnose(context(
                resource(NETWORK_ID, Map.of(), Map.of())));

        assertThat(noResources.outcomes()).isEmpty();
        assertThat(noResources.hasFindings()).isFalse();
        assertThat(noRules.outcomes()).isEmpty();
    }

    @Test
    void exposesRulesInIdOrderImmutably() {
        var rules = new DiagnosisEngine(List.of(new PublicIngressRule(), new MissingOwnerTagRule())).rules();

        assertThat(rules).extracting(rule -> rule.id().value())
                .containsExactly("NET-PUBLIC-001", "TAG-OWNER-001");
        assertThatThrownBy(() -> rules.clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateRuleIds() {
        var rule = new StubRule(new RuleId("DUP-001"), "duplicate", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiagnosisEngine(List.of(rule, rule)))
                .withMessage("duplicate rule id: DUP-001");
    }

    @Test
    void rejectsInvalidRuleMetadata() {
        var blankDescription = new StubRule(new RuleId("META-001"), " ", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiagnosisEngine(List.of(blankDescription)))
                .withMessage("rule.description must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new DiagnosisEngine(java.util.Arrays.asList((DiagnosticRule) null)))
                .withMessage("rule");
    }

    @Test
    void rejectsNullOutcomeFromRule() {
        var rule = new StubRule(new RuleId("NULL-001"), "null outcome", null);

        assertThatNullPointerException()
                .isThrownBy(() -> new DiagnosisEngine(List.of(rule)).diagnose(
                        context(resource(NETWORK_ID, Map.of(), Map.of()))))
                .withMessage("rule returned null outcome: NULL-001");
    }

    @Test
    void rejectsOutcomeForAnotherRule() {
        var rule = new StubRule(
                new RuleId("WRONG-001"),
                "wrong rule",
                new DiagnosticOutcome.NoFinding(new RuleId("OTHER-001"), NETWORK_ID, "none"));

        assertThatIllegalStateException()
                .isThrownBy(() -> new DiagnosisEngine(List.of(rule)).diagnose(
                        context(resource(NETWORK_ID, Map.of(), Map.of()))))
                .withMessageContaining("returned outcome for OTHER-001");
    }

    @Test
    void rejectsOutcomeForAnotherResource() {
        var rule = new StubRule(
                new RuleId("WRONG-002"),
                "wrong resource",
                new DiagnosticOutcome.NoFinding(new RuleId("WRONG-002"), STORAGE_ID, "none"));

        assertThatIllegalStateException()
                .isThrownBy(() -> new DiagnosisEngine(List.of(rule)).diagnose(
                        context(resource(NETWORK_ID, Map.of(), Map.of()))))
                .withMessageContaining("returned outcome for " + STORAGE_ID.canonicalId());
    }

    @Test
    void rejectsNullContext() {
        assertThatNullPointerException()
                .isThrownBy(() -> engine.diagnose(null))
                .withMessage("context");
    }

    private static final class StubRule implements DiagnosticRule {

        private final RuleId id;
        private final String description;
        private final DiagnosticOutcome outcome;

        private StubRule(RuleId id, String description, DiagnosticOutcome outcome) {
            this.id = id;
            this.description = description;
            this.outcome = outcome;
        }

        @Override
        public RuleId id() {
            return id;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public DiagnosticOutcome evaluate(InfrastructureResource resource, DiagnosticContext context) {
            return outcome;
        }
    }
}
