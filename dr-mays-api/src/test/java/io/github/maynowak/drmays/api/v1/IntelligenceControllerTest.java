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

@SpringBootTest
@AutoConfigureMockMvc
class IntelligenceControllerTest {

  private static final String NETWORK_REQUEST =
      """
      {"resources":[{"identity":{"provider":"aws","accountId":"000000000000",
      "region":"eu-west-1","resourceType":"network","resourceId":"net-1"},
      "tags":{},"attributes":{"ingress.public":"true","vpc.id":"vpc-1"}}]}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void explainsFindingsWithMockAnalyzer() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(NETWORK_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.metadata.provider").value("mock"))
        .andExpect(jsonPath("$.explanations.length()").value(2))
        .andExpect(jsonPath("$.evidenceReferences").isNotEmpty())
        .andExpect(jsonPath("$.verifiedRelationships[0].type").value("SECURITY_GROUP_IN_VPC"));
  }

  @Test
  void handlesHealthyResourceWithoutFindings() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resources":[{"identity":{"provider":"aws","accountId":"000000000000",
                    "region":"eu-west-1","resourceType":"s3-bucket","resourceId":"bucket-1"},
                    "tags":{"owner":"platform"},"attributes":{}}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.explanations").isEmpty())
        .andExpect(jsonPath("$.uncertainties").isNotEmpty());
  }

  @Test
  void exposesPartialDiscoveryUncertainty() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"partialDiscovery":true,"resources":[{"identity":{"provider":"aws",
                    "accountId":"000000000000","region":"eu-west-1","resourceType":"network",
                    "resourceId":"net-1"},"tags":{},"attributes":{"ingress.public":"true"}}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uncertainties").isNotEmpty());
  }

  @Test
  void rejectsEmptyResources() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resources\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request validation failed"));
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request body"));
  }

  @Test
  void rejectsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.TEXT_PLAIN)
                .content("not json"))
        .andExpect(status().isUnsupportedMediaType());
  }
}
