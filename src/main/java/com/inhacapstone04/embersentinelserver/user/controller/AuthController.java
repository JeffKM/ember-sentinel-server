package com.inhacapstone04.embersentinelserver.user.controller;

import com.inhacapstone04.embersentinelserver.user.dto.OAuthLoginRequest;
import com.inhacapstone04.embersentinelserver.user.dto.AuthInfoResponse;
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
@RequestMapping("/auth") // /auth/google, /auth/kakao 요청을 처리
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
}
