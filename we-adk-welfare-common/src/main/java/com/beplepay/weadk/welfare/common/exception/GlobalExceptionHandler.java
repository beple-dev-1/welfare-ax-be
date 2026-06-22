package com.beplepay.weadk.welfare.common.exception;

import com.beplepay.weadk.welfare.common.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 서비스 전역 예외 처리기.
 * 컨트롤러에서 발생한 예외를 {@link ApiResponse} 형식의 일관된 응답으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 시 첫 번째 필드 오류 메시지를 반환한다.
     *
     * @param e 입력값 검증 예외
     * @return 400 Bad Request + E001 오류 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 바인딩 오류 중 첫 번째 필드 오류 메시지 추출
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getDefaultMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, message));
    }

    /**
     * 비즈니스 예외를 ErrorCode에 정의된 HTTP 상태코드로 응답한다.
     *
     * @param e 비즈니스 예외
     * @return ErrorCode에 매핑된 HTTP 상태 + 오류 응답
     */
    @ExceptionHandler(WelfareException.class)
    public ResponseEntity<ApiResponse<Void>> handleWelfareException(WelfareException e) {
        // 예외에 포함된 ErrorCode로 HTTP 상태 결정
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    /**
     * 정의되지 않은 예외를 500 Internal Server Error로 응답한다.
     *
     * @param e 처리되지 않은 예외
     * @return 500 Internal Server Error + E999 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 알 수 없는 예외는 반드시 로그에 기록하여 운영 환경에서 추적 가능하게 한다
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity
                .status(ErrorCode.UNKNOWN.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.UNKNOWN));
    }
}
