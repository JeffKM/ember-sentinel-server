package com.inhacapstone04.embersentinelserver.common.response;

import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final int code;       // ErrorCode에서 가져온 HTTP 상태 코드 (예: 401)
    private final String message; // ErrorCode에서 가져온 상세 메시지 (예: "유효하지 않은 토큰입니다.")

    public ErrorResponse(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
