package com.inhacapstone04.embersentinelserver.user.controller;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import com.inhacapstone04.embersentinelserver.user.dto.UserInfoResponse;
import com.inhacapstone04.embersentinelserver.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserQueryController {

    private final UserQueryService userQueryService;

    @GetMapping("/info")
    public ResponseEntity<UserInfoResponse> findUserInfo(
            @AuthorizedUser Long userId
    ) {

        return ResponseEntity.ok(userQueryService.findUserById(userId));
    }
}
