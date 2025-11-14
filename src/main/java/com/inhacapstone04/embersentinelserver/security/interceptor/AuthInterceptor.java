package com.inhacapstone04.embersentinelserver.security.interceptor;

import com.inhacapstone04.embersentinelserver.common.exception.CustomException;
import com.inhacapstone04.embersentinelserver.common.exception.ErrorCode;
import com.inhacapstone04.embersentinelserver.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTRIBUTE = "userId"; // (3) 리졸버에서 사용할 Attribute 이름

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 1. Authorization 헤더 추출
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 2. 헤더가 없거나 "Bearer "로 시작하지 않으면 CustomException throw
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // GlobalExceptionHandler가 이 예외를 처리합니다.
            throw new CustomException(ErrorCode.EMPTY_TOKEN);
        }

        // 3. "Bearer " 접두사 제거
        String token = authHeader.substring(BEARER_PREFIX.length());

        // 4. 토큰 유효성 검증
        jwtUtil.validateToken(token);

        // 5. 토큰에서 userId 추출
        // - validateToken()이 이미 성공했으므로, getUserIdFromToken()도 대부분 성공합니다.
        // - 만약 getUserIdFromToken()이 예외(예: 클레임 타입 오류)를 throw하더라도
        //   GlobalExceptionHandler가 처리해줄 것입니다.
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 6. request attribute에 userId 저장
        // -> 이 값은 이후 ArgumentResolver에서 사용됩니다.
        request.setAttribute(USER_ID_ATTRIBUTE, userId);

        return true; // 컨트롤러로 요청 계속 진행
    }
}
