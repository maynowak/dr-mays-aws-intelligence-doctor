package io.github.maynowak.drmays.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class DiagnosisErrorHandlingTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsEmptyResourceList() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resources\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void rejectsBlankIdentityField() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resources":[{"identity":{"provider":" ","accountId":"123456789012",
                    "region":"eu-central-1","resourceType":"network","resourceId":"net-1"}}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request body"));
  }

  @Test
  void rejectsUnknownJsonProperties() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resources":[{"identity":{"provider":"aws","accountId":"123456789012",
                    "region":"eu-central-1","resourceType":"network","resourceId":"net-1"}}],
                    "unexpected":true}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request body"));
  }

  @Test
  void rejectsDuplicateResourceIdentities() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resources":[
                    {"identity":{"provider":"aws","accountId":"123456789012",
                    "region":"eu-central-1","resourceType":"network","resourceId":"net-1"}},
                    {"identity":{"provider":"aws","accountId":"123456789012",
                    "region":"eu-central-1","resourceType":"network","resourceId":"net-1"}}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  @Test
  void rejectsInvalidResourceType() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diagnoses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"resources":[{"identity":{"provider":"aws","accountId":"123456789012",
                    "region":"eu-central-1","resourceType":"Network","resourceId":"net-1"}}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void rejectsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(post("/api/v1/diagnoses").contentType(MediaType.TEXT_PLAIN).content("not json"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.title").value("Unsupported media type"));
  }

  @Test
  void rejectsUnsupportedMethod() throws Exception {
    mockMvc
        .perform(put("/api/v1/rules"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.title").value("Method not allowed"));
  }
}
