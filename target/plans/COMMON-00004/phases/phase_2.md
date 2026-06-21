# 페이즈 2: Swagger 인프라 구성

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 2 |
| 목표 | springdoc 의존성 추가, SwaggerConfig(JWT Bearer) 생성, SecurityConfig에 Swagger 경로 허용 |
| 의존 페이즈 | Phase 1 (빌드 성공 확인 후 진행) |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|---------|---------|------|
| `welfare-ax-user/build.gradle.kts` | 수정 | springdoc-openapi 의존성 추가 ⚠️ 스코프 외 |
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/SwaggerConfig.java` | 신규 생성 | OpenAPI 메타정보 + BearerAuth SecurityScheme |
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/SecurityConfig.java` | 수정 | Swagger 경로 permitAll 추가 |

---

## 상세 구현 가이드

### build.gradle.kts — 의존성 추가

`welfare-ax-user/build.gradle.kts`의 `dependencies { }` 블록에 추가:

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}")
```

> **⚠️ 버전 확인 필수 (OPEN-01)**: Spring Boot 4.1.0과 호환되는 springdoc-openapi 버전을 Maven Central에서 확인한다.
> 추가 후 `./gradlew :welfare-ax-user:dependencies --configuration compileClasspath` 로 의존성 충돌 여부를 검증한다.
> Spring Boot 4.x는 Spring Framework 7.x 기반이므로 springdoc 3.x 이상이 필요할 수 있다.

### SwaggerConfig.java

```java
package com.beplepay.welfareaxbe.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI 3.0) 설정 클래스.
 *
 * <p>JWT Bearer 인증 스키마를 전역으로 등록하여 Swagger UI의 Authorize 버튼을 통해
 * 모든 API에 {@code Authorization: Bearer {token}} 헤더를 자동 추가할 수 있다.
 * 로그인 API가 구현되기 전에도 외부에서 발급받은 토큰으로 API를 테스트할 수 있도록
 * 사전 구성한다.
 *
 * <p>Swagger UI는 {@code springdoc.swagger-ui.enabled} 설정으로 환경별 활성화를 제어한다.
 * 운영 환경에서는 반드시 {@code false}로 설정해야 한다.
 */
@Configuration
public class SwaggerConfig {

    /** Swagger BearerAuth SecurityScheme 이름. SecurityConfig의 JwtFilter와 동일한 Bearer 형식을 사용한다. */
    private static final String BEARER_AUTH = "BearerAuth";

    /**
     * OpenAPI 스펙을 정의한다.
     * JWT Bearer 인증 스키마를 전역 Security Requirement로 등록하여
     * UI에서 Authorize 버튼으로 토큰을 입력하면 모든 API에 자동 적용된다.
     *
     * @return 설정된 OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증 스키마 정의
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
                                .email("yukio_k@beple.co.kr")))
                .addSecurityItem(globalSecurity)
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme));
    }
}
```

### SecurityConfig.java — Swagger 경로 허용

기존 `requestMatchers` 블록에 Swagger 경로를 추가한다:

```java
// 기존
.requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()

// 변경 후
.requestMatchers(
    "/api/v1/auth/**",
    "/actuator/health",
    // Swagger UI 접근 허용 — springdoc 비활성화 환경(운영)에서는 404로 차단됨
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
).permitAll()
```

---

## 완료 기준

- [ ] `./gradlew :welfare-ax-user:dependencies` 실행 시 의존성 충돌 없음
- [ ] `GET /v3/api-docs` 200 OK 반환 확인 (애플리케이션 기동 후)
- [ ] OpenAPI JSON에 `components.securitySchemes.BearerAuth` 포함 확인
- [ ] `GET /swagger-ui/index.html` 인증 없이 200 OK 반환 확인
- [ ] `./gradlew :welfare-ax-user:test` 기존 테스트 전체 통과

---

## 다음 페이즈 연결

Phase 3에서 `application-local.yaml`·`application-dev.yaml`에 springdoc 활성화 설정을 추가하고 전체 빌드를 검증한다.
