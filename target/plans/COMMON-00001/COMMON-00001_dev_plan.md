# 개발 계획서

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00001 |
| 제목 | 공통 인프라 구축 (ApiResponse, 예외처리, JWT, Security) |
| 작성일 | 2026-06-21 |
| 작성자 | AI (검토: yukio_k) |
| 대상 스코프 | common |
| 브랜치 | feature/COMMON-00001/gkwns458 |

---

## 1. 과업 개요

복지AX-BE 전 도메인이 공통으로 사용할 인프라 레이어를 구축한다.
- **공통 응답 포맷**: `ApiResponse<T>` 래퍼 + `ErrorCode` 에러 코드 체계
- **공통 예외 처리**: `WelfareException` + `GlobalExceptionHandler`
- **JWT 유틸**: 토큰 검증·파싱 (`JwtProvider`)
- **Security 설정**: Spring Security 6.x FilterChain + `JwtFilter`

로그인 API(토큰 발급)와 Member 도메인은 별도 과업으로 분리한다.

---

## 2. 기능 요구사항

- [ ] `ApiResponse<T>` — 코드(`0000`/`Exxx`), 메시지, 데이터를 포함하는 공통 응답 래퍼
- [ ] `ErrorCode` enum — 에러 코드·메시지 쌍 관리, 다중언어(i18n) 확장 고려 구조
- [ ] `WelfareException` — `ErrorCode`를 포함하는 공통 런타임 예외
- [ ] `GlobalExceptionHandler` — 전역 예외를 `ApiResponse`로 변환 (`@RestControllerAdvice`)
- [ ] `JwtProvider` — JWT 토큰 검증·파싱·클레임 추출
- [ ] JWT 설정 — 환경별 yml에 secret·expiration 설정 (local/dev: 직접, prod: 환경변수)
- [ ] `SecurityConfig` — Spring Security 6.x `SecurityFilterChain` Bean
- [ ] `JwtFilter` — `Authorization: Bearer` 헤더에서 토큰 추출 및 SecurityContext 설정

---

## 3. 대상 스코프 및 허용 경로

`scope.yaml` 기준 `common` 스코프 허용 경로:

```
welfare-ax-common/src/main/java/**
welfare-ax-common/src/test/java/**
welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/**
welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/security/**
welfare-ax-user/src/main/resources/application.yaml
welfare-ax-user/src/main/resources/application-local.yaml
```

---

## 4. 페이즈 목록

| 페이즈 | 제목 | 대상 파일 | 완료 |
|--------|------|----------|------|
| 1 | 공통 응답·예외 인프라 | `ApiResponse`, `ErrorCode`, `WelfareException`, `GlobalExceptionHandler` | [ ] |
| 2 | JWT 유틸 및 설정 | `JwtProvider`, `application-local.yaml` 설정 추가 | [ ] |
| 3 | Security 필터 체인 | `SecurityConfig`, `JwtFilter` | [ ] |

---

## 5. DB 변경사항

없음. 이번 과업에서 Entity/테이블 추가 없음.

---

## 6. 공통 모듈 변경사항

### welfare-ax-common 신규 생성 파일

```
com.beplepay.welfareaxbe.common
├── response/
│   └── ApiResponse.java
└── exception/
    ├── ErrorCode.java
    ├── WelfareException.java
    └── GlobalExceptionHandler.java
```

### welfare-ax-user 신규 생성 파일

```
com.beplepay.welfareaxbe.user
├── config/
│   └── SecurityConfig.java
└── security/
    ├── JwtProvider.java
    └── JwtFilter.java
```

---

## 7. 보안 고려사항

- JWT 시크릿: `application-local.yaml`, `application-dev.yaml`에 직접 기재 (로컬/개발)
- prod 환경: `${JWT_SECRET}` 환경변수 주입 (Docker 배포 시 컨테이너 환경변수로 주입)
- `application-prod.yaml`은 하네스 보안 정책상 접근 금지 — 배포 담당자가 직접 관리
- JWT Claims에 PII(개인정보) 포함 금지 — `memberId`, `role`만 포함
- 만료·서명 오류 토큰: 401 응답 반환
- SecurityContext에 인증 정보 설정 후 다음 필터로 전달

---

## 8. 위험 요소 및 대응

| 위험 | 대응 |
|------|------|
| Spring Boot 4.x에서 `@EntityScan` 패키지 이동 | Phase 1에서는 Entity 없으므로 해당 없음. 첫 Entity 추가 시 별도 확인 |
| `GlobalExceptionHandler`가 common 모듈에 있을 때 user 모듈에서 스캔 여부 | `@SpringBootApplication(scanBasePackages = "com.beplepay.welfareaxbe")`로 전체 스캔 설정 완료 |
| Security 설정 없을 때 모든 경로가 차단될 수 있음 | Phase 3에서 `SecurityConfig` 완성 전 테스트 시 `permitAll()` 임시 설정 사용 |
| JWT 시크릿 노출 | local/dev 시크릿은 실제 운영 시크릿과 다른 임의 문자열 사용 |

---

## 9. 테스트 전략

| 유형 | 도구 | 범위 |
|------|------|------|
| 단위 | JUnit5 + Mockito | `ApiResponse`, `ErrorCode`, `WelfareException`, `JwtProvider` |
| 슬라이스 | `@WebMvcTest` + MockMvc | `GlobalExceptionHandler` 예외 응답 변환 |
| 슬라이스 | `@WebMvcTest` + Spring Security | `JwtFilter` 토큰 추출·인증 처리 |
| 통합 | `@SpringBootTest` | Security 필터 체인 공개/보호 경로 접근 |

---

## 10. 예상 산출물

### 신규 생성 파일

| 파일 | 모듈 | 페이즈 |
|------|------|--------|
| `common/response/ApiResponse.java` | welfare-ax-common | 1 |
| `common/exception/ErrorCode.java` | welfare-ax-common | 1 |
| `common/exception/WelfareException.java` | welfare-ax-common | 1 |
| `common/exception/GlobalExceptionHandler.java` | welfare-ax-common | 1 |
| `user/security/JwtProvider.java` | welfare-ax-user | 2 |
| `user/config/SecurityConfig.java` | welfare-ax-user | 3 |
| `user/security/JwtFilter.java` | welfare-ax-user | 3 |

### 수정 파일

| 파일 | 모듈 | 변경 내용 | 페이즈 |
|------|------|----------|--------|
| `application-local.yaml` | welfare-ax-user | jwt.secret, jwt.expiration 추가 | 2 |

### 테스트 파일 (신규 생성)

| 파일 | 모듈 | 페이즈 |
|------|------|--------|
| `common/response/ApiResponseTest.java` | welfare-ax-common | 1 |
| `common/exception/GlobalExceptionHandlerTest.java` | welfare-ax-common | 1 |
| `user/security/JwtProviderTest.java` | welfare-ax-user | 2 |
| `user/security/JwtFilterTest.java` | welfare-ax-user | 3 |
