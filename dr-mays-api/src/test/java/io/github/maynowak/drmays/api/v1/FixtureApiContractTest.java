package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class FixtureApiContractTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired private MockMvc mockMvc;

  @Test
  void healthyFixtureReturnsNoFindings() throws Exception {
    postFixture("healthy")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findingCount").value(0))
        .andExpect(jsonPath("$.findings").isEmpty());
  }

  @Test
  void healthyFixtureProducesNoFindingAndNotApplicableOutcomes() throws Exception {
    postFixture("healthy")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcomes.length()").value(2))
        .andExpect(jsonPath("$.outcomes[0].type").value("NOT_APPLICABLE"))
        .andExpect(jsonPath("$.outcomes[1].type").value("NO_FINDING"));
  }

  @Test
  void missingOwnerFixtureTriggersTagRule() throws Exception {
    postFixture("missing-owner")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findingCount").value(1))
        .andExpect(jsonPath("$.findings[0].ruleId").value("TAG-OWNER-001"));
  }

  @Test
  void missingOwnerEvidenceShowsAbsentOwner() throws Exception {
    postFixture("missing-owner")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findings[0].evidence.observed").value("absent"))
        .andExpect(jsonPath("$.findings[0].evidence.expected").value("non-blank owner tag"));
  }

  @Test
  void publicIngressFixtureTriggersNetworkRule() throws Exception {
    postFixture("public-ingress")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findingCount").value(1))
        .andExpect(jsonPath("$.findings[0].ruleId").value("NET-PUBLIC-001"));
  }

  @Test
  void publicIngressEvidenceShowsObservedAndExpectedValues() throws Exception {
    postFixture("public-ingress")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findings[0].evidence.observed").value("ingress.public=true"))
        .andExpect(jsonPath("$.findings[0].evidence.expected").value("ingress.public=false"));
  }

  @Test
  void multipleFindingsFixtureReturnsBothRules() throws Exception {
    postFixture("multiple-findings")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findingCount").value(2))
        .andExpect(jsonPath("$.findings[0].ruleId").value("NET-PUBLIC-001"))
        .andExpect(jsonPath("$.findings[1].ruleId").value("TAG-OWNER-001"));
  }

  @Test
  void multipleFindingsFixtureProducesFourOutcomes() throws Exception {
    postFixture("multiple-findings")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcomes.length()").value(4));
  }

  @Test
  void multipleFindingsFixtureIsDeterministic() throws Exception {
    var first = postFixture("multiple-findings").andReturn().getResponse().getContentAsString();
    var second = postFixture("multiple-findings").andReturn().getResponse().getContentAsString();

    assertThat(first).isEqualTo(second);
  }

  @Test
  void invalidFixtureReturnsValidationProblem() throws Exception {
    postFixture("invalid-input")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  @Test
  void allFixturePayloadsAreValidJsonWithResourcesArrays() throws IOException {
    for (String name :
        new String[] {
          "healthy", "missing-owner", "public-ingress", "multiple-findings", "invalid-input"
        }) {
      var node = OBJECT_MAPPER.readTree(fixture(name));
      assertThat(node.get("resources").isArray()).as(name).isTrue();
    }
  }

  @Test
  void fixturesUseSyntheticAccountIdentifiers() throws IOException {
    for (String name :
        new String[] {"healthy", "missing-owner", "public-ingress", "multiple-findings"}) {
      assertThat(fixture(name)).as(name).contains("\"accountId\": \"000000000000\"");
      assertThat(fixture(name)).as(name).doesNotContain("AKIA");
    }
  }

  private ResultActions postFixture(String name) throws Exception {
    return mockMvc.perform(
        post("/api/v1/diagnoses").contentType(MediaType.APPLICATION_JSON).content(fixture(name)));
  }

  private String fixture(String name) throws IOException {
    try (var stream = getClass().getResourceAsStream("/fixtures/" + name + ".json")) {
      if (stream == null) {
        throw new IOException("missing fixture: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
