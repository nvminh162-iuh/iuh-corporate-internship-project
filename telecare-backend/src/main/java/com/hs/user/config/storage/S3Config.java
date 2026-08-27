package com.hs.user.config.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class S3Config {

    @Value("${aws.s3.region:ap-southeast-1}")
    private String region;

    @Value("${aws.s3.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key:}")
    private String secretAccessKey;

    @Bean
    public S3Client s3Client() {
        AwsCredentialsProvider credentialsProvider;
        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            log.info("Initializing S3Client with static credentials for region {}", region);
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())
            );
        } else {
            log.info("Initializing S3Client with default credential provider chain for region {}", region);
            try {
                credentialsProvider = DefaultCredentialsProvider.create();
            } catch (Exception e) {
                log.warn("Could not create DefaultCredentialsProvider, falling back to AnonymousCredentialsProvider: {}", e.getMessage());
                credentialsProvider = AnonymousCredentialsProvider.create();
            }
        }

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
