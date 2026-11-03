package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class DiagnosisResponseJsonTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializesFindingsAndOutcomesWithWireNames() throws Exception {
    var identity = new ResourceResponse("aws", "123456789012", "eu-central-1", "network", "net-1");
    var evidence = new EvidenceResponse("resource-attribute", "true", "false", Map.of("a", "b"));
    var finding =
        new FindingResponse(
            "NET-PUBLIC-001",
            "1.0",
            "HIGH",
            identity,
            "Public ingress",
            "The resource declares public ingress.",
            evidence,
            "Restrict ingress.",
            List.of());
    var response =
        new DiagnosisResponse(
            List.of(finding),
            List.of(new OutcomeResponse("NET-PUBLIC-001", identity, "FINDING", null)),
            1);

    var json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"findingCount\":1");
    assertThat(json).contains("\"ruleId\":\"NET-PUBLIC-001\"");
    assertThat(json).contains("\"severity\":\"HIGH\"");
    assertThat(json).contains("\"type\":\"FINDING\"");
    assertThat(json).doesNotContain("\"reason\"");
  }

  @Test
  void serializesRulesResponseAsArray() throws Exception {
    var response = new RulesResponse(List.of(new RuleResponse("TAG-OWNER-001", "1.0", "Owner")));

    assertThat(objectMapper.writeValueAsString(response))
        .isEqualTo(
            "{\"rules\":[{\"ruleId\":\"TAG-OWNER-001\",\"version\":\"1.0\",\"description\":\"Owner\"}]}");
  }
}
