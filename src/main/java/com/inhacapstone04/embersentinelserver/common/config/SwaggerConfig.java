package com.inhacapstone04.embersentinelserver.common.config;

import com.inhacapstone04.embersentinelserver.common.resolver.AuthorizedUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // 1. @AuthorizedUser 어노테이션이 붙은 파라미터는 Swagger 입력 필드에서 제외합니다.
    //    (이 설정이 없으면 requestingUserId를 직접 숫자로 입력하는 폼이 생성됩니다.)
    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthorizedUser.class);
    }

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "BearerAuth";

        // 2. 모든 API 요청에 헤더 인증을 적용
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        // 3. SecurityScheme 등록 (JWT Bearer Token 방식 정의)
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("발급받은 Access Token을 입력해주세요. (Bearer 없이 토큰 값만 입력)"));

        return new OpenAPI()
                .info(new Info()
                        .title("Ember Sentinel API Server")
                        .description("Ember Sentinel 프로젝트 API 명세서입니다.")
                        .version("v1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
