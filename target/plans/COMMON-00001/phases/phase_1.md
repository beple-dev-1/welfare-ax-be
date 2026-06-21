# 페이즈 1: 공통 응답·예외 인프라

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 1 |
| 목표 | ApiResponse 래퍼, ErrorCode 에러 체계, WelfareException, GlobalExceptionHandler 구현 |
| 의존 페이즈 | 없음 |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|----------|----------|------|
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/response/ApiResponse.java` | 신규 생성 | 공통 응답 래퍼 |
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/exception/ErrorCode.java` | 신규 생성 | 에러 코드 enum |
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/exception/WelfareException.java` | 신규 생성 | 공통 런타임 예외 |
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/exception/GlobalExceptionHandler.java` | 신규 생성 | 전역 예외 처리 |
| `welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/response/ApiResponseTest.java` | 신규 생성 | ApiResponse 단위 테스트 |
| `welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/exception/GlobalExceptionHandlerTest.java` | 신규 생성 | 예외 처리 슬라이스 테스트 |

---

## 상세 구현 가이드

### ApiResponse.java

- **제네릭**: `ApiResponse<T>` — data 타입 유연
- **필드**: `code` (String), `message` (String), `data` (T)
- **Lombok**: `@Getter`, `@NoArgsConstructor(access = PROTECTED)` — 직렬화 및 상속 대비
- **정적 팩토리**:
  - `success(T data)` → code `"0000"`, message `"성공"`, data
  - `success()` → code `"0000"`, message `"성공"`, data `null`
  - `error(ErrorCode errorCode)` → code, 기본 메시지, data `null`
  - `error(ErrorCode errorCode, String message)` → code, 직접 지정 메시지 (E999용), data `null`
- **직렬화 주의**: `@JsonInclude(NON_NULL)`으로 data가 null이면 JSON에서 생략 고려

```java
// 구조 예시
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success() { ... }
    public static <T> ApiResponse<T> error(ErrorCode errorCode) { ... }
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) { ... }
}
```

### ErrorCode.java

- **enum 필드**: `code` (String), `defaultMessage` (String)
- **코드 체계**: `"0000"` 성공, `"E001"`~`"E998"` 정의된 에러, `"E999"` 기타 오류
- **i18n 확장 고려**: `messageKey` 필드 추가 (예: `"error.e001"`) — 향후 `MessageSource` 연동 시 사용
- **초기 에러 코드 정의** (추후 도메인 개발 시 추가):

| 코드 | 기본 메시지 | 용도 |
|------|------------|------|
| `E001` | 잘못된 요청입니다 | 입력값 검증 실패 (400) |
| `E002` | 리소스를 찾을 수 없습니다 | 조회 실패 (404) |
| `E003` | 이미 존재하는 데이터입니다 | 중복 (409) |
| `E004` | 비즈니스 규칙 위반입니다 | 도메인 규칙 위반 (422) |
| `E005` | 인증이 필요합니다 | 미인증 (401) |
| `E006` | 접근 권한이 없습니다 | 권한 없음 (403) |
| `E999` | 기타 오류 | 직접 메시지 지정 (500) |

```java
// 구조 예시
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT("E001", "잘못된 요청입니다"),
    NOT_FOUND("E002", "리소스를 찾을 수 없습니다"),
    DUPLICATE("E003", "이미 존재하는 데이터입니다"),
    BUSINESS_RULE_VIOLATION("E004", "비즈니스 규칙 위반입니다"),
    UNAUTHORIZED("E005", "인증이 필요합니다"),
    FORBIDDEN("E006", "접근 권한이 없습니다"),
    UNKNOWN("E999", "기타 오류");

    private final String code;
    private final String defaultMessage;
}
```

### WelfareException.java

- `RuntimeException` 확장
- `ErrorCode errorCode` + `String message` (선택) 필드
- 생성자:
  - `WelfareException(ErrorCode errorCode)` — errorCode의 기본 메시지 사용
  - `WelfareException(ErrorCode errorCode, String message)` — E999용, 직접 메시지 지정

```java
// 구조 예시
@Getter
public class WelfareException extends RuntimeException {
    private final ErrorCode errorCode;

    public WelfareException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public WelfareException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

### GlobalExceptionHandler.java

- `@RestControllerAdvice` + `@ExceptionHandler`
- **처리 대상 및 HTTP 상태코드**:

| 예외 | 상태 | 에러코드 |
|------|------|---------|
| `MethodArgumentNotValidException` | 400 | E001 |
| `WelfareException(E002)` 계열 | 404 | 해당 ErrorCode |
| `WelfareException(E003)` 계열 | 409 | 해당 ErrorCode |
| `WelfareException(E004)` 계열 | 422 | 해당 ErrorCode |
| `WelfareException(E005)` | 401 | E005 |
| `WelfareException(E006)` | 403 | E006 |
| `WelfareException(E999)` | 500 | E999 (직접 메시지) |
| `WelfareException` (기타) | 422 | 해당 ErrorCode |
| `Exception` (미처리) | 500 | E999 |

- `WelfareException`의 HTTP 상태 결정: `ErrorCode` → HttpStatus 매핑 (`errorCode.getHttpStatus()`) 방식 또는 핸들러 내 분기 처리
- **주의**: `ErrorCode` enum에 `HttpStatus` 필드를 추가하는 것이 확장성에 유리

---

## 완료 기준

- [ ] `ApiResponse.success(data)` → `{"code":"0000","message":"성공","data":{...}}` 직렬화 확인
- [ ] `ApiResponse.error(UNKNOWN, "직접 메시지")` → `{"code":"E999","message":"직접 메시지"}` 확인
- [ ] `GlobalExceptionHandler` — `MethodArgumentNotValidException` → 400 + E001 응답 확인
- [ ] `GlobalExceptionHandler` — `WelfareException(NOT_FOUND)` → 404 + E002 응답 확인
- [ ] `GlobalExceptionHandler` — 미처리 `Exception` → 500 + E999 응답 확인
- [ ] 단위 테스트 통과

---

## 다음 페이즈 연결

페이즈 2(`JwtProvider`)에서 JWT 파싱 오류 시 `WelfareException(UNAUTHORIZED)`를 던지며,
페이즈 3(`SecurityConfig`)에서 인증 실패 응답을 `ApiResponse.error()`로 반환하는 데 이번 페이즈 산출물을 사용한다.
