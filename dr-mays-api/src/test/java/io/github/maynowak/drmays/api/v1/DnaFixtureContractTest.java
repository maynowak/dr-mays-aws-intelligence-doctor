package io.github.maynowak.drmays.api.v1;

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

@SpringBootTest
@AutoConfigureMockMvc
class DnaFixtureContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void singleResourceGraphHasNoEdges() throws Exception {
    postFixture("dna-single")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(1))
        .andExpect(jsonPath("$.edges").isEmpty())
        .andExpect(jsonPath("$.stats.componentCount").value(1));
  }

  @Test
  void securityGroupVpcReferenceIsVisible() throws Exception {
    postFixture("dna-sg-vpc")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(2))
        .andExpect(jsonPath("$.edges[0].type").value("SECURITY_GROUP_IN_VPC"))
        .andExpect(jsonPath("$.nodes[1].status").value("REFERENCED"));
  }

  @Test
  void securityGroupReferenceRequiresExplicitId() throws Exception {
    postFixture("dna-sg-reference")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.edges.length()").value(1))
        .andExpect(jsonPath("$.edges[0].type").value("SECURITY_GROUP_REFERENCES_SECURITY_GROUP"));
  }

  @Test
  void multipleAccountsStaySeparated() throws Exception {
    postFixture("dna-multi-account")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(4))
        .andExpect(jsonPath("$.edges.length()").value(2))
        .andExpect(jsonPath("$.stats.inventoriedCount").value(2))
        .andExpect(jsonPath("$.stats.referencedCount").value(2));
  }

  @Test
  void cyclicReferencesAreHandledSafely() throws Exception {
    postFixture("dna-cycle")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stats.cyclic").value(true))
        .andExpect(jsonPath("$.stats.componentCount").value(1));
  }

  @Test
  void partialDiscoveryRemainsVisible() throws Exception {
    postFixture("dna-partial")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completeness").value("PARTIAL"))
        .andExpect(jsonPath("$.issues[0].code").value("PARTIAL_DISCOVERY"));
  }

  private ResultActions postFixture(String name) throws Exception {
    return mockMvc.perform(
        post("/api/v1/dna/graphs").contentType(MediaType.APPLICATION_JSON).content(fixture(name)));
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
