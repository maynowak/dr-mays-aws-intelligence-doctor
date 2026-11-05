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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ExpertiseControllerTest {

  private static final String RESOURCE =
      """
      {"identity":{"provider":"aws","accountId":"123456789012","region":"eu-west-1",
      "resourceType":"network","resourceId":"net-1"},"tags":{},
      "attributes":{"ingress.public":"true","vpc.id":"vpc-0def5678"}}
      """;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired private MockMvc mockMvc;

  @Test
  void previewReturnsSanitizedPayloadWithoutProviderCall() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(previewBody("mock")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providerId").value("mock"))
        .andExpect(jsonPath("$.approvalRequired").value(false))
        .andExpect(jsonPath("$.validation.valid").value(true))
        .andExpect(jsonPath("$.payloadJson").isNotEmpty());
  }

  @Test
  void previewShowsExcludedFieldsAndNoRelationshipsByDefault() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(previewBody("mock")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.payloadJson").value(org.hamcrest.Matchers.containsString("[excluded]")))
        .andExpect(
            jsonPath("$.payloadJson")
                .value(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("fromAlias"))));
  }

  @Test
  void approveAndExplainReturnsValidatedExpertise() throws Exception {
    var preview =
        mockMvc
            .perform(
                post("/api/v1/expertise/previews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(previewBody("mock")))
            .andExpect(status().isOk())
            .andReturn();
    var previewId =
        OBJECT_MAPPER
            .readTree(preview.getResponse().getContentAsString())
            .get("previewId")
            .asText();

    var approval =
        mockMvc
            .perform(
                post("/api/v1/expertise/approvals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"previewId\":\"" + previewId + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    var token =
        OBJECT_MAPPER
            .readTree(approval.getResponse().getContentAsString())
            .get("approvalToken")
            .asText();

    mockMvc
        .perform(
            post("/api/v1/expertise/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"previewId\":\"" + previewId + "\",\"approvalToken\":\"" + token + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.explanations.length()").value(2))
        .andExpect(jsonPath("$.privacyReceipt.approvalStatus").value("NOT_REQUIRED"));
  }

  @Test
  void unknownPreviewReturnsApprovalRequired() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"previewId\":\"unknown\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVAL_REQUIRED"));
  }

  @Test
  void rejectsModelThatIsNotAllowlisted() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"providerId\":\"mock\",\"modelId\":\"gpt-4o\","
                        + "\"expertiseId\":\"standard\",\"resources\":["
                        + RESOURCE
                        + "]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnknownAuthorizationField() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"previewId\":\"x\",\"authorization\":\"forged\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unknownProviderIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(previewBody("not-registered")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void emptyResourcesAreRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resources\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void malformedJsonIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnknownJsonProperties() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expertise/previews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"unexpected\":true,\"resources\":[" + RESOURCE + "]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request body"));
  }

  @Test
  void existingIntelligenceEndpointStillWorks() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/intelligence/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resources\":[" + RESOURCE + "]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.metadata.provider").value("mock"));
  }

  private static String previewBody(String provider) {
    return "{\"providerId\":\""
        + provider
        + "\",\"modelId\":\"deterministic-v1\",\"expertiseId\":\"standard\","
        + "\"resources\":["
        + RESOURCE
        + "]}";
  }
}
