# Phase 3 — logback-spring.xml 구성

## 목표
로그 출력 패턴에 `%X{traceId}` MDC 참조를 추가하여,
모든 로그 라인에 traceId가 자동으로 포함되도록 한다.

---

## 신규 파일

### `logback-spring.xml`

```
경로: welfare-ax-user/src/main/resources/logback-spring.xml
```

**설계 포인트:**
- Spring Profile별 로그 레벨 분리 (`<springProfile name="local">`)
- traceId 미설정 환경 기본값: `NO_TRACE` (`%X{traceId:-NO_TRACE}`)
- 운영 환경: INFO 레벨, Body(TRACE) 비활성화
- 로컬 환경: DEBUG 레벨, SQL·Hibernate 로그 포함

**로그 패턴:**
```
[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%X{traceId:-NO_TRACE}] %-5level %logger{36} - %msg%n
```

**파일 구조:**
```xml
<configuration>

  <property name="LOG_PATTERN"
    value="[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%X{traceId:-NO_TRACE}] %-5level %logger{36} - %msg%n"/>

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
    </encoder>
  </appender>

  <!-- 로컬 환경: DEBUG 레벨, SQL 로깅 포함 -->
  <springProfile name="local">
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
    </root>
    <logger name="com.beplepay.welfareaxbe" level="DEBUG"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql" level="TRACE"/>
  </springProfile>

  <!-- 기본(운영 포함): INFO 레벨, Body 로깅(TRACE) 비활성화 -->
  <springProfile name="!local">
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
    </root>
    <logger name="com.beplepay.welfareaxbe" level="INFO"/>
  </springProfile>

</configuration>
```

**기존 `application-local.yaml` logging 설정과의 관계:**
- `logback-spring.xml`이 존재하면 Spring Boot는 이를 우선 사용
- `application-local.yaml`의 `logging.level.*` 설정은 `logback-spring.xml`과 병합 가능하나
  충돌을 피하기 위해 `application-local.yaml`의 `logging` 섹션을 제거하고 `logback-spring.xml`로 일원화

---

## 완료 기준
- [ ] 로컬 기동 후 로그에 `[{UUID}]` 형태의 traceId 포함 확인
- [ ] traceId 없는 요청 로그에 `[NO_TRACE]` 출력 확인
- [ ] 운영 프로파일에서 TRACE 로그 미출력 확인
