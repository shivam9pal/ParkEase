package com.parkease.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            // Use default credential provider chain (IAM roles, environment variables, etc.)
            return S3Client.builder()
                    .region(Region.of(region))
                    .build();
        } else {
            // Use provided credentials
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
        }
    }
}
