/**
 * 공통 AJAX 설정.
 *
 * <p>모든 페이지의 scripts fragment에 포함되어 자동 적용된다.
 * CSRF 토큰 자동 포함 및 전역 HTTP 오류 처리를 담당한다.
 * ApiResponse(res.code) 처리는 각 페이지 IIFE의 success 콜백에서 수행한다.
 *
 * <p>주의: ui-common.js(showToast 정의)보다 나중에 로드되어야 한다.
 */
(() => {
    "use strict";

    $(document).ready(() => {
        // POST / PUT / DELETE 요청에 CSRF 토큰 자동 포함
        $.ajaxSetup({
            beforeSend(xhr, settings) {
                if (!/^(GET|HEAD|OPTIONS|TRACE)$/i.test(settings.type)) {
                    xhr.setRequestHeader(_csrfHeader_, _csrfToken_);
                }
            }
        });

        // 전역 HTTP 오류 처리 (비즈니스 오류는 각 페이지에서 처리)
        $(document).ajaxError((event, xhr) => {
            if (xhr.status === 401) {
                location.href = "/login";
            } else if (xhr.status === 403) {
                showToast("접근 권한이 없습니다.", "error");
            } else if (xhr.status === 500) {
                showToast("서버 오류가 발생했습니다.", "error");
            }
        });
    });
})();
