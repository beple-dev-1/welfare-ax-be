package com.beplepay.welfareaxbe.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SwaggerConfig 단위 테스트.
 *
 * <p>OpenAPI 빈의 메타정보, JWT BearerAuth SecurityScheme 설정,
 * 전역 Security Requirement 포함 여부를 검증한다.
 */
class SwaggerConfigTest {

    private OpenAPI openAPI;

    @BeforeEach
    void setUp() {
        openAPI = new SwaggerConfig().openAPI();
    }

    @Test
    void openAPI_메타정보_검증() {
        // title, version 기본값 설정 여부 확인
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("welfare-ax-user API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("0.0.1");
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("beplepay");
    }

    @Test
    void openAPI_BearerAuth_securityScheme_등록() {
        // BearerAuth SecurityScheme이 components에 등록되어야 한다
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("BearerAuth");

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("BearerAuth");
        // JWT Bearer 스키마 설정값 검증
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void openAPI_전역_security_BearerAuth_적용() {
        // 전역 Security Requirement에 BearerAuth가 포함되어야 한다
        assertThat(openAPI.getSecurity()).isNotNull().isNotEmpty();
        assertThat(openAPI.getSecurity().get(0)).containsKey("BearerAuth");
    }
}
