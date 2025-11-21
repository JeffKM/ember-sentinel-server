package com.inhacapstone04.embersentinelserver.common.util;

import io.livekit.server.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiveKitUtil {

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    /**
     * LiveKit Access Token 생성 (io.livekit:livekit-server 라이브러리 사용)
     *
     * @param roomName      입장할 방의 이름
     * @param identity      참여자의 고유 식별자
     * @param name          참여자의 표시 이름
     * @param metadata      참여자의 메타데이터 (JSON 문자열)
     * @param canPublish    송출 권한
     * @param canSubscribe  시청 권한
     * @return 생성된 JWT 토큰 문자열
     */
    public String createToken(String roomName, String identity, String name, String metadata, boolean canPublish, boolean canSubscribe) {
        // 1. AccessToken 생성
        AccessToken token = new AccessToken(apiKey, apiSecret);

        // 2. 기본 정보 설정
        token.setName(name);
        token.setIdentity(identity);
        token.setMetadata(metadata);

        // 3. 권한(Grants) 추가
        // Kotlin SDK 방식: 개별 Grant 객체를 추가
        token.addGrants(
                new RoomJoin(true),          // 방 입장 허용
                new RoomName(roomName),      // 방 이름 지정
                new CanPublish(canPublish),  // 송출 권한
                new CanSubscribe(canSubscribe) // 시청 권한
        );

        // 데이터 송출 권한 (Publisher인 경우 추가)
        if (canPublish) {
            token.addGrants(new CanPublishData(true));
        }

        // 4. 토큰 유효기간 설정 (2시간 = 7200초)
        // Kotlin SDK의 setTtl은 초 단위를 사용함
        token.setTtl(7200);

        // 5. JWT 문자열로 변환
        return token.toJwt();
    }
}
