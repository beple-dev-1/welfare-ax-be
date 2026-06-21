# 테스트 계획서

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00002 |
| 제목 | HTTP 공통 클라이언트 구현 테스트 계획 |
| 작성일 | 2026-06-21 |
| 대응 개발 계획서 | COMMON-00002_dev_plan.md |

---

## 1. 테스트 범위

| 대상 | 테스트 유형 |
|------|------------|
| `HttpLoggingInterceptor` — 로깅 및 Authorization 마스킹 | 단위 |
| `CommonHttpClient` — GET·POST·PUT·DELETE, 예외 변환, 타임아웃 | 단위 |
| `TestApiClient` — httpbin.org 모킹 연동 | 단위 (Mock) |
| `TestApiClient` — httpbin.org 실 호출 E2E | 통합 |

---

## 2. 테스트 방법

| 유형 | 도구 | 범위 |
|------|------|------|
| 단위 테스트 | JUnit 5 + Mockito | `HttpLoggingInterceptor`, `CommonHttpClient` |
| 슬라이스/Mock 테스트 | `MockRestServiceServer` | `CommonHttpClient`, `TestApiClient` |
| 통합 테스트 | `@SpringBootTest` + httpbin.org 실 호출 | `TestApiClient` E2E |

---

## 3. 테스트 환경

- DB: 없음 (HTTP 클라이언트 과업 — DB 의존 없음)
- 외부: httpbin.org (통합 테스트 시 네트워크 필요)
- 프로파일: `local` + `api` (`application-local.yaml`, `application-api.yaml` 모두 활성화)
- 통합 테스트 격리: `@Tag("integration")` — CI 환경에서 단위 테스트만 실행 시 제외 가능

---

## 4. 테스트케이스 목록

### HttpLoggingInterceptor 단위 테스트

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-C2-LOG-001 | Authorization 헤더 마스킹 — 값이 `****`로 로깅됨 | 단위 | 높음 | [ ] |
| TC-C2-LOG-002 | 일반 헤더는 마스킹 없이 로깅됨 | 단위 | 중 | [ ] |
| TC-C2-LOG-003 | 응답 상태코드 로깅 확인 | 단위 | 중 | [ ] |
| TC-C2-LOG-004 | 인터셉터 실행 후 응답 정상 반환 | 단위 | 높음 | [ ] |

### CommonHttpClient 단위 테스트

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-C2-HTTP-001 | GET 성공 — 200 응답 역직렬화 | 단위 | 높음 | [ ] |
| TC-C2-HTTP-002 | POST 성공 — 요청 Body JSON 직렬화 및 200 응답 역직렬화 | 단위 | 높음 | [ ] |
| TC-C2-HTTP-003 | PUT 성공 — 200 응답 역직렬화 | 단위 | 중 | [ ] |
| TC-C2-HTTP-004 | DELETE 성공 — 응답 없음 정상 처리 | 단위 | 중 | [ ] |
| TC-C2-HTTP-005 | GET 4xx 응답 → `WelfareException(EXTERNAL_API_ERROR)` 발생 | 단위 | 높음 | [ ] |
| TC-C2-HTTP-006 | GET 5xx 응답 → `WelfareException(EXTERNAL_API_ERROR)` 발생 | 단위 | 높음 | [ ] |
| TC-C2-HTTP-007 | 타임아웃 → `WelfareException(EXTERNAL_API_ERROR)` 발생 | 단위 | 높음 | [ ] |
| TC-C2-HTTP-008 | 호출부 전달 헤더가 요청에 포함되는지 확인 | 단위 | 중 | [ ] |
| TC-C2-HTTP-009 | Authorization 헤더를 CommonHttpClient 내부에서 생성하지 않음 확인 (코드 검토) | 코드리뷰 | 높음 | [ ] |

### TestApiClient 단위 테스트 (MockRestServiceServer)

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-C2-CLI-001 | GET /get 모킹 성공 — 응답 Map 반환 | 단위 | 높음 | [ ] |
| TC-C2-CLI-002 | POST /post 모킹 성공 — Body 직렬화 및 응답 반환 | 단위 | 높음 | [ ] |
| TC-C2-CLI-003 | PUT /put 모킹 성공 — Body 직렬화 및 응답 반환 | 단위 | 중 | [ ] |
| TC-C2-CLI-004 | DELETE /delete 모킹 성공 | 단위 | 중 | [ ] |

### TestApiClient 통합 테스트 (httpbin.org 실 호출)

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-C2-INT-001 | GET https://httpbin.org/get — 상태 200, url 필드 확인 | 통합 | 높음 | [ ] |
| TC-C2-INT-002 | POST https://httpbin.org/post — 상태 200, json 필드에 요청 body 포함 확인 | 통합 | 높음 | [ ] |
| TC-C2-INT-003 | PUT https://httpbin.org/put — 상태 200, json 필드 확인 | 통합 | 중 | [ ] |
| TC-C2-INT-004 | DELETE https://httpbin.org/delete — 상태 200, 정상 완료 | 통합 | 중 | [ ] |

---

## 5. 완료 기준 (DoD)

### 기능 검증
- [ ] 전체 TC GREEN (단위 17건 + 통합 4건)
- [ ] 정상 케이스 (Happy Path) TC 포함 ✓
- [ ] 경계값 케이스 TC 포함 (4xx·5xx·타임아웃) ✓
- [ ] 예외 케이스 TC 포함 (EXTERNAL_API_ERROR 변환) ✓

### 복지AX 도메인 필수 검증
- [ ] 복지혜택 지급 중복 방지 TC — 해당 없음 (HTTP 인프라 과업)
- [ ] 잔액 부족 예외 처리 TC — 해당 없음
- [ ] 권한 없는 접근 거부 TC — 해당 없음 (CommonHttpClient 인증 무관 설계)
- [ ] 미등록/정지 가맹점 처리 TC — 해당 없음

### 코드 품질
- [ ] `/code-review` 실행 후 CRITICAL 0건
- [ ] `CommonHttpClient` 내 Authorization·API Key·Basic Auth 생성 코드 없음 확인

### 보안
- [ ] Authorization 헤더 로그 마스킹 확인 (TC-C2-LOG-001)
- [ ] `application-api.yaml` 민감 값 코드 포함 없음 확인
- [ ] PII 로그 노출 없음 확인
