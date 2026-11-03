package io.github.maynowak.drmays.core.rule;

import static io.github.maynowak.drmays.core.TestFixtures.NETWORK_ID;
import static io.github.maynowak.drmays.core.TestFixtures.STORAGE_ID;
import static io.github.maynowak.drmays.core.TestFixtures.context;
import static io.github.maynowak.drmays.core.TestFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosticContextTest {

  @Test
  void sortsResourcesByCanonicalIdentity() {
    var netTwo =
        resource(
            new io.github.maynowak.drmays.core.resource.ResourceIdentity(
                "aws", "123456789012", "eu-central-1", "network", "net-2"),
            Map.of(),
            Map.of());
    var netOne = resource(NETWORK_ID, Map.of(), Map.of());

    var context = context(netTwo, netOne);

    assertThat(context.resources())
        .extracting(resource -> resource.identity().canonicalId())
        .containsExactly(NETWORK_ID.canonicalId(), netTwo.identity().canonicalId());
  }

  @Test
  void rejectsDuplicateResourceIdentities() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            context(
                resource(NETWORK_ID, Map.of(), Map.of()),
                resource(NETWORK_ID, Map.of("owner", "other"), Map.of())))
        .withMessageContaining("duplicate resource identity");
  }

  @Test
  void exposesImmutableResourceList() {
    var context = context(resource(NETWORK_ID, Map.of(), Map.of()));

    assertThatThrownBy(() -> context.resources().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void findsResourceByExactIdentity() {
    var context =
        context(resource(NETWORK_ID, Map.of(), Map.of()), resource(STORAGE_ID, Map.of(), Map.of()));

    assertThat(context.find(STORAGE_ID)).isPresent();
    assertThat(
            context.find(
                io.github.maynowak.drmays.core.TestFixtures.identity("network", "unknown")))
        .isEmpty();
  }

  @Test
  void allowsEmptyContext() {
    assertThat(new DiagnosticContext(java.util.List.of()).resources()).isEmpty();
  }

  @Test
  void rejectsNullElementsAndNullList() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DiagnosticContext(null))
        .withMessage("resources");
    assertThatNullPointerException()
        .isThrownBy(
            () -> context((io.github.maynowak.drmays.core.resource.InfrastructureResource) null))
        .withMessage("resource");
  }
}
