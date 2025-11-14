package com.inhacapstone04.embersentinelserver.common.handler;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
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

        // 1. 발생한 예외(e)로부터 code와 message를 가져옵니다.
        int errorCode = e.getCode();
        String errorMessage = e.getMessage();

        // 2. 로그를 기록합니다.
        log.warn("CustomException occurred - Code: {}, Message: {}", errorCode, errorMessage);

        // 3. 클라이언트에게 반환할 ErrorResponse DTO를 생성합니다.
        ErrorResponse errorResponse = new ErrorResponse(errorCode, errorMessage);

        // 4.
        // ResponseEntity를 생성할 때, Body에는 ErrorResponse DTO를,
        // HTTP Status에는 ErrorCode의 code 값(int)을 HttpStatus enum으로 변환하여 설정합니다.
        return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(errorCode));
    }

    /**
     * CustomException 외에 미처 처리하지 못한 모든 예외(Exception)를
     * 500 Internal Server Error로 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        log.error("Unhandled exception occurred", e); // 스택 트레이스 포함

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 내부 오류가 발생했습니다: " + e.getMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
