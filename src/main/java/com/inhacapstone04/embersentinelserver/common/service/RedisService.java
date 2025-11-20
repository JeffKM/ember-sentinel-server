package com.inhacapstone04.embersentinelserver.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 데이터 저장, 조회, 삭제를 처리하는 서비스 클래스.
 * Refresh Token 관리 등에 사용됩니다.
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    // RedisConfig에서 String, String 직렬화로 설정된 RedisTemplate을 주입받습니다.
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis에 키-값 쌍을 저장하고 만료 시간(TTL)을 설정합니다.
     *
     * @param key 저장할 키 (예: "RT:<userId>")
     * @param value 저장할 값 (예: Refresh Token 문자열)
     * @param duration 만료 시간 (예: Duration.ofDays(7))
     */
    public void setValues(String key, String value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * 주어진 키에 해당하는 값을 조회합니다.
     *
     * @param key 조회할 키
     * @return 조회된 값 (없으면 null 반환)
     */
    public String getValues(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 주어진 키가 Redis에 존재하는지 확인합니다.
     *
     * @param key 확인할 키
     * @return 존재하면 true, 아니면 false
     */
    public boolean hasKey(String key) {
        // null 체크를 통해 Boolean 값이 null인 경우를 대비합니다.
        Boolean result = redisTemplate.hasKey(key);
        return result != null && result;
    }

    /**
     * 주어진 키를 삭제합니다.
     *
     * @param key 삭제할 키
     */
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    /**
     * (선택적) 주어진 키의 만료 시간(TTL)을 설정합니다.
     *
     * @param key 만료 시간을 설정할 키
     * @param duration 새로운 만료 시간
     */
    public Boolean setExpiration(String key, Duration duration) {
        return redisTemplate.expire(key, duration);
    }
}
