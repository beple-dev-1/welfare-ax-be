# 페이즈 1: traceId 상수화

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 1 |
| 목표 | `MdcConstants` 상수 클래스 생성, `TraceIdFilter`·`HttpLoggingInterceptor`의 `"traceId"` 하드코딩 제거 |
| 의존 페이즈 | 없음 |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|---------|---------|------|
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/util/MdcConstants.java` | 신규 생성 | traceId MDC 키 상수 정의 |
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/filter/TraceIdFilter.java` | 수정 | 내부 `TRACE_ID_KEY` → `MdcConstants` 참조 |
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/http/HttpLoggingInterceptor.java` | 수정 | `MDC.get("traceId")` → `MDC.get(MdcConstants.TRACE_ID_KEY)` |

---

## 상세 구현 가이드

### MdcConstants.java

```java
package com.beplepay.welfareaxbe.common.util;

/**
 * MDC(Mapped Diagnostic Context) 키 상수 클래스.
 *
 * <p>logback-spring.xml의 {@code %X{traceId:-NO_TRACE}} 패턴과
 * {@link com.beplepay.welfareaxbe.common.filter.TraceIdFilter},
 * {@link com.beplepay.welfareaxbe.common.http.HttpLoggingInterceptor}에서
 * 동일한 키를 사용하도록 단일 상수로 관리한다.
 */
public final class MdcConstants {

    /** TraceIdFilter가 MDC에 저장하는 traceId 키. logback 패턴 %X{traceId:-NO_TRACE}와 일치해야 한다. */
    public static final String TRACE_ID_KEY = "traceId";

    private MdcConstants() {}
}
```

### TraceIdFilter.java — 수정 사항

- **제거**: `static final String TRACE_ID_KEY = "traceId";` (34~35행, 기존 package-private 상수)
- **추가**: `import com.beplepay.welfareaxbe.common.util.MdcConstants;`
- **교체**: `MDC.put(TRACE_ID_KEY, traceId)` → `MDC.put(MdcConstants.TRACE_ID_KEY, traceId)`
- **교체**: `MDC.remove(TRACE_ID_KEY)` → `MDC.remove(MdcConstants.TRACE_ID_KEY)`
- Javadoc `{@value TRACE_ID_KEY}` 참조 → `{@code MdcConstants#TRACE_ID_KEY}` 또는 상수명 직접 명시로 수정
- `TRACE_ID_HEADER` 상수는 TraceIdFilter 전용이므로 그대로 유지

> **주의**: 기존 테스트 `TraceIdFilterTest`가 `TraceIdFilter.TRACE_ID_KEY`를 직접 참조하는 경우 `MdcConstants.TRACE_ID_KEY`로 변경 필요.

### HttpLoggingInterceptor.java — 수정 사항

- **추가**: `import com.beplepay.welfareaxbe.common.util.MdcConstants;`
- **교체**: `propagateTraceId()` 메서드 내 `MDC.get("traceId")` (88행) → `MDC.get(MdcConstants.TRACE_ID_KEY)`

---

## 완료 기준

- [ ] `MdcConstants.TRACE_ID_KEY` 값이 `"traceId"` 인지 단위 테스트로 검증
- [ ] `TraceIdFilter` — `"traceId"` 문자열 리터럴 0건 (상수 참조만 사용)
- [ ] `HttpLoggingInterceptor` — `"traceId"` 문자열 리터럴 0건
- [ ] `./gradlew :welfare-ax-common:test` 전체 통과

---

## 다음 페이즈 연결

Phase 2에서 `SwaggerConfig`는 `MdcConstants`와 직접 연관 없으나, Phase 1의 빌드 성공이 확인된 후 Phase 2를 진행한다.
