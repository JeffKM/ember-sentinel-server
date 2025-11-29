package com.inhacapstone04.embersentinelserver.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * S3 Object Key를 기반으로 Presigned URL을 생성합니다.
     *
     * @param storedUrl DB에 저장된 원본 URL (예: https://bucket.s3.../key.mp4) 또는 Key
     * @return 서명된 임시 URL
     */
    public String generatePresignedUrl(String storedUrl) {
        if (storedUrl == null || storedUrl.isEmpty()) {
            return null;
        }

        try {
            // 1. Key 추출 (전체 URL인 경우 파싱)
            String objectKey = extractKeyFromUrl(storedUrl);

            // 2. Presign 요청 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30)) // 유효기간 30분
                    .getObjectRequest(getObjectRequest)
                    .build();

            // 3. URL 생성
            String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Generated Presigned URL for key: {}", objectKey);

            return presignedUrl;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", storedUrl, e);
            // 실패 시 원본 URL이라도 반환하거나 예외 처리 (상황에 따라 결정)
            // 여기서는 예외를 던지기보다 로그를 남기고 null 반환 -> 클라이언트가 처리
            return null;
        }
    }

    /**
     * URL에서 S3 Object Key만 추출합니다.
     * 예: "https://my-bucket.s3.ap-northeast-2.amazonaws.com/recordings/video.mp4"
     * -> "recordings/video.mp4"
     */
    private String extractKeyFromUrl(String url) {
        // 만약 DB에 Key만 저장되어 있다면 그대로 반환
        if (!url.startsWith("http")) {
            return url;
        }

        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            // path는 "/recordings/video.mp4" 형태이므로 앞의 슬래시 제거
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("Failed to parse S3 URL: {}. Assuming it is a key.", url);
            return url;
        }
    }
}
