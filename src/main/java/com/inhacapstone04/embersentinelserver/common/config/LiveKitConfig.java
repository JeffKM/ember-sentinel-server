package com.inhacapstone04.embersentinelserver.common.config;

import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.WebhookReceiver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @Value("${livekit.api.url}")
    private String apiUrl;

    /**
     * 방(Room) 관리 및 참가자 제어를 위한 클라이언트
     * Retrofit 기반의 RoomServiceClient를 생성합니다.
     */
    @Bean
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.createClient(apiUrl, apiKey, apiSecret);
    }

    /**
     * 녹화(Egress) 및 송출 제어를 위한 클라이언트
     * Retrofit 기반의 EgressServiceClient를 생성합니다.
     */
    @Bean
    public EgressServiceClient egressServiceClient() {
        return EgressServiceClient.createClient(apiUrl, apiKey, apiSecret);
    }

    /**
     * Webhook 서명 검증을 위한 Receiver 빈 등록
     * 이 객체는 내부적으로 API Key와 Secret을 사용하여 들어오는 요청의
     * Authorization 헤더(JWT)를 검증합니다.
     */
    @Bean
    public WebhookReceiver webhookReceiver() {
        return new WebhookReceiver(apiKey, apiSecret);
    }
}
