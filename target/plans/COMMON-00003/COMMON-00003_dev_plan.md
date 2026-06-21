# 개발 계획서 — COMMON-00003

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00003 |
| 작성일 | 2026-06-21 |
| 스코프 | common |
| 브랜치 | feature/COMMON-00003/gkwns458 |
| 브리프 | `target/works/COMMON-00003_dev_brief.md` |

---

## 1. 과업 목표

모든 HTTP 인바운드 요청에 서버 생성 UUID 기반 traceId를 부여한다.
traceId를 MDC에 저장하여 logback 패턴에 자동 포함시키고, 응답 헤더 및 외부 API 호출에도 전파하여
단일 traceId로 전체 요청 처리 흐름을 추적 가능하게 한다.

---

## 2. 구현 페이즈

| 페이즈 | 제목 | 주요 작업 | 대상 모듈 |
|--------|------|-----------|-----------|
| Phase 1 | TraceIdFilter 구현 | 필터 구현 + 단위 테스트 | welfare-ax-common |
| Phase 2 | HttpLoggingInterceptor traceId 전파 | 외부 API 요청 헤더 전파 + 테스트 | welfare-ax-common |
| Phase 3 | logback-spring.xml 구성 | traceId 포함 로그 패턴 설정 | welfare-ax-user |

---

## 3. 대상 파일

### 신규 파일

| 파일 경로 | 모듈 | Phase |
|----------|------|-------|
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/filter/TraceIdFilter.java` | common | 1 |
| `welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/filter/TraceIdFilterTest.java` | common | 1 |
| `welfare-ax-user/src/main/resources/logback-spring.xml` | user | 3 |

### 수정 파일

| 파일 경로 | 모듈 | Phase | 변경 내용 |
|----------|------|-------|-----------|
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/http/HttpLoggingInterceptor.java` | common | 2 | MDC traceId → 외부 요청 X-Trace-Id 헤더 전파 |
| `welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/http/HttpLoggingInterceptorTest.java` | common | 2 | traceId 전파 테스트 TC05·TC06 추가 |

---

## 4. 핵심 설계 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| traceId 생성 | 서버 UUID (`UUID.randomUUID()`) | 클라이언트 위조 방지, 신뢰성 보장 |
| 필터 등록 | `@Component` + `@Order(HIGHEST_PRECEDENCE)` | Security 체인과 무관, admin·batch 재사용 가능 |
| MDC 키 | `traceId` | logback `%X{traceId}` 참조 |
| 외부 전파 | `HttpRequestWrapper`로 헤더 추가 | ClientHttpRequestInterceptor 헤더 불변성 우회 |
| logback 미존재 시 | `%X{traceId:-NO_TRACE}` 기본값 | traceId 없는 환경에서도 패턴 깨지지 않음 |

---

## 5. 의존성 변경

없음 — 신규 라이브러리 추가 없이 `slf4j-api`(기존 의존), `jakarta.servlet-api`(webmvc 전이)로 구현 가능

---

## 6. 완료 기준 (DoD)

- [ ] TraceIdFilter 단위 테스트 4건 통과
- [ ] HttpLoggingInterceptorTest traceId 전파 테스트 2건 통과
- [ ] `/code-review` CRITICAL 0건
- [ ] 로그 출력 시 `[traceId]` 필드 포함 확인
- [ ] 응답 헤더 `X-Trace-Id` 반환 확인
