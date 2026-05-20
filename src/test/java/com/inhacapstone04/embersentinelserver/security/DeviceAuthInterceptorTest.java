package com.inhacapstone04.embersentinelserver.security;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RateLimitService;
import com.inhacapstone04.embersentinelserver.security.interceptor.DeviceAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceAuthInterceptorTest {

    @InjectMocks
    private DeviceAuthInterceptor deviceAuthInterceptor;

    @Mock private CameraEdgeRepository cameraEdgeRepository;
    @Mock private RateLimitService rateLimitService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private final String VALID_API_KEY = "valid-api-key";
    private final Long CAMERA_ID = 10L;

    @Test
    @DisplayName("성공: 유효한 API Key + Rate Limit 통과 + setAttribute 검증")
    void preHandle_Success() throws Exception {
        // Given
        CameraEdge camera = mock(CameraEdge.class);
        when(camera.getId()).thenReturn(CAMERA_ID);

        when(request.getHeader("X-Device-API-Key")).thenReturn(VALID_API_KEY);
        when(cameraEdgeRepository.findByApiKey(VALID_API_KEY)).thenReturn(Optional.of(camera));
        doNothing().when(rateLimitService).checkRateLimit(VALID_API_KEY);

        // When
        boolean result = deviceAuthInterceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(request).setAttribute("deviceCameraEdgeId", CAMERA_ID);
    }

    @Test
    @DisplayName("실패: 헤더 없음 → INVALID_DEVICE_API_KEY")
    void preHandle_Fail_NoHeader() {
        // Given
        when(request.getHeader("X-Device-API-Key")).thenReturn(null);

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> deviceAuthInterceptor.preHandle(request, response, new Object()));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_DEVICE_API_KEY);
    }

    @Test
    @DisplayName("실패: 빈 헤더 → INVALID_DEVICE_API_KEY")
    void preHandle_Fail_BlankHeader() {
        // Given
        when(request.getHeader("X-Device-API-Key")).thenReturn("   ");

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> deviceAuthInterceptor.preHandle(request, response, new Object()));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_DEVICE_API_KEY);
    }

    @Test
    @DisplayName("실패: DB에 존재하지 않는 API Key → INVALID_DEVICE_API_KEY")
    void preHandle_Fail_InvalidApiKey() {
        // Given
        when(request.getHeader("X-Device-API-Key")).thenReturn("unknown-key");
        when(cameraEdgeRepository.findByApiKey("unknown-key")).thenReturn(Optional.empty());

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> deviceAuthInterceptor.preHandle(request, response, new Object()));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_DEVICE_API_KEY);
    }

    @Test
    @DisplayName("실패: Rate Limit 초과 → RATE_LIMIT_EXCEEDED")
    void preHandle_Fail_RateLimitExceeded() {
        // Given
        CameraEdge camera = mock(CameraEdge.class);

        when(request.getHeader("X-Device-API-Key")).thenReturn(VALID_API_KEY);
        when(cameraEdgeRepository.findByApiKey(VALID_API_KEY)).thenReturn(Optional.of(camera));
        doThrow(new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED))
                .when(rateLimitService).checkRateLimit(VALID_API_KEY);

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> deviceAuthInterceptor.preHandle(request, response, new Object()));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
