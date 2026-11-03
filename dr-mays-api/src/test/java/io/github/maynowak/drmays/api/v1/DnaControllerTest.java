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
class DnaControllerTest {

  private static final String GRAPH_REQUEST =
      """
      {"resources":[{"identity":{"provider":"aws","accountId":"000000000000",
      "region":"eu-west-1","resourceType":"network","resourceId":"sg-1"},
      "tags":{"owner":"platform"},"attributes":{"vpc.id":"vpc-1",
      "ingress.referenced.securityGroupIds":"sg-2"}}]}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsGraphNodesAndEdges() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/dna/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GRAPH_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(3))
        .andExpect(jsonPath("$.edges.length()").value(2))
        .andExpect(jsonPath("$.edges[0].type").value("SECURITY_GROUP_REFERENCES_SECURITY_GROUP"))
        .andExpect(jsonPath("$.edges[1].type").value("SECURITY_GROUP_IN_VPC"))
        .andExpect(jsonPath("$.stats.nodeCount").value(3))
        .andExpect(jsonPath("$.stats.referencedCount").value(2));
  }

  @Test
  void returnsSingleNodeGraph() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/dna/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resources":[{"identity":{"provider":"aws","accountId":"000000000000",
                    "region":"eu-west-1","resourceType":"s3-bucket","resourceId":"bucket-1"},
                    "tags":{"owner":"platform"},"attributes":{}}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(1))
        .andExpect(jsonPath("$.edges").isEmpty())
        .andExpect(jsonPath("$.stats.componentCount").value(1));
  }

  @Test
  void exposesPartialDiscovery() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/dna/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"partialDiscovery":true,"resources":[{"identity":{"provider":"aws",
                    "accountId":"000000000000","region":"eu-west-1","resourceType":"s3-bucket",
                    "resourceId":"bucket-1"},"tags":{},"attributes":{}}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completeness").value("PARTIAL"))
        .andExpect(jsonPath("$.issues[0].code").value("PARTIAL_DISCOVERY"));
  }

  @Test
  void rejectsEmptyResources() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/dna/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resources\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request validation failed"));
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/dna/graphs").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request body"));
  }

  @Test
  void rejectsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(post("/api/v1/dna/graphs").contentType(MediaType.TEXT_PLAIN).content("not json"))
        .andExpect(status().isUnsupportedMediaType());
  }
}
