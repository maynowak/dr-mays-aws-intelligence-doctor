package io.github.maynowak.drmays.aws.failure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.aws.discovery.AwsService;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

class AwsFailureMapperTest {

  @Test
  void mapsAccessDenied() {
    var exception = S3Exception.builder().statusCode(403).build();

    assertThat(AwsFailureMapper.mapCode(exception)).isEqualTo(DiscoveryFailureCode.ACCESS_DENIED);
    assertThat(AwsFailureMapper.failure(AwsService.S3, exception).message())
        .isEqualTo("AWS request was denied");
  }

  @Test
  void mapsThrottling() {
    assertThat(AwsFailureMapper.mapCode(S3Exception.builder().statusCode(429).build()))
        .isEqualTo(DiscoveryFailureCode.THROTTLING);
  }

  @Test
  void mapsServerError() {
    assertThat(AwsFailureMapper.mapCode(S3Exception.builder().statusCode(503).build()))
        .isEqualTo(DiscoveryFailureCode.SERVICE_ERROR);
  }

  @Test
  void mapsTimeout() {
    assertThat(AwsFailureMapper.mapCode(ApiCallTimeoutException.create(1000L)))
        .isEqualTo(DiscoveryFailureCode.TIMEOUT);
  }

  @Test
  void mapsClientFailureToEndpoint() {
    assertThat(AwsFailureMapper.mapCode(SdkClientException.create("unreachable")))
        .isEqualTo(DiscoveryFailureCode.ENDPOINT);
  }

  @Test
  void mapsUnknownFailureWithoutLeakingDetails() {
    var failure = AwsFailureMapper.failure(AwsService.EC2, new RuntimeException("secret"));

    assertThat(failure.code()).isEqualTo(DiscoveryFailureCode.UNKNOWN);
    assertThat(failure.message()).doesNotContain("secret");
  }
}
