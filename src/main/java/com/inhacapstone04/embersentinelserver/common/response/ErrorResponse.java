package com.inhacapstone04.embersentinelserver.common.response;

import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    // 기존 int code 대신 String code (e.g., "ACCESS_TOKEN_EXPIRED") 사용
    private final String code;
    private final String message;
}
