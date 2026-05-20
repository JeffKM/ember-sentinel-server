package com.inhacapstone04.embersentinelserver.support;

import com.inhacapstone04.embersentinelserver.camera_edge.repository.CameraEdgeRepository;
import com.inhacapstone04.embersentinelserver.common.service.RateLimitService;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * MockMvc 컨트롤러 테스트 공통 설정
 * - JwtUtil, CameraEdgeRepository, RateLimitService를 MockBean으로 설정
 * - 인터셉터 바이패스를 위한 헬퍼 메서드 제공
 */
public abstract class MockMvcTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected JwtUtil jwtUtil;

    @MockitoBean
    protected CameraEdgeRepository cameraEdgeRepository;

    @MockitoBean
    protected RateLimitService rateLimitService;

    /**
     * AuthInterceptor를 바이패스하기 위한 Mock 설정
     * - JWT 토큰 검증 통과
     * - userId 추출 시 지정된 userId 반환
     */
    protected void mockValidJwt(Long userId) {
        doNothing().when(jwtUtil).validateToken(anyString());
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(userId);
    }
}
