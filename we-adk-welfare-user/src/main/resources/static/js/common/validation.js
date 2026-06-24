/**
 * 공통 유효성 검사 유틸리티.
 *
 * <p>한국 도메인 필수 정규식과 폼 일괄 검증 함수를 제공한다.
 * window.WelfareValidation.validate / validateForm 으로 호출한다.
 */
(() => {
    "use strict";

    /** 한국 도메인 필수 정규식 */
    const PATTERNS = {
        phone:    /^01[016789]-?\d{3,4}-?\d{4}$/,
        amount:   /^\d+$/,
        date:     /^\d{4}-\d{2}-\d{2}$/,
        postCode: /^\d{5}$/,
        email:    /^[\w.-]+@[\w.-]+\.\w{2,}$/,
        bizNo:    /^\d{3}-?\d{2}-?\d{5}$/
    };

    /**
     * 단일 값 정규식 검증.
     *
     * @param {string} type  - PATTERNS 키 (phone | amount | date | postCode | email | bizNo)
     * @param {string} value - 검증할 값
     * @returns {boolean} 유효 여부
     */
    function validate(type, value) {
        const pattern = PATTERNS[type];
        if (!pattern) return true;
        return pattern.test((value ?? "").trim());
    }

    /**
     * 폼 전체 필수 입력 검증 (제출 직전 일괄 검사).
     *
     * <p>[required] 속성 필드를 순회하며 빈 값에 .is-invalid를 추가한다.
     * 첫 번째 오류 필드로 포커스를 이동한다.
     *
     * @param {string} formSelector - jQuery 셀렉터 (예: "#applyForm")
     * @returns {boolean} 모두 유효하면 true
     */
    function validateForm(formSelector) {
        let isValid = true;

        $(`${formSelector} [required]`).each(function () {
            if (!$(this).val().trim()) {
                $(this).addClass("is-invalid");
                isValid = false;
            } else {
                $(this).removeClass("is-invalid");
            }
        });

        // 첫 번째 오류 필드로 포커스 이동
        if (!isValid) {
            $(`${formSelector} .is-invalid`).first().trigger("focus");
        }

        return isValid;
    }

    window.WelfareValidation = { validate, validateForm };
})();
