package com.inhacapstone04.embersentinelserver.common;

import com.inhacapstone04.embersentinelserver.common.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @InjectMocks
    private S3Service s3Service;

    @Mock
    private S3Presigner s3Presigner;

    @Test
    @DisplayName("null 입력 → null 반환")
    void generatePresignedUrl_NullInput() {
        assertThat(s3Service.generatePresignedUrl(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열 입력 → null 반환")
    void generatePresignedUrl_EmptyInput() {
        assertThat(s3Service.generatePresignedUrl("")).isNull();
    }

    @Test
    @DisplayName("URL에서 key 추출하여 presigned URL 생성")
    void generatePresignedUrl_FromUrl() throws Exception {
        // Given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/recordings/video.mp4?signed=true"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // When
        String result = s3Service.generatePresignedUrl("https://test-bucket.s3.amazonaws.com/recordings/video.mp4");

        // Then
        assertThat(result).contains("signed=true");
    }

    @Test
    @DisplayName("직접 key 사용하여 presigned URL 생성")
    void generatePresignedUrl_FromKey() throws Exception {
        // Given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/recordings/video.mp4?signed=true"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // When
        String result = s3Service.generatePresignedUrl("recordings/video.mp4");

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("예외 발생 시 null 반환")
    void generatePresignedUrl_ExceptionReturnsNull() {
        // Given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("S3 error"));

        // When
        String result = s3Service.generatePresignedUrl("recordings/video.mp4");

        // Then
        assertThat(result).isNull();
    }
}
