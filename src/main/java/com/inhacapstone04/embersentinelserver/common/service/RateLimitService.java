package com.inhacapstone04.embersentinelserver.common.service;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 Rate Limiter.
 * 디바이스별 요청 빈도를 제한합니다 (슬라이딩 윈도우).
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "RL:device:";
    private static final int MAX_REQUESTS = 1;       // 1분당 최대 요청 수
    private static final Duration WINDOW = Duration.ofSeconds(60); // 윈도우 크기

    /**
     * 디바이스 API Key 기반 Rate Limiting 검사.
     * 제한 초과 시 CustomException(RATE_LIMIT_EXCEEDED)을 throw합니다.
     *
     * @param apiKey 디바이스 API Key
     */
    public void checkRateLimit(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;

        // INCR: 키가 없으면 0에서 시작하여 1을 반환, 있으면 기존 값 +1
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            // 첫 번째 요청 시 TTL 설정
            redisTemplate.expire(key, WINDOW);
        }

        if (currentCount != null && currentCount > MAX_REQUESTS) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }
}
