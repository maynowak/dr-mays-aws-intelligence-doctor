package io.github.maynowak.drmays.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Local Spring Boot entry point for the Dr. Mays REST API.
 *
 * <p>The service binds to loopback by default and uses only synthetic domain data; it performs no
 * AWS or model-provider calls.
 */
@SpringBootApplication
public class DrMaysApiApplication {

  /**
   * Starts the API.
   *
   * @param args optional Spring Boot arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(DrMaysApiApplication.class, args);
  }
}
