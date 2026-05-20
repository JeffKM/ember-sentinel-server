package com.inhacapstone04.embersentinelserver.common;

import com.inhacapstone04.embersentinelserver.common.service.RedisService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @InjectMocks
    private RedisService redisService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("setValues: 키-값 저장 + TTL 설정")
    void setValues() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration duration = Duration.ofMinutes(10);

        // When
        redisService.setValues("key", "value", duration);

        // Then
        verify(valueOperations).set("key", "value", duration);
    }

    @Test
    @DisplayName("getValues: 값 조회")
    void getValues() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("value");

        // When
        String result = redisService.getValues("key");

        // Then
        assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("hasKey: true 반환")
    void hasKey_True() {
        // Given
        when(redisTemplate.hasKey("key")).thenReturn(true);

        // When & Then
        assertThat(redisService.hasKey("key")).isTrue();
    }

    @Test
    @DisplayName("hasKey: null 반환 → false (null-safe)")
    void hasKey_Null() {
        // Given
        when(redisTemplate.hasKey("key")).thenReturn(null);

        // When & Then
        assertThat(redisService.hasKey("key")).isFalse();
    }

    @Test
    @DisplayName("deleteValues: 키 삭제")
    void deleteValues() {
        // When
        redisService.deleteValues("key");

        // Then
        verify(redisTemplate).delete("key");
    }

    @Test
    @DisplayName("setExpiration: TTL 설정")
    void setExpiration() {
        // Given
        Duration duration = Duration.ofMinutes(5);
        when(redisTemplate.expire("key", duration)).thenReturn(true);

        // When
        Boolean result = redisService.setExpiration("key", duration);

        // Then
        assertThat(result).isTrue();
    }
}
