package com.beplepay.welfareaxbe.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스 전체에서 사용하는 오류 코드 정의 열거형.
 * 코드 체계: 정상 "0000" / 오류 "Exxx" (4자리)
 * E999는 정의되지 않은 기타 오류로, 직접 메시지를 지정할 때 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 — 입력·조회·중복
    INVALID_INPUT("E001", "잘못된 요청입니다", HttpStatus.BAD_REQUEST),
    NOT_FOUND("E002", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATE("E003", "이미 존재하는 데이터입니다", HttpStatus.CONFLICT),
    BUSINESS_RULE_VIOLATION("E004", "비즈니스 규칙 위반입니다", HttpStatus.UNPROCESSABLE_ENTITY),

    // 공통 — 인증·권한
    UNAUTHORIZED("E005", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("E006", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // 외부 연동 — 외부 API 호출 오류 (E1xx)
    EXTERNAL_API_ERROR("E101", "외부 API 호출에 실패했습니다", HttpStatus.BAD_GATEWAY),

    // 기타 — 직접 메시지 지정 시 사용
    UNKNOWN("E999", "기타 오류", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
