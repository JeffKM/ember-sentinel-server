package com.inhacapstone04.embersentinelserver.user.dto.response;

public record AuthInfoResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn, // 만료 시간 (초 단위)
        Boolean isNewUser
) {
    /**
     * @param accessToken 서버가 발급한 액세스 토큰
     * @param refreshToken 서버가 발급한 리프레시 토큰
     * @param accessTokenExpiresIn 액세스 토큰 만료 시간 (초)
     * @param isNewUser 신규 유저인지 여부
     */
    public static AuthInfoResponse of(String accessToken, String refreshToken, Long accessTokenExpiresIn, Boolean isNewUser) {
        return new AuthInfoResponse("Bearer", accessToken, refreshToken, accessTokenExpiresIn, isNewUser);
    }
}
