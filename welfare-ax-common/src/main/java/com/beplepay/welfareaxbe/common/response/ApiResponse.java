package com.beplepay.welfareaxbe.common.response;

import com.beplepay.welfareaxbe.common.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 API 응답에 사용되는 공통 응답 래퍼 클래스.
 * 정상 응답은 코드 "0000", 오류 응답은 "Exxx" 형식의 코드를 반환한다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 데이터가 있는 성공 응답을 생성한다.
     *
     * @param <T>  응답 데이터 타입
     * @param data 응답 데이터
     * @return 코드 "0000", 메시지 "성공"인 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0000", "성공", data);
    }

    /**
     * 데이터가 없는 성공 응답을 생성한다.
     *
     * @param <T> 응답 데이터 타입
     * @return 코드 "0000", 메시지 "성공", 데이터 null인 응답 객체
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("0000", "성공", null);
    }

    /**
     * ErrorCode 기본 메시지를 사용하는 오류 응답을 생성한다.
     *
     * @param <T>       응답 데이터 타입
     * @param errorCode 오류 코드 (코드값과 기본 메시지를 포함)
     * @return 오류 응답 객체
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    /**
     * 직접 지정한 메시지를 사용하는 오류 응답을 생성한다.
     * 입력값 검증 오류처럼 필드별 상세 메시지가 필요할 때 사용한다.
     *
     * @param <T>       응답 데이터 타입
     * @param errorCode 오류 코드
     * @param message   응답에 표시할 메시지
     * @return 오류 응답 객체
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }
}
