package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvidenceResponseTest {

  @Test
  void treatsNullDetailsAsEmpty() {
    var response = new EvidenceResponse("source", "observed", "expected", null);

    assertThat(response.details()).isEmpty();
  }
}
