package com.inhacapstone04.embersentinelserver.security.config;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUserArgumentResolver;
import com.inhacapstone04.embersentinelserver.security.interceptor.AuthInterceptor;
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
