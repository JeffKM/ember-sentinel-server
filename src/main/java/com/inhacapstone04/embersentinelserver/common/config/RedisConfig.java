package com.inhacapstone04.embersentinelserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Redis 연결 팩토리 생성 (Lettuce 기반)
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * RedisTemplate 설정 및 Bean 등록
     * Key와 Value를 String 형태로 관리하기 위해 StringRedisSerializer를 사용합니다.
     * @param connectionFactory 연결 팩토리
     * @return 설정된 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        // 연결 팩토리 설정
        redisTemplate.setConnectionFactory(connectionFactory);

        // Key와 Value 직렬화 설정
        // StringRedisSerializer를 사용하여 Key와 Value를 문자열 형태로 Redis에 저장하고 조회합니다.
        StringRedisSerializer serializer = new StringRedisSerializer();

        // Key는 String으로 설정 (예: "RT:<userId>")
        redisTemplate.setKeySerializer(serializer);
        // Value도 String으로 설정 (Refresh Token 문자열)
        redisTemplate.setValueSerializer(serializer);

        // Hash Key와 Value 직렬화 설정 (필요하다면)
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        // 초기화 후 반환
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
