/**
 * 탭 가시성 및 활성화 상태 관리 모듈
 * 브라우저 탭 가시성(visibilitychange)과 iframe 탭 활성화(TAB_ACTIVATION)를 통합 관리
 * visibilityGate.js의 기능도 포함
 */
(function(window) {
    'use strict';

    // 탭 활성 상태 플래그
    var tabActive = true;

    // 콜백 함수들
    var callbacks = {
        onHidden: null,           // 브라우저 탭이 숨겨질 때
        onVisible: null,         // 브라우저 탭이 다시 보일 때
        onTabActivated: null,     // iframe 탭이 활성화될 때
        onTabDeactivated: null   // iframe 탭이 비활성화될 때
    };

    // visibilityGate 호환성을 위한 리스너 배열
    var visibilityChangeListeners = [];

    /**
     * 현재 탭이 활성 상태인지 확인
     * @returns {boolean} 브라우저 탭이 보이고 iframe 탭도 활성 상태면 true
     */
    function isActive() {
        return !document.hidden && tabActive;
    }

    /**
     * 브라우저 탭 가시성 변경 처리
     */
    function handleVisibilityChange() {
        var visible = !document.hidden;
        
        // visibilityGate 호환성: onChange 리스너 호출
        visibilityChangeListeners.forEach(function(listener) {
            try {
                listener(visible);
            } catch (e) {
                console.error('[tabVisibilityManager] visibilityChange listener error:', e);
            }
        });

        if (document.hidden) {
            // 브라우저 탭이 숨겨짐
            if (callbacks.onHidden && typeof callbacks.onHidden === 'function') {
                try {
                    callbacks.onHidden();
                } catch (e) {
                    console.error('[tabVisibilityManager] onHidden callback error:', e);
                }
            }
        } else {
            // 브라우저 탭이 다시 보임
            if (callbacks.onVisible && typeof callbacks.onVisible === 'function') {
                try {
                    callbacks.onVisible(isActive());
                } catch (e) {
                    console.error('[tabVisibilityManager] onVisible callback error:', e);
                }
            }
        }
    }

    /**
     * iframe 탭 활성화/비활성화 메시지 처리
     */
    function handleTabActivationMessage(event) {
        if (event.data && event.data.type === 'TAB_ACTIVATION') {
            var wasActive = tabActive;
            tabActive = !!event.data.active;

            if (tabActive && !wasActive) {
                // iframe 탭이 활성화됨
                if (callbacks.onTabActivated && typeof callbacks.onTabActivated === 'function') {
                    try {
                        callbacks.onTabActivated(isActive());
                    } catch (e) {
                        console.error('[tabVisibilityManager] onTabActivated callback error:', e);
                    }
                }
            } else if (!tabActive && wasActive) {
                // iframe 탭이 비활성화됨
                if (callbacks.onTabDeactivated && typeof callbacks.onTabDeactivated === 'function') {
                    try {
                        callbacks.onTabDeactivated();
                    } catch (e) {
                        console.error('[tabVisibilityManager] onTabDeactivated callback error:', e);
                    }
                }
            }
        }
    }

    // 이벤트 리스너 등록
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('message', handleTabActivationMessage);

    // 전역 API 노출
    window.tabVisibilityManager = {
        /**
         * 현재 탭이 활성 상태인지 확인
         * @returns {boolean}
         */
        isActive: isActive,

        /**
         * iframe 탭 활성 상태 확인
         * @returns {boolean}
         */
        isTabActive: function() {
            return tabActive;
        },

        /**
         * 브라우저 탭 가시성 확인
         * @returns {boolean}
         */
        isVisible: function() {
            return !document.hidden;
        },

        /**
         * 콜백 함수 등록
         * @param {Object} options - 콜백 함수들
         * @param {Function} options.onHidden - 브라우저 탭이 숨겨질 때 호출
         * @param {Function} options.onVisible - 브라우저 탭이 다시 보일 때 호출 (isActive 파라미터 전달)
         * @param {Function} options.onTabActivated - iframe 탭이 활성화될 때 호출 (isActive 파라미터 전달)
         * @param {Function} options.onTabDeactivated - iframe 탭이 비활성화될 때 호출
         */
        register: function(options) {
            if (options.onHidden) {
                callbacks.onHidden = options.onHidden;
            }
            if (options.onVisible) {
                callbacks.onVisible = options.onVisible;
            }
            if (options.onTabActivated) {
                callbacks.onTabActivated = options.onTabActivated;
            }
            if (options.onTabDeactivated) {
                callbacks.onTabDeactivated = options.onTabDeactivated;
            }
        },

        /**
         * 콜백 함수 해제
         */
        unregister: function() {
            callbacks.onHidden = null;
            callbacks.onVisible = null;
            callbacks.onTabActivated = null;
            callbacks.onTabDeactivated = null;
        }
    };

    // visibilityGate 호환성 API (하위 호환성 유지)
    window.visibilityGate = {
        isVisible: function() {
            return !document.hidden;
        },
        onChange: function(callback) {
            if (typeof callback === 'function') {
                visibilityChangeListeners.push(callback);
                // 초기 상태 호출
                callback(window.tabVisibilityManager.isVisible());
            }
        }
    };

    // 초기 상태 알림 (visibilityGate 호환성)
    if (document.readyState === 'complete') {
        var visible = !document.hidden;
        visibilityChangeListeners.forEach(function(listener) {
            try {
                listener(visible);
            } catch (e) {
                console.error('[tabVisibilityManager] visibilityChange listener error:', e);
            }
        });
    }

})(window);

