package com.inhacapstone04.embersentinelserver.security.config;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUserArgumentResolver;
import com.inhacapstone04.embersentinelserver.security.interceptor.AuthInterceptor;
import com.inhacapstone04.embersentinelserver.security.interceptor.DeviceAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final DeviceAuthInterceptor deviceAuthInterceptor;
    private final AuthorizedUserArgumentResolver authorizedUserArgumentResolver;

    /**
     * CORS 설정
     * http://127.0.0.1:5500 (VSCode Live Server) 등 프론트엔드에서의 접근 허용
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 모든 Origin 허용 (http://127.0.0.1:5500 포함)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 쿠키, 인증 헤더 포함 허용
                .maxAge(3600);
    }

    /**
     * 인터셉터를 스프링에 등록합니다.
     * - AuthInterceptor: JWT 기반 사용자 인증 (모든 경로, 일부 제외)
     * - DeviceAuthInterceptor: API Key 기반 디바이스 인증 (/embedded/** 전용)
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. JWT 사용자 인증 인터셉터
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
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

                        // --- 엣지 디바이스 (DeviceAuthInterceptor가 별도 처리) ---
                        "/embedded/**",

                        // --- webhook ---
                        "/livekit/webhook",

                        // --- media streaming test ---
                        "/media/test/**"
                );

        // 2. 디바이스 API Key 인증 인터셉터 (/embedded/** 전용)
        registry.addInterceptor(deviceAuthInterceptor)
                .addPathPatterns("/embedded/**");
    }

    /**
     * AuthorizedUserArgumentResolver를 스프링에 등록합니다.
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authorizedUserArgumentResolver);
    }
}
