package com.inhacapstone04.embersentinelserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

    private boolean isMinioEnabled() {
        return endpoint != null && !endpoint.isEmpty();
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (isMinioEnabled() && accessKey != null && !accessKey.isEmpty()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }

    /**
     * S3 Presigner: Presigned URL 생성을 위한 클라이언트
     * 로컬 환경에서는 MinIO 엔드포인트로 연결
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        if (isMinioEnabled()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(pathStyleAccess)
                            .build());
        }

        return builder.build();
    }

    /**
     * 일반 S3 Client (업로드/삭제 등 필요 시 사용)
     * 로컬 환경에서는 MinIO 엔드포인트로 연결
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        if (isMinioEnabled()) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(pathStyleAccess);
        }

        return builder.build();
    }
}
