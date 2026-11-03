package io.github.maynowak.drmays.aws.ec2;

import software.amazon.awssdk.services.ec2.Ec2Client;

/** Supplies an EC2 client for a specific region. */
@FunctionalInterface
public interface Ec2ClientProvider {

  Ec2Client client(String region);
}
