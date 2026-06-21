package com.beplepay.welfareaxbe.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger(OpenAPI 3.0) 설정 클래스.
 *
 * <p>JWT Bearer 인증 스키마를 전역으로 등록하여 Swagger UI의 Authorize 버튼을 통해
 * 모든 API에 {@code Authorization: Bearer {token}} 헤더를 자동 추가할 수 있다.
 * 로그인 API 구현 전에도 외부에서 발급받은 토큰으로 API를 테스트할 수 있도록 사전 구성한다.
 *
 * <p>Swagger UI 활성화는 {@code springdoc.swagger-ui.enabled} 설정으로 환경별 제어한다.
 * 운영 환경에서는 반드시 {@code false}로 설정해야 한다.
 */
@Configuration
public class SwaggerConfig {

    /** SecurityScheme 이름. SecurityConfig의 JwtFilter와 동일한 Bearer 형식을 참조한다. */
    private static final String BEARER_AUTH = "BearerAuth";

    /**
     * OpenAPI 스펙을 정의한다.
     * JWT Bearer 인증 스키마를 전역 Security Requirement로 등록하여
     * Swagger UI에서 Authorize 버튼으로 토큰을 입력하면 모든 API에 자동 적용된다.
     *
     * @return 설정된 OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증 스키마 정의 (HTTP Bearer, bearerFormat=JWT)
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 API에서 발급받은 JWT 액세스 토큰을 입력하세요. (Bearer 접두사 제외)");

        // 전체 API에 BearerAuth를 기본 Security Requirement로 적용
        SecurityRequirement globalSecurity = new SecurityRequirement().addList(BEARER_AUTH);

        return new OpenAPI()
                .info(new Info()
                        .title("welfare-ax-user API")
                        .version("0.0.1")
                        .description("비플페이 복지AX 사용자 API")
                        .contact(new Contact()
                                .name("beplepay")
                                .email("support@beple.co.kr")))
                .addSecurityItem(globalSecurity)
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme));
    }
}
