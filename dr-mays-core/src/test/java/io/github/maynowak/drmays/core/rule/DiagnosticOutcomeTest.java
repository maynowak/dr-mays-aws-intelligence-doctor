package io.github.maynowak.drmays.core.rule;

import static io.github.maynowak.drmays.core.TestFixtures.NETWORK_ID;
import static io.github.maynowak.drmays.core.TestFixtures.finding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class DiagnosticOutcomeTest {

    private static final RuleId RULE_ID = new RuleId("NET-PUBLIC-001");

    @Test
    void findingDetectedDerivesRuleAndResource() {
        var detected = new DiagnosticOutcome.FindingDetected(finding(NETWORK_ID, RULE_ID));

        assertThat(detected.ruleId()).isEqualTo(RULE_ID);
        assertThat(detected.resource()).isEqualTo(NETWORK_ID);
        assertThat(detected.summary()).isEqualTo("FINDING NET-PUBLIC-001 @ " + NETWORK_ID.canonicalId());
    }

    @Test
    void noFindingExposesSummary() {
        var noFinding = new DiagnosticOutcome.NoFinding(RULE_ID, NETWORK_ID, "no issue");

        assertThat(noFinding.ruleId()).isEqualTo(RULE_ID);
        assertThat(noFinding.resource()).isEqualTo(NETWORK_ID);
        assertThat(noFinding.reason()).isEqualTo("no issue");
        assertThat(noFinding.summary()).isEqualTo("NO_FINDING NET-PUBLIC-001 @ " + NETWORK_ID.canonicalId());
    }

    @Test
    void notApplicableExposesSummary() {
        var notApplicable = new DiagnosticOutcome.NotApplicable(RULE_ID, NETWORK_ID, "wrong type");

        assertThat(notApplicable.summary())
                .isEqualTo("NOT_APPLICABLE NET-PUBLIC-001 @ " + NETWORK_ID.canonicalId());
    }

    @Test
    void rejectsBlankReasons() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiagnosticOutcome.NoFinding(RULE_ID, NETWORK_ID, " "))
                .withMessage("reason must not be blank");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiagnosticOutcome.NotApplicable(RULE_ID, NETWORK_ID, ""))
                .withMessage("reason must not be blank");
    }

    @Test
    void rejectsNullFinding() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DiagnosticOutcome.FindingDetected(null))
                .withMessage("finding");
    }
}
