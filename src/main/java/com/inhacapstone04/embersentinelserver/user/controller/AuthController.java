package com.inhacapstone04.embersentinelserver.user.controller;

import com.inhacapstone04.embersentinelserver.user.dto.request.EmailLoginRequest;
import com.inhacapstone04.embersentinelserver.user.dto.request.OAuthLoginRequest;
import com.inhacapstone04.embersentinelserver.user.dto.response.AuthInfoResponse;
import com.inhacapstone04.embersentinelserver.user.entity.AuthType;
import com.inhacapstone04.embersentinelserver.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthInfoResponse> loginGoogle(
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        // AuthType.GOOGLE과 3rd-party 토큰을 서비스로 전달
        AuthInfoResponse response = authService.login(AuthType.GOOGLE, request.accessToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/kakao")
    public ResponseEntity<AuthInfoResponse> loginKakao(
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        // AuthType.KAKAO와 3rd-party 토큰을 서비스로 전달
        AuthInfoResponse response = authService.login(AuthType.KAKAO, request.accessToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email")
    public ResponseEntity<AuthInfoResponse> loginUserEmail(
            @Valid @RequestBody EmailLoginRequest request
    ) {
        // AuthType.EMAIL과 request를 서비스로 전달
        AuthInfoResponse response = authService.loginByEmail(AuthType.EMAIL, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh Token을 사용하여 Access Token과 Refresh Token을 재발급합니다.
     * 클라이언트로부터 Refresh Token을 본문으로 받습니다.
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<AuthInfoResponse> refresh(
            @RequestBody String refreshToken
    ) {
        AuthInfoResponse response = authService.reissueToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
