# 페이즈 2: JWT 유틸 및 설정

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 2 |
| 목표 | JWT 토큰 검증·파싱 유틸 구현 및 환경별 yml 설정 추가 |
| 의존 페이즈 | Phase 1 (WelfareException, ErrorCode 사용) |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|----------|----------|------|
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/security/JwtProvider.java` | 신규 생성 | JWT 토큰 검증·파싱 유틸 |
| `welfare-ax-user/src/main/resources/application-local.yaml` | 수정 | jwt.secret, jwt.expiration 추가 |
| `welfare-ax-user/src/test/java/com/beplepay/welfareaxbe/user/security/JwtProviderTest.java` | 신규 생성 | JwtProvider 단위 테스트 |

---

## 상세 구현 가이드

### JwtProvider.java

- **위치**: `welfare-ax-user/security/` 패키지 — 사용자 모듈 전용 보안 컴포넌트
- **`@Component`** + `@Value`로 yml 설정 주입
- **의존성**: `io.jsonwebtoken:jjwt-api:0.12.6` (이미 build.gradle.kts에 포함)

**주요 메서드**:

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `generateToken(Long memberId, String role)` | `String` | Access Token 발급 (로그인 과업에서 사용) |
| `validateToken(String token)` | `boolean` | 서명 유효성 + 만료 여부 검증 |
| `getMemberId(String token)` | `Long` | Claims에서 memberId 추출 |
| `getRole(String token)` | `String` | Claims에서 role 추출 |

**설정 바인딩**:
```java
@Value("${jwt.secret}")
private String secret;

@Value("${jwt.expiration}")
private long expirationSeconds;
```

**jjwt 0.12.x API 주의사항**:
- `Jwts.builder()` → `.signWith(key)` → `.compact()`
- `Jwts.parser()` → `.verifyWith(key)` → `.parseSignedClaims(token)`
- 키 생성: `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`
- 만료 예외: `ExpiredJwtException` (별도 처리)
- 서명 오류: `JwtException` (상위 예외)

**예외 처리**:
- `validateToken()` 내부에서 `JwtException`, `IllegalArgumentException` catch → `false` 반환
- `JwtFilter`에서 `false`이면 인증 없이 다음 필터로 전달 (Security가 401 처리)

```java
// 구조 예시
@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationSeconds;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long memberId, String role) { ... }

    public boolean validateToken(String token) { ... }

    public Long getMemberId(String token) { ... }

    public String getRole(String token) { ... }
}
```

### application-local.yaml 추가 설정

기존 파일 하단에 추가:

```yaml
jwt:
  secret: local-dev-secret-key-must-be-at-least-32-characters-long
  expiration: 43200  # 12시간 (초 단위)
```

- **시크릿 길이**: HS256 최소 32자, HS512 최소 64자 — HS256 기준 최소 32자 이상 사용
- **로컬 전용 시크릿**: 실제 운영 시크릿과 다른 임의 문자열 사용
- prod 환경 시크릿은 `application-prod.yaml`에 `${JWT_SECRET}` 방식으로 설정 (접근 금지 파일 — 배포 담당자 관리)

---

## 완료 기준

- [ ] `generateToken()` 호출 시 JWT 형식(`xxxxx.yyyyy.zzzzz`) 반환 확인
- [ ] `validateToken(유효한 토큰)` → `true` 반환 확인
- [ ] `validateToken(만료된 토큰)` → `false` 반환 확인
- [ ] `validateToken(잘못된 서명 토큰)` → `false` 반환 확인
- [ ] `getMemberId()`, `getRole()` Claims 파싱 확인
- [ ] yml 설정 주입 (`@Value`) 동작 확인
- [ ] 단위 테스트 통과

---

## 다음 페이즈 연결

페이즈 3(`JwtFilter`)에서 `JwtProvider.validateToken()`, `getMemberId()`, `getRole()`을 사용하여
요청 헤더에서 추출한 토큰을 검증하고 `SecurityContext`에 인증 정보를 설정한다.
