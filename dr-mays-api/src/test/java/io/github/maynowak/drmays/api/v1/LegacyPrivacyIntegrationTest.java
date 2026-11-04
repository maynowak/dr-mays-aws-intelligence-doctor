package io.github.maynowak.drmays.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies the property-enabled legacy path fails closed without contacting a provider. */
@SpringBootTest(properties = "dr.mays.ai.enabled=true")
@AutoConfigureMockMvc
class LegacyPrivacyIntegrationTest {

  private static final String REQUEST =
      """
      {"resources":[{"identity":{"provider":"aws","accountId":"123456789012",
      "region":"eu-west-1","resourceType":"network","resourceId":"net-1"},
      "tags":{},"attributes":{"ingress.public":"true"}}]}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void enabledLegacyPathReturnsApprovalRequired() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVAL_REQUIRED"))
        .andExpect(jsonPath("$.explanations").isEmpty());
  }
}
