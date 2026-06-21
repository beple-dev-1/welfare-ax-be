# 페이즈 3: Security 필터 체인

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 3 |
| 목표 | Spring Security 6.x FilterChain 구성 및 JwtFilter 구현 |
| 의존 페이즈 | Phase 1 (ApiResponse, ErrorCode), Phase 2 (JwtProvider) |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|----------|----------|------|
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/security/JwtFilter.java` | 신규 생성 | JWT 검증 필터 |
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/SecurityConfig.java` | 신규 생성 | Security FilterChain 설정 |
| `welfare-ax-user/src/test/java/com/beplepay/welfareaxbe/user/security/JwtFilterTest.java` | 신규 생성 | JwtFilter 슬라이스 테스트 |

---

## 상세 구현 가이드

### JwtFilter.java

- **`OncePerRequestFilter`** 확장 — 요청당 1회만 실행 보장
- `JwtProvider` 주입 (`@RequiredArgsConstructor`)
- **처리 흐름**:
  1. `Authorization` 헤더에서 `Bearer {token}` 추출
  2. 토큰 존재 여부 확인 (없으면 `filterChain.doFilter()` 바로 호출)
  3. `JwtProvider.validateToken()` 호출
  4. 유효하면 → `memberId`, `role` 추출 → `UsernamePasswordAuthenticationToken` 생성 → `SecurityContextHolder` 설정
  5. 유효하지 않으면 → SecurityContext 설정 없이 다음 필터로 전달 (Security가 401 처리)

**인증 토큰 생성**:
```java
UsernamePasswordAuthenticationToken authentication =
    new UsernamePasswordAuthenticationToken(memberId, null, authorities);
SecurityContextHolder.getContext().setAuthentication(authentication);
```

- `principal`: `memberId` (Long)
- `authorities`: `role` 문자열 → `SimpleGrantedAuthority("ROLE_" + role)`
- `credentials`: `null` (토큰 검증 완료 후이므로)

**Bearer 토큰 추출 유틸**:
```java
private String extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
        return header.substring(7);
    }
    return null;
}
```

### SecurityConfig.java

- `@Configuration` + `@EnableWebSecurity`
- `JwtFilter` 주입 (`@RequiredArgsConstructor`)
- **`SecurityFilterChain` Bean**:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(e -> e
            .authenticationEntryPoint(jwtAuthenticationEntryPoint())
            .accessDeniedHandler(jwtAccessDeniedHandler())
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

**인증 실패 핸들러 (선택적 구현)**:
- `AuthenticationEntryPoint` → 401 응답을 `ApiResponse.error(ErrorCode.UNAUTHORIZED)` JSON으로 반환
- `AccessDeniedHandler` → 403 응답을 `ApiResponse.error(ErrorCode.FORBIDDEN)` JSON으로 반환
- 두 핸들러는 `SecurityConfig` 내 중첩 클래스 또는 별도 파일로 구현 가능

**PasswordEncoder Bean** (로그인 과업을 위해 미리 등록):
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

---

## 완료 기준

- [ ] 공개 경로(`/api/v1/auth/**`) → 토큰 없이 200 응답 확인
- [ ] 보호 경로 + 유효 토큰 → SecurityContext에 인증 설정 + 200 응답 확인
- [ ] 보호 경로 + 토큰 없음 → 401 응답 확인
- [ ] 보호 경로 + 만료 토큰 → 401 응답 확인
- [ ] 보호 경로 + 잘못된 서명 토큰 → 401 응답 확인
- [ ] 401/403 응답이 `ApiResponse` 형식 JSON으로 반환 확인
- [ ] 슬라이스/통합 테스트 통과

---

## 다음 페이즈 연결

이 페이즈 완료 후 전체 공통 인프라가 완성된다.
이후 `ceremony` 등 도메인 과업에서 Controller에 `@PreAuthorize` 또는 `requestMatchers()`를 추가하여 권한 제어를 확장한다.
로그인 과업(`LOGIN-xxxxx`)에서 `JwtProvider.generateToken()`과 `PasswordEncoder`를 사용한다.
