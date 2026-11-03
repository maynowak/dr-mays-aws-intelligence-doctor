package io.github.maynowak.drmays.core.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvidenceTest {

    private static final ResourceIdentity ID =
            new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-1");

    @Test
    void copiesDetailsImmutably() {
        var details = new LinkedHashMap<String, String>();
        details.put("attribute", "ingress.public");
        var evidence = new Evidence(ID, "resource-attribute", "true", "false", details);

        details.put("later", "mutation");

        assertThat(evidence.details()).containsExactly(Map.entry("attribute", "ingress.public"));
        assertThatThrownBy(() -> evidence.details().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankTextFields() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Evidence(ID, " ", "observed", "expected", Map.of()))
                .withMessage("source must not be blank");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Evidence(ID, "source", "", "expected", Map.of()))
                .withMessage("observed must not be blank");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Evidence(ID, "source", "observed", " ", Map.of()))
                .withMessage("expected must not be blank");
    }

    @Test
    void rejectsNullResource() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Evidence(null, "source", "observed", "expected", Map.of()))
                .withMessage("resource");
    }
}
