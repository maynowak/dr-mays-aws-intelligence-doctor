package io.github.maynowak.drmays.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InfrastructureResourceTest {

  private static final ResourceIdentity ID =
      new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-1");

  @Test
  void exposesTagsAndAttributesImmutably() {
    var tags = new LinkedHashMap<String, String>();
    tags.put("owner", "platform");
    var resource = new InfrastructureResource(ID, tags, Map.of("ingress.public", "true"));

    tags.put("environment", "prod");

    assertThat(resource.tags()).containsExactly(Map.entry("owner", "platform"));
    assertThat(resource.tag("owner")).contains("platform");
    assertThat(resource.attribute("ingress.public")).contains("true");
    assertThatThrownBy(() -> resource.tags().put("x", "y"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> resource.attributes().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void returnsEmptyForMissingKeys() {
    var resource = new InfrastructureResource(ID, Map.of(), Map.of());

    assertThat(resource.tag("owner")).isEmpty();
    assertThat(resource.attribute("ingress.public")).isEmpty();
  }

  @Test
  void rejectsBlankLookupKeys() {
    var resource = new InfrastructureResource(ID, Map.of(), Map.of());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> resource.tag(" "))
        .withMessage("key must not be blank");
  }

  @Test
  void rejectsInvalidMaps() {
    var nullValue = new LinkedHashMap<String, String>();
    nullValue.put("key", null);

    assertThatNullPointerException()
        .isThrownBy(() -> new InfrastructureResource(ID, null, Map.of()))
        .withMessage("tags");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new InfrastructureResource(ID, Map.of(" ", "value"), Map.of()))
        .withMessage("tags key must not be blank");
    assertThatNullPointerException()
        .isThrownBy(() -> new InfrastructureResource(ID, Map.of("owner", "x"), nullValue))
        .withMessage("attributes value for key");
  }
}
