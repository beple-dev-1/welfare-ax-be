/**
 * 공통 UI 유틸리티.
 *
 * <p>Bootstrap 5 Toast / Modal API 래퍼를 전역으로 노출한다.
 * window.showToast, window.openConfirmModal 으로 호출한다.
 *
 * <p>의존: toast.html fragment(#toastMsg), modal-confirm.html fragment(#confirmModal)
 */
(() => {
    "use strict";

    /** 확인 모달 대기 중인 콜백 */
    let _pendingAction = null;

    /**
     * Bootstrap Toast 알림 표시.
     *
     * @param {string} message         - 표시할 메시지
     * @param {string} [type="success"] - "success" | "error" | "warning"
     */
    function showToast(message, type = "success") {
        const toastEl = document.getElementById("toastMsg");
        if (!toastEl) return;

        // 타입별 색상 클래스 교체
        toastEl.classList.remove("text-bg-success", "text-bg-danger", "text-bg-warning");
        const clsMap = {
            success: "text-bg-success",
            error:   "text-bg-danger",
            warning: "text-bg-warning"
        };
        toastEl.classList.add(clsMap[type] ?? "text-bg-success");

        toastEl.querySelector(".toast-body").textContent = message;
        new bootstrap.Toast(toastEl, { delay: 3000 }).show();
    }

    /**
     * 확인/취소 모달 오픈.
     *
     * @param {string}   message        - 모달 본문 메시지
     * @param {Function} onConfirm      - 확인 클릭 시 실행할 콜백
     * @param {string}   [title="확인"] - 모달 타이틀
     */
    function openConfirmModal(message, onConfirm, title = "확인") {
        document.getElementById("confirmModalLabel").textContent = title;
        document.getElementById("confirmModalBody").textContent  = message;
        _pendingAction = onConfirm;
        new bootstrap.Modal(document.getElementById("confirmModal")).show();
    }

    // 확인 버튼 클릭 처리 (문서 전체에 한 번만 바인딩)
    document.addEventListener("DOMContentLoaded", () => {
        const btnConfirm = document.getElementById("btnConfirmAction");
        if (!btnConfirm) return;

        btnConfirm.addEventListener("click", () => {
            if (_pendingAction) _pendingAction();
            bootstrap.Modal.getInstance(document.getElementById("confirmModal"))?.hide();
            _pendingAction = null;
        });
    });

    // 전역 노출
    window.showToast        = showToast;
    window.openConfirmModal = openConfirmModal;
})();
