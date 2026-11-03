package io.github.maynowak.drmays.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ResourceIdentityTest {

    @Test
    void exposesCanonicalIdentity() {
        var identity = new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-1");

        assertThat(identity.canonicalId()).isEqualTo("aws|123456789012|eu-central-1|network|net-1");
    }

    @Test
    void usesRecordEquality() {
        var first = new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-1");
        var second = new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-1");

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void rejectsBlankFields() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ResourceIdentity(" ", "account", "region", "type", "id"))
                .withMessage("provider must not be blank");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ResourceIdentity("aws", "", "region", "type", "id"))
                .withMessage("accountId must not be blank");
    }

    @Test
    void rejectsNullFields() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ResourceIdentity(null, "account", "region", "type", "id"))
                .withMessage("provider");
        assertThatNullPointerException()
                .isThrownBy(() -> new ResourceIdentity("aws", null, "region", "type", "id"))
                .withMessage("accountId");
    }
}
