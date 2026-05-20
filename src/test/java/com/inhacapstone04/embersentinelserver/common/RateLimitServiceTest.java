package com.inhacapstone04.embersentinelserver.common;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @InjectMocks
    private RateLimitService rateLimitService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final String API_KEY = "test-api-key";
    private final String EXPECTED_KEY = "RL:device:" + API_KEY;

    @Test
    @DisplayName("성공: 첫 번째 요청 (count=1) → TTL 설정, 예외 없음")
    void checkRateLimit_FirstRequest_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(1L);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(API_KEY));
        verify(redisTemplate).expire(eq(EXPECTED_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("실패: 두 번째 요청 (count=2) → RATE_LIMIT_EXCEEDED")
    void checkRateLimit_SecondRequest_Exceeds() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(2L);

        // When & Then
        CustomException ex = assertThrows(CustomException.class,
                () -> rateLimitService.checkRateLimit(API_KEY));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("성공: increment 반환값 null → 방어 로직에 의해 예외 없음")
    void checkRateLimit_NullReturn_NoException() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(EXPECTED_KEY)).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(API_KEY));
    }
}
