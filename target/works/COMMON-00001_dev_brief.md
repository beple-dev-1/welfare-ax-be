# 개발 브리프 — COMMON-00001

> 작성일: 2026-06-21
> 과업번호: COMMON-00001
> 작성자: yukio_k

---

## 1. 과업 개요

**목적:** 복지AX-BE 전 업무 도메인이 공통으로 사용할 인프라 레이어를 구축한다.

**배경:**
- 멀티모듈 구조(`welfare-ax-common`, `welfare-ax-user` 등)로 전환 완료 후 실제 구현 파일이 없는 상태
- 경조사 신청 등 도메인 기능 개발 전에 공통 응답 포맷, 예외 처리, 보안 필터 체인을 먼저 확립해야 한다
- 로그인 API(토큰 발급)는 별도 과업(`LOGIN-xxxxx`)으로 분리하며, 이번 과업에서는 Security 설정(토큰 검증 필터)까지만 구현한다

---

## 2. 대상 도메인

- **common** (주 대상): `welfare-ax-common` 모듈 — ApiResponse, ErrorCode, GlobalExceptionHandler
- **user-infra** (부 대상): `welfare-ax-user/config`, `welfare-ax-user/security` — SecurityConfig, JwtProvider, JwtFilter

---

## 3. 기능 요구사항

### 3-1. 공통 응답 래퍼 (`welfare-ax-common/response/`)

- `ApiResponse<T>` — 모든 API 응답을 감싸는 공통 래퍼
- 응답 구조:
  ```json
  { "code": "0000", "message": "성공", "data": { ... } }
  ```
- 성공 코드: `"0000"`
- 에러 코드: `"Exxx"` 형식 (4자리, E+3자리 숫자)
  - `E001`~`E998`: 사전 정의된 에러 (공통 메시지 고정)
  - `E999`: 직접 메시지를 지정하는 기타 오류
  - 미정의 에러의 기본 메시지: `"기타 오류"`
- 정적 팩토리 메서드: `ApiResponse.success(data)`, `ApiResponse.error(errorCode)`, `ApiResponse.error(errorCode, message)`

### 3-2. 에러 코드 관리 (`welfare-ax-common/exception/`)

- `ErrorCode` enum — 코드·기본 메시지 쌍으로 관리
  - 예시: `E001("잘못된 요청입니다")`, `E002("리소스를 찾을 수 없습니다")`
  - `E999`: 기본 메시지 `"기타 오류"` (직접 메시지 오버라이드 가능)
- **다중언어(i18n) 확장 고려**: 메시지를 코드에 직접 하드코딩하지 않고 메시지 소스 키(`message.key`) 방식으로 확장 가능하도록 설계
- `WelfareException extends RuntimeException` — ErrorCode를 포함하는 공통 예외
- `GlobalExceptionHandler (@RestControllerAdvice)` — 전역 예외를 ApiResponse로 변환

예외 처리 HTTP 상태코드 매핑:
| 예외 유형 | HTTP 상태 |
|----------|----------|
| `@Valid` 검증 실패 (`MethodArgumentNotValidException`) | 400 |
| 리소스 없음 | 404 |
| 중복/충돌 | 409 |
| 비즈니스 규칙 위반 (`WelfareException`) | 422 |
| 인증 실패 | 401 |
| 권한 없음 | 403 |
| 서버 오류 (미처리 예외) | 500 |

### 3-3. JWT 유틸 (`welfare-ax-user/security/`)

- `JwtProvider` — JWT 토큰 생성·검증·파싱 유틸
  - 토큰 생성: 로그인 과업에서 사용 (이번 과업에서 인터페이스만 설계)
  - 토큰 검증: 서명 유효성, 만료 여부 확인
  - Claims 파싱: memberId, role 추출
- Access Token 만료 시간: 기본 12시간 (환경별 yml로 override)
- Refresh Token: 미사용

### 3-4. Security 설정 (`welfare-ax-user/config/`)

- `SecurityConfig` — Spring Security 6.x `SecurityFilterChain` Bean
  - CSRF 비활성화 (stateless API)
  - 세션: STATELESS
  - 공개 경로: `/api/v1/auth/**` (로그인 등)
  - 나머지 경로: 인증 필요
  - `JwtFilter`를 `UsernamePasswordAuthenticationFilter` 이전에 등록
- `JwtFilter extends OncePerRequestFilter` — 요청 헤더의 `Authorization: Bearer {token}` 검증

### 3-5. application.yaml 설정 추가

JWT 관련 설정을 환경별 yml에 추가:
```yaml
# application-local.yaml, application-dev.yaml
jwt:
  secret: (로컬/개발용 시크릿 직접 기재)
  expiration: 43200  # 12시간 (초 단위)

# application-prod.yaml (환경변수 참조)
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:43200}
```

---

## 4. 비기능 요구사항

| 항목 | 기준 |
|------|------|
| 보안 | JWT 시크릿 운영 환경에서 환경변수 주입 (Docker 배포) |
| 확장성 | 에러 메시지 다중언어(i18n) 확장 가능 구조 |
| 모듈 분리 | 공통 응답·예외는 `welfare-ax-common`, Security는 각 실행 모듈(`user/config`, `user/security`) |
| 관리자 확장 | `welfare-ax-common` 내 `user/`, `admin/` 패키지 분리 구조로 향후 관리자 공통 추가 대비 |

---

## 5. 대상 API/화면 목록

이번 과업에서 신규 외부 API 엔드포인트는 없음.
- `JwtFilter`가 모든 인증 필요 경로에서 토큰 검증을 수행
- 로그인 API (`POST /api/v1/auth/login`)는 별도 과업에서 구현

---

## 6. DB 변경 사항

없음. 이번 과업에서 Entity/테이블 추가 없음.

---

## 7. 연동 대상

없음. 외부 API 연동 없음.

---

## 8. 보안 고려사항

- JWT 시크릿: local/dev 환경은 yml 파일 기재, prod 환경은 `${JWT_SECRET}` 환경변수 주입
- Docker 배포 시 환경변수를 컨테이너에 주입 (docker-compose 또는 K8s Secret)
- `application-prod.yaml`은 코드·하네스에서 읽기·수정 금지
- 토큰 만료 처리: 만료된 토큰은 401 응답 반환
- 권한 없는 접근: 403 응답 반환
- PII(개인정보)를 JWT Claims에 포함하지 않음 (memberId, role만 포함)

---

## 9. 테스트 기준

| 구분 | 대상 | 방법 |
|------|------|------|
| 단위 | `ApiResponse` 생성 검증 | JUnit5 |
| 단위 | `ErrorCode` 코드·메시지 매핑 | JUnit5 |
| 단위 | `GlobalExceptionHandler` 예외별 응답 변환 | `@WebMvcTest` + MockMvc |
| 단위 | `JwtProvider` 토큰 생성·검증·만료 | JUnit5 + Mockito |
| 단위 | `JwtFilter` 토큰 추출·인증 처리 | JUnit5 + Mockito |
| 통합 | Security 필터 체인 공개/보호 경로 접근 | `@SpringBootTest` |

필수 테스트 케이스:
- `ApiResponse.success()` 코드 `0000` 반환 확인
- `ApiResponse.error(E999, "직접 메시지")` 메시지 오버라이드 확인
- 유효 토큰 → SecurityContext 인증 설정 확인
- 만료 토큰 → 401 응답 확인
- 토큰 없음 → 401 응답 확인 (보호 경로)
- 공개 경로(`/api/v1/auth/**`) → 토큰 없이 통과 확인

---

## 10. 산출물 위치

| 산출물 | 경로 |
|--------|------|
| 공통 응답 | `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/response/` |
| 공통 예외 | `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/exception/` |
| Security 설정 | `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/` |
| JWT 필터·유틸 | `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/security/` |
| 설정 파일 | `welfare-ax-user/src/main/resources/application-local.yaml` |
| 개발 브리프 | `target/works/COMMON-00001_dev_brief.md` |
| 개발 계획서 | `target/plans/COMMON-00001/` |
| 테스트 결과서 | `target/test-reports/COMMON-00001_test_result.md` |

---

## 11. 미결 사항

| 항목 | 내용 | 결정 시점 |
|------|------|----------|
| 에러 코드 정의 목록 | E001~E998 전체 목록 미정. 도메인 개발 시 점진적으로 추가 | 도메인 과업별 추가 |
| i18n 구현 방식 | 메시지 소스 키 방식 vs Spring MessageSource 연동 | 다중언어 요구사항 확정 시 |
| 관리자 공통 | `welfare-ax-common/admin/` 패키지 구현은 관리자 개발 과업에서 진행 | 관리자 과업 시작 시 |
| 로그인 API | `POST /api/v1/auth/login` (토큰 발급) 별도 과업 | LOGIN 과업 생성 시 |
| prod yml 시크릿 플레이스홀더 | `application-prod.yaml` 수정 필요하나 접근 금지 파일 — 배포 담당자가 직접 설정 | 운영 배포 시 |

---

## 부록: 질의응답 로그

| 질문 | 답변 |
|------|------|
| Q2. 공통 응답 포맷 | 코드형. 코드 4자리(0000/Exxx). E999=직접 메시지 기타 오류. 다중언어 고려 |
| Q3. 로그인 포함 여부 | 포함 → 별도 과업으로 변경. 이번 과업은 Security 필터(검증)만 |
| Q4. JWT 시크릿 관리 | local/dev: yml 직접 기재, prod: 환경변수. Docker 배포 |
| Q5. 관리자 공통 | 별도 체계, 현재 미고려. common 내 user/admin 패키지 분리 |
| Q6. 공통 분리 방식 | 모듈 분리 아닌 패키지 분리 (`welfare-ax-common` 내) |
| Q7. JWT 만료 시간 | Access Token 12시간 기본, 환경별 yml override. Refresh Token 미사용 |
| Q8. Member 도메인 | 로그인이 별도 과업이므로 이번에 제외 |
