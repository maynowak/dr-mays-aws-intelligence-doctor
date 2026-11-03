package io.github.maynowak.drmays.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthIntegrationTest {

  private final HttpClient client =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  @Value("${local.server.port}")
  private int port;

  @Value("${server.address}")
  private String serverAddress;

  @Value("${management.endpoints.web.exposure.include}")
  private String exposedEndpoints;

  @Test
  void healthEndpointReportsUp() throws Exception {
    var response = get("/actuator/health");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"");
  }

  @Test
  void exposesOnlyHealthActuatorEndpoint() throws Exception {
    assertThat(exposedEndpoints).isEqualTo("health");
    assertThat(get("/actuator/info").statusCode()).isEqualTo(404);
  }

  @Test
  void bindsToLoopbackByDefault() {
    assertThat(serverAddress).isEqualTo("127.0.0.1");
  }

  private HttpResponse<String> get(String path) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
