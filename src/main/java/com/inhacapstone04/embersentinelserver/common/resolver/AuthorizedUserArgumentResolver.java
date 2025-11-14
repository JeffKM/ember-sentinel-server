package com.inhacapstone04.embersentinelserver.common.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class AuthorizedUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_ATTRIBUTE = "userId";

    /**
     * 이 리졸버가 어떤 파라미터를 지원(support)하는지 검사합니다.
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 1. @AuthorizedUser 어노테이션이 붙어있는지 확인
        boolean hasAnnotation = parameter.hasParameterAnnotation(AuthorizedUser.class);
        // 2. 파라미터의 타입이 Long인지 확인
        boolean isLongType = parameter.getParameterType().equals(Long.class);

        return hasAnnotation && isLongType;
    }

    /**
     * supportsParameter가 true를 반환했을 때,
     * 실제 파라미터에 어떤 값을 주입(resolve)할지 결정합니다.
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // 1. AuthInterceptor가 request attribute에 저장해둔 "userId"를 꺼냅니다.
        Object userIdObject = request.getAttribute(USER_ID_ATTRIBUTE);

        if (userIdObject == null) {
            // 인터셉터가 실행되지 않도록 설정된 경로(excludePathPatterns)가 아닌데도
            // userId가 없는 경우, 서버 로직 어딘가에 문제가 있는 것입니다.
            log.warn("AuthorizedUserArgumentResolver: userId attribute is null. " +
                    "Check AuthInterceptor logic and WebConfig exclude paths.");
            // (혹은 예외를 던져 500 에러를 낼 수도 있습니다)
            return null;
        }

        // 2. Long 타입으로 캐스팅하여 반환 -> 컨트롤러 매개변수에 주입됨
        return (Long) userIdObject;
    }
}