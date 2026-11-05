package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.maynowak.drmays.api.config.DiagnosisEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DoctorReportController.class)
@Import({DoctorReportService.class, DiagnosisService.class, DiagnosisEngineConfiguration.class})
class DoctorReportControllerTest {

  private static final String REQUEST =
      """
      {"partialDiscovery":false,"resources":[{"identity":{"provider":"aws","accountId":"000000000000",
      "region":"eu-west-1","resourceType":"network","resourceId":"net-1"},
      "tags":{},"attributes":{"ingress.public":"true","vpc.id":"vpc-1"}}]}
      """;

  private static final String CLEAN_REQUEST =
      """
      {"partialDiscovery":false,"resources":[{"identity":{"provider":"aws","accountId":"000000000000",
      "region":"eu-west-1","resourceType":"storage","resourceId":"store-1"},
      "tags":{"owner":"platform"},"attributes":{}}]}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsStructuredJsonReport() throws Exception {
    mockMvc
        .perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(jsonPath("$.schemaVersion").value("doctor-report-v1"))
        .andExpect(jsonPath("$.metadata.diagnosisSource").value("DiagnosisEngine"))
        .andExpect(jsonPath("$.summary.findingCount").value(2))
        .andExpect(jsonPath("$.summary.highestSeverity").value("HIGH"))
        .andExpect(jsonPath("$.findings[0].ruleId").value("NET-PUBLIC-001"))
        .andExpect(jsonPath("$.findings[0].evidence.observed").value("ingress.public=true"))
        .andExpect(jsonPath("$.outcomes.length()").value(2))
        .andExpect(jsonPath("$.graph.stats.nodeCount").value(2))
        .andExpect(jsonPath("$.graph.stats.edgeCount").value(1))
        .andExpect(jsonPath("$.limitations").isNotEmpty());
  }

  @Test
  void returnsMarkdownReport() throws Exception {
    var response =
        mockMvc
            .perform(
                post("/api/v1/reports/markdown")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST))
            .andExpect(status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(MediaType.parseMediaType("text/markdown")))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).startsWith("# Dr. Mays Infrastructure Doctor Report");
    assertThat(response).contains("## 5. Infrastructure DNA");
    assertThat(response).contains("NET-PUBLIC-001");
  }

  @Test
  void marksPartialDiscoveryExplicitly() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    REQUEST.replace("\"resources\"", "\"partialDiscovery\":true,\"resources\"")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.partialDiscovery").value(true))
        .andExpect(jsonPath("$.graph.completeness").value("PARTIAL"))
        .andExpect(
            jsonPath("$.limitations[0]")
                .value("PARTIAL DISCOVERY — RESULTS DO NOT COVER UNKNOWN RESOURCES"));
  }

  @Test
  void emptyFindingsRemainExplicitlyInconclusive() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(CLEAN_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.findingCount").value(0))
        .andExpect(jsonPath("$.summary.highestSeverity").value("NONE"))
        .andExpect(jsonPath("$.findings").isEmpty());
  }

  @Test
  void repeatedRequestsProduceByteIdenticalJson() throws Exception {
    var first =
        mockMvc
            .perform(
                post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var second =
        mockMvc
            .perform(
                post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(first).isEqualTo(second);
  }

  @Test
  void rejectsEmptyResources() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partialDiscovery\":false,\"resources\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Request validation failed"));
  }

  @Test
  void rejectsUnknownFieldsAndProviderParameters() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    REQUEST.replace(
                        "{\"partialDiscovery\":false,",
                        "{\"providerId\":\"openai\",\"partialDiscovery\":false,")))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"approvalToken\":\"forged\",\"partialDiscovery\":false,"
                        + "\"resources\":[{\"identity\":{\"provider\":\"aws\","
                        + "\"accountId\":\"000000000000\",\"region\":\"eu-west-1\","
                        + "\"resourceType\":\"network\",\"resourceId\":\"net-1\"},"
                        + "\"tags\":{},\"attributes\":{}}]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMissingPartialDiscoveryFlag() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"resources\":[{\"identity\":{\"provider\":\"aws\","
                        + "\"accountId\":\"000000000000\",\"region\":\"eu-west-1\","
                        + "\"resourceType\":\"network\",\"resourceId\":\"net-1\"},"
                        + "\"tags\":{},\"attributes\":{}}]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMoreThanOneHundredResources() throws Exception {
    var body = new StringBuilder("{\"partialDiscovery\":false,\"resources\":[");
    for (var index = 0; index < 101; index++) {
      if (index > 0) {
        body.append(',');
      }
      body.append(
              "{\"identity\":{\"provider\":\"aws\",\"accountId\":\"000000000000\","
                  + "\"region\":\"eu-west-1\",\"resourceType\":\"network\","
                  + "\"resourceId\":\"net-")
          .append(index)
          .append("\"},\"tags\":{},\"attributes\":{}}");
    }
    body.append("]}");

    mockMvc
        .perform(
            post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnsupportedContentType() throws Exception {
    mockMvc
        .perform(post("/api/v1/reports").contentType(MediaType.TEXT_PLAIN).content(REQUEST))
        .andExpect(status().isUnsupportedMediaType());
  }
}
