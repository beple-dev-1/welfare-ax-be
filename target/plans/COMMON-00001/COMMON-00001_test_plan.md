# 테스트 계획서

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00001 |
| 제목 | 공통 인프라 구축 테스트 계획 |
| 작성일 | 2026-06-21 |
| 대응 개발 계획서 | COMMON-00001_dev_plan.md |

---

## 1. 테스트 범위

| 대상 | 유형 |
|------|------|
| `ApiResponse` 생성 및 직렬화 | 단위 |
| `ErrorCode` 코드·메시지 매핑 | 단위 |
| `WelfareException` 생성 및 메시지 전달 | 단위 |
| `GlobalExceptionHandler` 예외 → 응답 변환 | 슬라이스 |
| `JwtProvider` 토큰 생성·검증·파싱 | 단위 |
| `JwtFilter` 토큰 추출 및 SecurityContext 설정 | 슬라이스 |
| Security 필터 체인 공개/보호 경로 접근 | 통합 |

---

## 2. 테스트 방법

| 유형 | 도구 | 범위 |
|------|------|------|
| 단위 테스트 | JUnit5 + Mockito | `ApiResponse`, `ErrorCode`, `WelfareException`, `JwtProvider` |
| 슬라이스 테스트 | `@WebMvcTest` + MockMvc + `@WithMockUser` | `GlobalExceptionHandler`, `JwtFilter` |
| 통합 테스트 | `@SpringBootTest` + MockMvc | Security 필터 체인 E2E |

---

## 3. 테스트 환경

- DB 불필요: 이번 과업에서 Entity/JPA 없음
- Spring Boot 테스트 슬라이스 (`@WebMvcTest`) 활용
- JWT 시크릿: 테스트용 고정 문자열 (`test-secret-key-for-unit-test-at-least-32-chars`) 사용

---

## 4. 테스트케이스 목록

### Phase 1 — 공통 응답·예외

---

#### TC-COMMON-00001-COMMON-001: 성공 응답 생성

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | 없음 |
| 입력 | `ApiResponse.success("test-data")` |
| 기대결과 | code=`"0000"`, message=`"성공"`, data=`"test-data"` |
| 대응 개발 항목 | phase_1.md — ApiResponse |

---

#### TC-COMMON-00001-COMMON-002: 성공 응답 (data 없음)

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 중간 |
| 전제조건 | 없음 |
| 입력 | `ApiResponse.success()` |
| 기대결과 | code=`"0000"`, message=`"성공"`, data=`null` |
| 대응 개발 항목 | phase_1.md — ApiResponse |

---

#### TC-COMMON-00001-COMMON-003: 에러 응답 — 정의된 에러코드

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | 없음 |
| 입력 | `ApiResponse.error(ErrorCode.NOT_FOUND)` |
| 기대결과 | code=`"E002"`, message=`"리소스를 찾을 수 없습니다"`, data=`null` |
| 대응 개발 항목 | phase_1.md — ApiResponse, ErrorCode |

---

#### TC-COMMON-00001-COMMON-004: 에러 응답 — E999 직접 메시지

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | 없음 |
| 입력 | `ApiResponse.error(ErrorCode.UNKNOWN, "주문번호가 없습니다")` |
| 기대결과 | code=`"E999"`, message=`"주문번호가 없습니다"` |
| 대응 개발 항목 | phase_1.md — ApiResponse, ErrorCode |

---

#### TC-COMMON-00001-COMMON-005: WelfareException 기본 메시지

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 중간 |
| 전제조건 | 없음 |
| 입력 | `new WelfareException(ErrorCode.NOT_FOUND)` |
| 기대결과 | `getMessage()` = `"리소스를 찾을 수 없습니다"`, `getErrorCode()` = `ErrorCode.NOT_FOUND` |
| 대응 개발 항목 | phase_1.md — WelfareException |

---

#### TC-COMMON-00001-COMMON-006: GlobalExceptionHandler — 입력값 검증 실패

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest`) |
| 우선순위 | 높음 |
| 전제조건 | `@Valid` 적용된 테스트 컨트롤러 stub |
| 입력 | 필수 필드 누락 요청 |
| 기대결과 | HTTP 400, body `{"code":"E001","message":"잘못된 요청입니다"}` |
| 대응 개발 항목 | phase_1.md — GlobalExceptionHandler |

---

#### TC-COMMON-00001-COMMON-007: GlobalExceptionHandler — WelfareException(NOT_FOUND)

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest`) |
| 우선순위 | 높음 |
| 전제조건 | `WelfareException(NOT_FOUND)` 던지는 테스트 컨트롤러 stub |
| 입력 | GET 요청 |
| 기대결과 | HTTP 404, body `{"code":"E002","message":"리소스를 찾을 수 없습니다"}` |
| 대응 개발 항목 | phase_1.md — GlobalExceptionHandler |

---

#### TC-COMMON-00001-COMMON-008: GlobalExceptionHandler — 미처리 예외

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest`) |
| 우선순위 | 높음 |
| 전제조건 | 미처리 `RuntimeException` 던지는 테스트 컨트롤러 stub |
| 입력 | GET 요청 |
| 기대결과 | HTTP 500, body `{"code":"E999","message":"기타 오류"}` |
| 대응 개발 항목 | phase_1.md — GlobalExceptionHandler |

---

### Phase 2 — JWT 유틸

---

#### TC-COMMON-00001-COMMON-009: JWT 토큰 생성 형식 확인

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | `JwtProvider` 인스턴스, secret/expiration 주입 |
| 입력 | `generateToken(1L, "USER")` |
| 기대결과 | `"."` 2개 포함하는 JWT 형식 문자열 반환 |
| 대응 개발 항목 | phase_2.md — JwtProvider |

---

#### TC-COMMON-00001-COMMON-010: 유효 토큰 검증

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | 방금 생성한 토큰 |
| 입력 | `validateToken(유효한 토큰)` |
| 기대결과 | `true` |
| 대응 개발 항목 | phase_2.md — JwtProvider |

---

#### TC-COMMON-00001-COMMON-011: 만료된 토큰 검증

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | expiration=0 또는 과거 시간으로 생성한 토큰 |
| 입력 | `validateToken(만료된 토큰)` |
| 기대결과 | `false` |
| 대응 개발 항목 | phase_2.md — JwtProvider |

---

#### TC-COMMON-00001-COMMON-012: 잘못된 서명 토큰 검증

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | 다른 시크릿으로 생성한 토큰 |
| 입력 | `validateToken(서명 불일치 토큰)` |
| 기대결과 | `false` |
| 대응 개발 항목 | phase_2.md — JwtProvider |

---

#### TC-COMMON-00001-COMMON-013: Claims 파싱 — memberId, role

| 항목 | 내용 |
|------|------|
| 유형 | 단위 |
| 우선순위 | 높음 |
| 전제조건 | `generateToken(42L, "USER")`로 생성한 토큰 |
| 입력 | `getMemberId(token)`, `getRole(token)` |
| 기대결과 | `getMemberId()` = `42L`, `getRole()` = `"USER"` |
| 대응 개발 항목 | phase_2.md — JwtProvider |

---

### Phase 3 — Security 필터 체인

---

#### TC-COMMON-00001-COMMON-014: 공개 경로 — 토큰 없이 접근 허용

| 항목 | 내용 |
|------|------|
| 유형 | 통합 (`@SpringBootTest`) |
| 우선순위 | 높음 |
| 전제조건 | 서버 구동, `/api/v1/auth/test` 공개 stub 엔드포인트 |
| 입력 | `GET /api/v1/auth/test` — Authorization 헤더 없음 |
| 기대결과 | HTTP 200 |
| 대응 개발 항목 | phase_3.md — SecurityConfig |

---

#### TC-COMMON-00001-COMMON-015: 보호 경로 — 유효 토큰 접근 허용

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest` + `@WithMockUser`) |
| 우선순위 | 높음 |
| 전제조건 | 보호 stub 엔드포인트, JwtProvider mock으로 토큰 검증 `true` 반환 |
| 입력 | `GET /api/v1/protected` + `Authorization: Bearer {유효한 토큰}` |
| 기대결과 | HTTP 200, SecurityContext에 인증 설정됨 |
| 대응 개발 항목 | phase_3.md — JwtFilter, SecurityConfig |

---

#### TC-COMMON-00001-COMMON-016: 보호 경로 — 토큰 없음

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest`) |
| 우선순위 | 높음 |
| 전제조건 | 보호 stub 엔드포인트 |
| 입력 | `GET /api/v1/protected` — Authorization 헤더 없음 |
| 기대결과 | HTTP 401, body `{"code":"E005","message":"인증이 필요합니다"}` |
| 대응 개발 항목 | phase_3.md — SecurityConfig (AuthenticationEntryPoint) |

---

#### TC-COMMON-00001-COMMON-017: 보호 경로 — 만료 토큰

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest`) |
| 우선순위 | 높음 |
| 전제조건 | JwtProvider mock으로 `validateToken()` = `false` 반환 |
| 입력 | `GET /api/v1/protected` + `Authorization: Bearer {만료 토큰}` |
| 기대결과 | HTTP 401 |
| 대응 개발 항목 | phase_3.md — JwtFilter |

---

#### TC-COMMON-00001-COMMON-018: 보호 경로 — 잘못된 Bearer 형식

| 항목 | 내용 |
|------|------|
| 유형 | 슬라이스 (`@WebMvcTest`) |
| 우선순위 | 중간 |
| 전제조건 | 보호 stub 엔드포인트 |
| 입력 | `GET /api/v1/protected` + `Authorization: Token {토큰}` (Bearer 아님) |
| 기대결과 | HTTP 401 |
| 대응 개발 항목 | phase_3.md — JwtFilter |

---

## 5. 완료 기준 (DoD)

- [ ] TC-COMMON-00001-COMMON-001 ~ 018 전체 GREEN
- [ ] `/code-review` 실행 후 CRITICAL 0건
- [ ] 경계값 케이스 포함 확인 (만료 토큰, 서명 불일치, 헤더 없음)
- [ ] E999 직접 메시지 오버라이드 케이스 포함 확인
- [ ] PII(개인정보) JWT Claims 포함 여부 확인
