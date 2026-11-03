package io.github.maynowak.drmays.aws.failure;

import io.github.maynowak.drmays.aws.discovery.AwsService;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailure;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;

/** Maps SDK failures to stable safe categories without raw messages. */
public final class AwsFailureMapper {

  private AwsFailureMapper() {}

  public static DiscoveryFailureCode mapCode(Throwable throwable) {
    if (throwable instanceof ApiCallTimeoutException
        || throwable instanceof ApiCallAttemptTimeoutException) {
      return DiscoveryFailureCode.TIMEOUT;
    }
    if (throwable instanceof AwsServiceException serviceException) {
      var status = serviceException.statusCode();
      if (status == 403) {
        return DiscoveryFailureCode.ACCESS_DENIED;
      }
      if (status == 429) {
        return DiscoveryFailureCode.THROTTLING;
      }
      if (status >= 500) {
        return DiscoveryFailureCode.SERVICE_ERROR;
      }
    }
    if (throwable instanceof SdkClientException) {
      return DiscoveryFailureCode.ENDPOINT;
    }
    return DiscoveryFailureCode.UNKNOWN;
  }

  public static DiscoveryFailure failure(AwsService service, Throwable throwable) {
    var code = mapCode(throwable);
    return new DiscoveryFailure(service, code, messageFor(code));
  }

  private static String messageFor(DiscoveryFailureCode code) {
    return switch (code) {
      case ACCESS_DENIED -> "AWS request was denied";
      case THROTTLING -> "AWS request was throttled";
      case TIMEOUT -> "AWS request timed out";
      case ENDPOINT -> "AWS endpoint was unreachable";
      case SERVICE_ERROR -> "AWS service returned an error";
      default -> "AWS discovery failed";
    };
  }
}
