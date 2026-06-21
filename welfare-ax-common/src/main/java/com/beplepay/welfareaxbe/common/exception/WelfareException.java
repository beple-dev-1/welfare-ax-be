package com.beplepay.welfareaxbe.common.exception;

import lombok.Getter;

/**
 * 복지AX 서비스 전용 비즈니스 예외 클래스.
 * 예외 발생 시 {@link ErrorCode}를 포함하여 {@link GlobalExceptionHandler}에서
 * HTTP 상태코드와 오류 코드로 변환된다.
 */
@Getter
public class WelfareException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode 기본 메시지로 예외를 생성한다.
     *
     * @param errorCode 발생한 오류 코드
     */
    public WelfareException(ErrorCode errorCode) {
        // ErrorCode에 정의된 기본 메시지를 예외 메시지로 사용
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 직접 지정한 메시지로 예외를 생성한다.
     * UNKNOWN(E999) 등 상황별 상세 메시지가 필요할 때 사용한다.
     *
     * @param errorCode 발생한 오류 코드
     * @param message   응답에 표시할 상세 메시지
     */
    public WelfareException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
