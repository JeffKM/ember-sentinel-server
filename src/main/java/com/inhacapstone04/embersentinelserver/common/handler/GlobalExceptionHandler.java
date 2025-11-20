package com.inhacapstone04.embersentinelserver.common.handler;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @RestControllerAdvice
 *
 * 컨트롤러 계층에서 발생하는 예외를 전역적으로 감지하고 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @ExceptionHandler(CustomException.class)
     *
     * CustomException 타입의 예외가 발생하면 이 메서드가 가로채서 처리합니다.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        // 1. 발생한 예외(e)로부터 ErrorCode enum 전체를 가져옵니다.
        //    (CustomException 내부 로직 변경이 필요합니다.)
        ErrorCode errorCode = e.getCode();

        // 2. HTTP Status Code를 가져옵니다. (토큰 만료 시 401을 반환하도록 설정되어야 함)
        HttpStatus httpStatus = errorCode.getHttpStatus();

        // 3. 클라이언트에게 반환할 ErrorResponse DTO를 생성합니다.
        //    (errorCode.name()은 ACCESS_TOKEN_EXPIRED 같은 문자열)
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.name(), // String Code (e.g., "ACCESS_TOKEN_EXPIRED")
                e.getMessage()
        );

        // 4. 로그를 기록합니다.
        log.warn("CustomException occurred - Code: {}, Message: {}", errorCode.name(), e.getMessage());

        // 5. HTTP Status와 DTO를 담아 반환
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    // Unhandled Exception 핸들러는 그대로 유지합니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return new ResponseEntity<>(
                new ErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다: " + e.getMessage()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
