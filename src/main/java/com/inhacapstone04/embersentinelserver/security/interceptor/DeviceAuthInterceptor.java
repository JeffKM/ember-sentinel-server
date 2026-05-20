package com.inhacapstone04.embersentinelserver.security.interceptor;

import com.inhacapstone04.embersentinelserver.camera_edge.entity.CameraEdge;
import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 엣지 디바이스(라즈베리파이) 전용 인증 인터셉터.
 * X-Device-API-Key 헤더를 검증하고 Rate Limiting을 적용합니다.
 */
@Component
@RequiredArgsConstructor
public class DeviceAuthInterceptor implements HandlerInterceptor {

    private static final String DEVICE_API_KEY_HEADER = "X-Device-API-Key";
    private static final String DEVICE_CAMERA_EDGE_ID_ATTRIBUTE = "deviceCameraEdgeId";

    private final CameraEdgeRepository cameraEdgeRepository;
    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 1. X-Device-API-Key 헤더 추출
        String apiKey = request.getHeader(DEVICE_API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_DEVICE_API_KEY);
        }

        // 2. API Key로 카메라 엣지 조회
        CameraEdge cameraEdge = cameraEdgeRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_DEVICE_API_KEY));

        // 3. Rate Limiting 검사 (디바이스 기반, 1분당 1회)
        rateLimitService.checkRateLimit(apiKey);

        // 4. 인증 성공 — request attribute에 카메라 ID 저장
        request.setAttribute(DEVICE_CAMERA_EDGE_ID_ATTRIBUTE, cameraEdge.getId());

        return true;
    }
}
