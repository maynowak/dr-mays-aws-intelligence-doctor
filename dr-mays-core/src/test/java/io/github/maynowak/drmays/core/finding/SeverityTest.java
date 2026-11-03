package io.github.maynowak.drmays.core.finding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class SeverityTest {

    @Test
    void ranksSeveritiesFromInfoToCritical() {
        assertThat(Severity.values())
                .extracting(Severity::rank)
                .containsExactly(10, 20, 30, 40, 50);
    }

    @Test
    void isAtLeastIncludesEqualAndHigherSeverities() {
        assertThat(Severity.HIGH.isAtLeast(Severity.MEDIUM)).isTrue();
        assertThat(Severity.HIGH.isAtLeast(Severity.HIGH)).isTrue();
        assertThat(Severity.LOW.isAtLeast(Severity.HIGH)).isFalse();
    }

    @Test
    void rejectsUnknownSeverityNames() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Severity.valueOf("UNKNOWN"));
    }

    @Test
    void rejectsNullThreshold() {
        assertThatNullPointerException()
                .isThrownBy(() -> Severity.INFO.isAtLeast(null))
                .withMessage("threshold");
    }
}
