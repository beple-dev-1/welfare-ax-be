# 페이즈 3: 환경별 설정 및 최종 검증

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 3 |
| 목표 | Swagger UI 환경별 활성화 설정, logback 주석 추가, 전체 빌드·테스트 최종 검증 |
| 의존 페이즈 | Phase 2 (Swagger 인프라 구성 완료 후 진행) |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|---------|---------|------|
| `welfare-ax-user/src/main/resources/application-local.yaml` | 수정 | springdoc 활성화 설정 추가 |
| `welfare-ax-user/src/main/resources/application-dev.yaml` | 신규 생성 | dev 프로파일 springdoc 활성화 ⚠️ 스코프 외 |
| `welfare-ax-user/src/main/resources/logback-spring.xml` | 수정 | traceId 패턴 옆 MdcConstants 참조 주석 추가 ⚠️ 스코프 외 |
| `welfare-ax-user/src/main/resources/application.yaml` | 수정 (필요 시) | dev 프로파일 그룹 등록 확인 |

---

## 상세 구현 가이드

### application-local.yaml — springdoc 설정 추가

기존 파일 하단에 다음 블록을 추가한다:

```yaml
springdoc:
  swagger-ui:
    enabled: true
    # Swagger UI 진입 경로
    path: /swagger-ui.html
    # 요청/응답 모델 예시 자동 표시
    try-it-out-enabled: true
  api-docs:
    enabled: true
    path: /v3/api-docs
```

### application-dev.yaml — 신규 생성

```yaml
# dev 프로파일 설정
# Swagger UI를 활성화하여 개발 환경에서 API 문서 및 테스트 제공

springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true
  api-docs:
    enabled: true
    path: /v3/api-docs
```

> **application.yaml 확인 필요**: `spring.profiles.group`에 `dev`가 등록되어 있는지 확인한다.
> 미등록 시 아래와 같이 추가:
> ```yaml
> spring:
>   profiles:
>     group:
>       local: local,api
>       dev: dev,api   # dev 그룹 추가
> ```

### logback-spring.xml — 주석 추가

`%X{traceId:-NO_TRACE}` 패턴이 있는 `property` 라인 바로 위에 주석 추가:

```xml
<!-- traceId 키는 MdcConstants.TRACE_ID_KEY("traceId")와 일치해야 한다 -->
<property name="LOG_PATTERN"
  value="[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%X{traceId:-NO_TRACE}] %-5level %logger{36} - %msg%n"/>
```

> **참고**: logback XML에서 Java 상수를 직접 참조할 수 없으므로 주석으로 연결 관계를 명시한다.

### 최종 빌드 검증 순서

```bash
# 1. 전체 클린 빌드
./gradlew clean build

# 2. 모듈별 테스트 확인
./gradlew :welfare-ax-common:test
./gradlew :welfare-ax-user:test

# 3. 의존성 충돌 확인
./gradlew :welfare-ax-user:dependencies --configuration compileClasspath

# 4. 로컬 기동 후 Swagger UI 확인
./gradlew :welfare-ax-user:bootRun
# 브라우저: http://localhost:8080/swagger-ui/index.html
# 브라우저: http://localhost:8080/v3/api-docs
```

---

## 완료 기준

- [ ] `./gradlew clean build` 성공 (경고 없이 BUILD SUCCESSFUL)
- [ ] `./gradlew test` 전체 통과 (기존 30건 + 신규 TC 포함)
- [ ] 로컬 기동 후 `http://localhost:8080/swagger-ui/index.html` 정상 렌더링
- [ ] Swagger UI에서 `Authorize` 버튼 → Bearer 토큰 입력 UI 표시 확인
- [ ] `GET /v3/api-docs` 응답의 `components.securitySchemes.BearerAuth` 확인
- [ ] logback 로그에 traceId 포함 여부 확인 (`[uuid-값]` 형태)
- [ ] `/code-review` CRITICAL 0건 확인

---

## 다음 페이즈 연결

Phase 3 완료 후 `/qa-test COMMON-00004` 로 테스트 결과서를 작성하고, `/code-review`·`/git` 순서로 커밋·PR을 진행한다.
