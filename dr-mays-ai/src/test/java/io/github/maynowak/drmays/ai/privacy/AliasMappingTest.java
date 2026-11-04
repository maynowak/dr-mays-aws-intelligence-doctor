package io.github.maynowak.drmays.ai.privacy;

import static io.github.maynowak.drmays.ai.privacy.PrivacyTestFixtures.SG;
import static io.github.maynowak.drmays.ai.privacy.PrivacyTestFixtures.VPC;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AliasMappingTest {

  @Test
  void aliasesSameResourceConsistently() {
    var mapping = AliasMapping.withKey(PrivacyTestFixtures.KEY);

    assertThat(mapping.aliasFor(SG)).isEqualTo(mapping.aliasFor(SG));
    assertThat(mapping.size()).isEqualTo(1);
  }

  @Test
  void aliasesDifferentResourcesDifferently() {
    var mapping = AliasMapping.withKey(PrivacyTestFixtures.KEY);

    assertThat(mapping.aliasFor(SG)).isNotEqualTo(mapping.aliasFor(VPC));
  }

  @Test
  void resolvesResourceAliasLocally() {
    var mapping = AliasMapping.withKey(PrivacyTestFixtures.KEY);
    var alias = mapping.aliasFor(SG);

    assertThat(mapping.resolveResource(alias)).contains(SG);
  }

  @Test
  void unknownAliasDoesNotResolve() {
    var mapping = AliasMapping.withKey(PrivacyTestFixtures.KEY);

    assertThat(mapping.resolveResource("res_unknown")).isEmpty();
  }

  @Test
  void aliasesNonResourceIdentifiersDeterministically() {
    var mapping = AliasMapping.withKey(PrivacyTestFixtures.KEY);

    assertThat(mapping.aliasFor("acct", "123456789012"))
        .isEqualTo(mapping.aliasFor("acct", "123456789012"))
        .startsWith("acct_");
  }

  @Test
  void redactsToString() {
    var mapping = AliasMapping.withKey(PrivacyTestFixtures.KEY);

    assertThat(mapping.toString()).isEqualTo("AliasMapping[redacted]");
  }

  @Test
  void rejectsShortKey() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> AliasMapping.withKey(new byte[8]))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
