package com.inhacapstone04.embersentinelserver.user.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.user.dto.request.FcmTokenRequest;
import com.inhacapstone04.embersentinelserver.user.service.UserCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserCommandController {

    private final UserCommandService userCommandService;

    /**
     * 사용자의 FCM 토큰 등록/갱신 API
     * API: POST /user/fcm/token
     */
    @PostMapping("/fcm/token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthorizedUser Long userId,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        userCommandService.updateFcmToken(userId, request.fcmToken());
        return ResponseEntity.ok().build();
    }
}
