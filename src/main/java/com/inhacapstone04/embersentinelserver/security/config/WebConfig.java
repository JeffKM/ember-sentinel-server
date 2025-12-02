package com.inhacapstone04.embersentinelserver.security.config;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUserArgumentResolver;
import com.inhacapstone04.embersentinelserver.security.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthorizedUserArgumentResolver authorizedUserArgumentResolver;

    /**
     * AuthInterceptor를 스프링에 등록합니다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**") // (1) 모든 요청에 대해 인터셉터 실행
                .excludePathPatterns( // (2) 단, 여기 명시된 경로는 인터셉터 실행 제외 (인증X)
                        // --- auth(login) 관련
                        "/auth/**",

                        // --- building 관련 API
                        "/building/**",

                        // --- Swagger/API Docs ---
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",

                        // --- 로그 더럽히는 요청 제거 ---
                        "/webjars/**",
                        "/favicon.ico",
                        "/error",

                        // --- RasberryPI ---
                        "/embedded/**",

                        // --- webhook ---
                        "/livekit/webhook",

                        // --- media streaming test ---
                        "/media/test/**"
                );
    }

    /**
     * AuthorizedUserArgumentResolver를 스프링에 등록합니다.
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authorizedUserArgumentResolver);
    }
}
