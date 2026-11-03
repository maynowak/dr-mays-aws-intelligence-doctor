package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.maynowak.drmays.api.config.DiagnosisEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiagnosisController.class)
@Import({DiagnosisService.class, DiagnosisEngineConfiguration.class})
class DiagnosisControllerTest {

  private static final String NETWORK_REQUEST =
      """
      {"resources":[{"identity":{"provider":"aws","accountId":"123456789012",
      "region":"eu-central-1","resourceType":"network","resourceId":"net-1"},
      "tags":{},"attributes":{"ingress.public":"true"}}]}
      """;

  private static final String CLEAN_STORAGE_REQUEST =
      """
      {"resources":[{"identity":{"provider":"aws","accountId":"123456789012",
      "region":"eu-central-1","resourceType":"storage","resourceId":"store-1"},
      "tags":{"owner":"platform"},"attributes":{}}]}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsDiagnosisFindings() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(NETWORK_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findingCount").value(2))
        .andExpect(jsonPath("$.findings[0].ruleId").value("NET-PUBLIC-001"))
        .andExpect(jsonPath("$.findings[0].severity").value("HIGH"))
        .andExpect(jsonPath("$.findings[0].evidence.observed").value("ingress.public=true"))
        .andExpect(jsonPath("$.findings[1].ruleId").value("TAG-OWNER-001"))
        .andExpect(jsonPath("$.outcomes[0].type").value("FINDING"));
  }

  @Test
  void returnsEmptyFindingsForCleanResources() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CLEAN_STORAGE_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findingCount").value(0))
        .andExpect(jsonPath("$.findings").isEmpty())
        .andExpect(jsonPath("$.outcomes[0].type").value("NOT_APPLICABLE"))
        .andExpect(jsonPath("$.outcomes[1].type").value("NO_FINDING"));
  }

  @Test
  void listsRegisteredRules() throws Exception {
    mockMvc
        .perform(get("/api/v1/rules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rules.length()").value(2))
        .andExpect(jsonPath("$.rules[0].ruleId").value("NET-PUBLIC-001"))
        .andExpect(jsonPath("$.rules[1].ruleId").value("TAG-OWNER-001"));
  }

  @Test
  void returnsDeterministicJsonForRepeatedRequests() throws Exception {
    var first =
        mockMvc
            .perform(
                post("/api/v1/diagnoses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(NETWORK_REQUEST))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var second =
        mockMvc
            .perform(
                post("/api/v1/diagnoses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(NETWORK_REQUEST))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(first).isEqualTo(second);
  }
}
