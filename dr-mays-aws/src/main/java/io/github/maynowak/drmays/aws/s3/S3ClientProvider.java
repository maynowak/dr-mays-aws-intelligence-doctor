package io.github.maynowak.drmays.aws.s3;

import software.amazon.awssdk.services.s3.S3Client;

/** Supplies an S3 client for a specific region. */
@FunctionalInterface
public interface S3ClientProvider {

  S3Client client(String region);
}
