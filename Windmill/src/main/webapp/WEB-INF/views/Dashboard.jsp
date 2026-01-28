<%@include file="common/common.jsp"%>
    <link href="/resources/css/dashboard.css" rel="stylesheet" type="text/css" />

    <script>
        // 전역 변수
        var selectedConnectionId = null; // 선택된 연결 ID
        var chartConfig = null; // 차트 설정
        var chartUpdateTimer = null; // 차트 업데이트 타이머
        var cachedApiResponse = null; // API 응답 데이터 캐시
        var chartConfigData = null; // 차트 설정 데이터
        var chartConfigHash = null; // 차트 설정 hash (변경 감지용)
        var monitoringConfigHash = null; // 모니터링 템플릿 설정 hash (변경 감지용)
        var monitoringConfigData = null; // 모니터링 템플릿 설정 데이터 캐시
        var isAdmin = ${isAdmin}; // 관리자 권한 여부
        
        // 타이머 핸들 저장용 변수 (setTimeout 기반)
        var connectionRefreshTimeout = null;
        var chartRefreshTimeout = null;
        var monitoringTemplateTimeout = null;

        // AJAX 요청 취소를 위한 AbortController
        var connectionStatusAbortController = null;
        var chartUpdateAbortController = null;
        var monitoringTemplateAbortController = null;

        // 탭(iframe) 활성 상태는 tabVisibilityManager에서 관리

        // Chart.js 플러그인 로드 상태
        var chartPluginsLoaded = {
            datalabels: false,
            annotation: false
        };

        // 색상 정의
        const COLORS = {
            green: 'rgba(140, 214, 16)',
            yellow: 'rgba(239, 198, 0)',
           red: 'rgba(231, 24, 48)',
           gray: 'rgba(158, 158, 158)'
        };

        const COLORS2 = {
            green: 'rgba(140, 214, 16, 0.8)',
            yellow: 'rgba(239, 198, 0, 0.8)',
           red: 'rgba(231, 24, 48, 0.8)',
           gray: 'rgba(158, 158, 158, 0.8)'
        };
        
        // 신호등 임계값 설정 (기본값)
        var defaultTrafficLightThresholds = {
            green: { min: 0, max: 70 },
            yellow: { min: 71, max: 89 },
            red: { min: 90, max: 100 }
        };

        // 날짜 시간 포맷팅 함수
        function formatDateTime(timestamp) {
            if (!timestamp) return '-';
            var date = new Date(timestamp);
            var year = date.getFullYear();
            var month = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            var hours = String(date.getHours()).padStart(2, '0');
            var minutes = String(date.getMinutes()).padStart(2, '0');
            var seconds = String(date.getSeconds()).padStart(2, '0');
            return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
        }

        function isDashboardRunning() {
            // tabVisibilityManager가 로드되지 않았으면 기본적으로 true 반환 (초기 로드 시)
            if (!window.tabVisibilityManager) {
                return !document.hidden;
            }
            return window.tabVisibilityManager.isActive();
        }

        function clearConnectionRefreshTimeout() {
            if (connectionRefreshTimeout) {
                clearTimeout(connectionRefreshTimeout);
                connectionRefreshTimeout = null;
            }
        }

        function clearChartRefreshTimeout() {
            if (chartRefreshTimeout) {
                clearTimeout(chartRefreshTimeout);
                chartRefreshTimeout = null;
            }
        }

        function clearMonitoringTemplateTimeout() {
            if (monitoringTemplateTimeout) {
                clearTimeout(monitoringTemplateTimeout);
                monitoringTemplateTimeout = null;
            }
        }

        // 진행 중인 AJAX 요청 취소
        function abortPendingAjaxRequests() {
            if (connectionStatusAbortController) {
                connectionStatusAbortController.abort();
                connectionStatusAbortController = null;
            }
            if (chartUpdateAbortController) {
                chartUpdateAbortController.abort();
                chartUpdateAbortController = null;
            }
            if (monitoringTemplateAbortController) {
                monitoringTemplateAbortController.abort();
                monitoringTemplateAbortController = null;
            }
        }

        // Chart.js 플러그인 동적 로드 함수
        function loadChartPlugin(pluginName, callback) {
            if (pluginName === 'datalabels' && chartPluginsLoaded.datalabels) {
                if (callback) callback();
                return;
            }
            if (pluginName === 'annotation' && chartPluginsLoaded.annotation) {
                if (callback) callback();
                return;
            }

            var script = document.createElement('script');
            script.onload = function() {
                if (pluginName === 'datalabels') {
                    chartPluginsLoaded.datalabels = true;
                    // datalabels 플러그인은 자동으로 등록됨
                } else if (pluginName === 'annotation') {
                    chartPluginsLoaded.annotation = true;
                    // annotation 플러그인은 자동으로 등록되지만, 
                    // 모든 차트에 빈 설정을 추가해야 함
                }
                if (callback) callback();
            };
            script.onerror = function() {
                console.error('Chart.js 플러그인 로드 실패: ' + pluginName);
                if (callback) callback();
            };

            if (pluginName === 'datalabels') {
                script.src = '/resources/plugins/chartjs/chartjs-plugin-datalabels.min.js';
            } else if (pluginName === 'annotation') {
                script.src = '/resources/plugins/chartjs/chartjs-plugin-annotation.min.js';
            }
            document.head.appendChild(script);
        }

        // 필요한 Chart.js 플러그인들을 동적으로 로드
        function ensureChartPlugins(chartType, callback) {
            var pluginsToLoad = [];
            
            if (chartType === 'doughnut') {
                pluginsToLoad.push('datalabels');
            } else if (chartType === 'gauge') {
                pluginsToLoad.push('annotation');
            }

            if (pluginsToLoad.length === 0) {
                if (callback) callback();
                return;
            }

            var loadedCount = 0;
            var totalCount = pluginsToLoad.length;

            pluginsToLoad.forEach(function(pluginName) {
                loadChartPlugin(pluginName, function() {
                    loadedCount++;
                    if (loadedCount === totalCount) {
                        if (callback) callback();
                    }
                });
            });
        }

        // 연결 관리 탭 열기 함수
        function openConnectionTab() {
            parent.tabManager.addTab('connection', '연결 관리', '/Connection');
        }

        $(document).ready(function() {
            // 초기 차트 설정 로딩
            loadChartConfig();
            
            // 초기 모니터링 템플릿 설정 로딩
            loadMonitoringTemplateConfig();
            
            // 연결 상태 모니터링 시작
            startConnectionMonitoring();
            
            // 차트 모니터링 시작 (통합 API 사용)
            startChartMonitoring();
            
            // 실행기록 모니터링 시작
            startMonitoringTemplateMonitoring();
            
            // 페이지 언로드 시 정리
            $(window).on('beforeunload', function() {
                if (chartUpdateTimer) {
                    clearInterval(chartUpdateTimer);
                }
                stopConnectionMonitoring();
                stopMonitoringTemplateMonitoring();
                
                // 모든 차트 인스턴스 정리
                if (window.chartInstances) {
                    for (var chartId in window.chartInstances) {
                        if (window.chartInstances.hasOwnProperty(chartId)) {
                            try {
                                window.chartInstances[chartId].destroy();
                            } catch (e) {
                                console.error('차트 인스턴스 정리 중 오류:', e);
                            }
                            delete window.chartInstances[chartId];
                        }
                    }
                    window.chartInstances = {};
                }
            });
            
            // 탭 가시성 및 활성화 상태 관리
            if (window.tabVisibilityManager) {
                window.tabVisibilityManager.register({
                    onHidden: function() {
                        // 브라우저 탭이 숨겨지면 모든 주기적인 작업을 중단
                        clearConnectionRefreshTimeout();
                        clearChartRefreshTimeout();
                        clearMonitoringTemplateTimeout();
                        abortPendingAjaxRequests();
                    },
                    onVisible: function(isActive) {
                        // 브라우저 탭이 다시 활성화되면 (iframe 탭도 활성 상태일 경우) 주기적인 작업 재개
                        if (isActive) {
                            refreshConnectionStatus();
                            updateDynamicCharts();
                            refreshMonitoringTemplate();
                        }
                    },
                    onTabActivated: function(isActive) {
                        // iframe 탭이 활성화되면 (브라우저 탭도 활성 상태일 경우) 주기적인 작업 재개
                        if (isActive) {
                            refreshConnectionStatus();
                            updateDynamicCharts();
                            refreshMonitoringTemplate();
                        }
                    },
                    onTabDeactivated: function() {
                        // iframe 탭이 비활성화되면 모든 주기적인 작업을 중단
                        clearConnectionRefreshTimeout();
                        clearChartRefreshTimeout();
                        clearMonitoringTemplateTimeout();
                        abortPendingAjaxRequests();
                    }
                });
            }
        });
        
        // 연결 상태 모니터링 시작
        function startConnectionMonitoring() {
            refreshConnectionStatus();
        }

        // 연결 상태 새로고침 함수
        function refreshConnectionStatus() {
            if (!isDashboardRunning()) {
                clearConnectionRefreshTimeout();
                return;
            }

            // 이전 요청이 있으면 취소
            if (connectionStatusAbortController) {
                connectionStatusAbortController.abort();
            }
            connectionStatusAbortController = new AbortController();

            $.ajax({
                type: 'post',
                url: '/Connection/status',
                signal: connectionStatusAbortController.signal,
                success: function(result) {
                    if (!isDashboardRunning()) {
                        return;
                    }
                    updateConnectionStatusDisplay(result);
                    // 연결 상태 확인 완료 후 다음 타이머 설정
                    scheduleNextRefresh();
                    connectionStatusAbortController = null;
                },
                error: function(xhr, status, error) {
                    // AbortError는 무시 (요청이 취소된 경우)
                    if (xhr.statusText === 'abort' || error === 'abort') {
                        return;
                    }
                    if (!isDashboardRunning()) {
                        return;
                    }
                    console.error('연결 상태 조회 실패:', error);
                    
                    // 연결 상태 컨테이너에 오류 메시지 표시
                    var container = $('#connectionStatusContainer');
                    container.html('<div class="col-md-12"><div class="alert alert-danger" role="alert">' +
                        '<i class="fa fa-exclamation-circle"></i> ' +
                        '연결 상태 조회 중 오류가 발생했습니다: ' + error + '<br>' +
                        '<button type="button" class="btn btn-sm btn-danger" onclick="refreshConnectionStatus()" style="margin-top: 10px;">' +
                        '<i class="fa fa-refresh"></i> 다시 시도</button>' +
                        '</div></div>');
                    
                    // 에러 발생 시에도 다음 타이머 설정
                    scheduleNextRefresh();
                    connectionStatusAbortController = null;
                }
            });
        }

        // 연결 순서 캐시 저장
        function saveConnectionOrder(connectionIds) {
            try {
                localStorage.setItem('dashboard_connection_order', JSON.stringify(connectionIds));
            } catch (e) {
                console.error('연결 순서 저장 실패:', e);
            }
        }
        
        // 연결 순서 캐시 로드
        function loadConnectionOrder() {
            try {
                var savedOrder = localStorage.getItem('dashboard_connection_order');
                if (savedOrder) {
                    return JSON.parse(savedOrder);
                }
            } catch (e) {
                console.error('연결 순서 로드 실패:', e);
            }
            return null;
        }
        
        // 연결 목록을 저장된 순서로 정렬
        function sortConnectionsBySavedOrder(connections) {
            var savedOrder = loadConnectionOrder();
            if (!savedOrder || !Array.isArray(savedOrder)) {
                return connections;
            }
            
            // 저장된 순서를 기준으로 정렬
            var sorted = [];
            var connectionMap = {};
            
            // 연결을 맵으로 변환
            connections.forEach(function(conn) {
                connectionMap[conn.connectionId] = conn;
            });
            
            // 저장된 순서대로 추가
            savedOrder.forEach(function(connectionId) {
                if (connectionMap[connectionId]) {
                    sorted.push(connectionMap[connectionId]);
                    delete connectionMap[connectionId];
                }
            });
            
            // 저장된 순서에 없는 새로운 연결 추가
            for (var connectionId in connectionMap) {
                sorted.push(connectionMap[connectionId]);
            }
            
            return sorted;
        }
        
        // 연결 상태 표시 업데이트 함수
        function updateConnectionStatusDisplay(connections) {
            var container = $('#connectionStatusContainer');
            
            // 연결이 없는 경우 처리
            if (!connections || connections.length === 0) {
                container.html('<div class="col-md-12"><div class="alert alert-warning" role="alert">' +
                    '<i class="fa fa-exclamation-triangle"></i> ' +
                    '연결된 데이터베이스가 없습니다. <a href="javascript:void(0);" class="alert-link" onclick="openConnectionTab(); return false;">연결 관리</a>에서 데이터베이스 연결을 추가하세요.' +
                    '</div></div>');
                return;
            }
            
            // 저장된 순서로 정렬
            connections = sortConnectionsBySavedOrder(connections);
            
            // 초기 로드인지 확인 (컨테이너가 비어있거나 alert가 있으면 초기 로드)
            var isInitialLoad = container.children().length === 0 || container.find('.alert').length > 0;
            
            if (isInitialLoad) {
                // 초기 로드: 전체 카드 생성
                container.empty();
                connections.forEach(function(conn) {
                    createConnectionCard(conn);
                });
                
                // 모든 연결에 대해 동적 신호등 생성
                connections.forEach(function(conn) {
                    createDynamicTrafficLights(conn.connectionId);
                });
                
                // 초기 로드 시 "연결됨" 상태인 첫 번째 연결 자동 선택
                var connectedConnection = connections.find(function(conn) {
                    return conn.status === 'connected';
                });
                
                if (connectedConnection) {
                    // 연결됨 상태인 연결이 있으면 자동 선택
                    setTimeout(function() {
                        selectConnection(connectedConnection.connectionId);
                    }, 100); // 약간의 지연을 두어 카드가 완전히 렌더링된 후 선택
                }
                
                // 현재 순서 저장
                var currentOrder = connections.map(function(conn) {
                    return conn.connectionId;
                });
                saveConnectionOrder(currentOrder);
            } else {
                // 업데이트: 추가/삭제/업데이트 처리
                var currentConnectionIds = [];
                $('.connection-card').each(function() {
                    currentConnectionIds.push($(this).data('connection-id'));
                });
                
                var newConnectionIds = connections.map(function(conn) {
                    return conn.connectionId;
                });
                
                // 삭제된 연결 제거
                currentConnectionIds.forEach(function(connectionId) {
                    if (newConnectionIds.indexOf(connectionId) === -1) {
                        $('.connection-card-wrapper[data-connection-id="' + connectionId + '"]').remove();
                    }
                });
                
                // 추가/업데이트 처리
                connections.forEach(function(conn) {
                    var existingCard = $('.connection-card[data-connection-id="' + conn.connectionId + '"]');
                    if (existingCard.length > 0) {
                        // 기존 카드 업데이트
                        updateConnectionCard(conn);
                    } else {
                        // 새 카드 생성
                        createConnectionCard(conn);
                    }
                });
                
                // 저장된 순서로 재정렬
                var savedOrder = loadConnectionOrder();
                if (savedOrder && Array.isArray(savedOrder)) {
                    var cardWrappers = container.find('.connection-card-wrapper');
                    savedOrder.forEach(function(connectionId) {
                        var cardWrapper = container.find('.connection-card-wrapper[data-connection-id="' + connectionId + '"]');
                        if (cardWrapper.length > 0) {
                            container.append(cardWrapper);
                        }
                    });
                }
            }
        }
        
        // 연결 카드 생성
        function createConnectionCard(conn) {
        	
            var statusIcon, statusText, statusClass;
            
            // 상태에 따른 아이콘, 텍스트, 클래스 결정
            if (conn.status === 'connected') {
                statusIcon = 'fa-check-circle';
                statusText = '연결됨';
                statusClass = 'connected';
            } else if (conn.status === 'checking') {
                statusIcon = 'fa-spinner fa-spin';
                statusText = '확인중';
                statusClass = 'checking';
            } else if (conn.status === 'error') {
                statusIcon = 'fa-exclamation-triangle';
                statusText = '오류';
                statusClass = 'error';
            } else {
                statusIcon = 'fa-times-circle';
                statusText = '연결실패';
                statusClass = 'disconnected';
            }
            
            var formattedTime = formatDateTime(conn.lastChecked);
            
            // 좌우 화살표 링크 HTML (양 끝에 배치, 아이콘만 표시)
            var arrowButtons = 
                '<a href="javascript:void(0);" class="btn-order btn-order-left" onclick="event.stopPropagation(); moveConnectionCard(\'' + conn.connectionId + '\', -1); return false;" title="왼쪽으로 이동">' +
                    '<i class="fa fa-chevron-left"></i>' +
                '</a>' +
                '<a href="javascript:void(0);" class="btn-order btn-order-right" onclick="event.stopPropagation(); moveConnectionCard(\'' + conn.connectionId + '\', 1); return false;" title="오른쪽으로 이동">' +
                    '<i class="fa fa-chevron-right"></i>' +
                '</a>';
            
            var connectionCard = 
                '<div class="col-md-2 col-sm-3 col-xs-4 connection-card-wrapper" data-connection-id="' + conn.connectionId + '">' +
                    '<div class="card connection-card ' + statusClass + '" data-connection-id="' + conn.connectionId + '"' + 
                    (conn.status === 'connected' ? ' onclick="selectConnection(\'' + conn.connectionId + '\')"' : '') + '>' +
                        arrowButtons +
                        '<div class="connection-name">' +
                            conn.connectionId +
                        '</div>' +
                        '<div class="status-text" id="status-' + conn.connectionId + '">' +
                            '<i class="fa ' + statusIcon + '"></i> ' + statusText +
                        '</div>' +
                        '<div class="last-checked" id="lastChecked-' + conn.connectionId + '">' +
                            formattedTime +
                        '</div>' +
                        '<div class="card-footer">' +
                            '<div class="traffic-light-container" id="traffic-light-' + conn.connectionId + '">' +
                                '<div class="traffic-light" id="traffic-lights-' + conn.connectionId + '">' +
                                    '<!-- 동적으로 신호등이 생성됩니다 -->' +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            
            $('#connectionStatusContainer').append(connectionCard);
            
            // 동적 신호등 생성
            createDynamicTrafficLights(conn.connectionId);
        }
        
        // 연결 카드 순서 변경
        function moveConnectionCard(connectionId, direction) {
            var container = $('#connectionStatusContainer');
            var cardWrapper = container.find('.connection-card-wrapper[data-connection-id="' + connectionId + '"]');
            
            if (cardWrapper.length === 0) {
                return;
            }
            
            var allCards = container.find('.connection-card-wrapper');
            var currentIndex = allCards.index(cardWrapper);
            var newIndex = currentIndex + direction;
            
            // 범위 체크
            if (newIndex < 0 || newIndex >= allCards.length) {
                return;
            }
            
            // DOM에서 이동
            if (direction < 0) {
                // 왼쪽으로 이동
                cardWrapper.insertBefore(allCards.eq(newIndex));
            } else {
                // 오른쪽으로 이동
                cardWrapper.insertAfter(allCards.eq(newIndex));
            }
            
            // 현재 순서 저장
            var currentOrder = [];
            container.find('.connection-card-wrapper').each(function() {
                currentOrder.push($(this).data('connection-id'));
            });
            saveConnectionOrder(currentOrder);
        }
        
        // 연결 카드 업데이트
        function updateConnectionCard(conn) {
            var card = $('.connection-card[data-connection-id="' + conn.connectionId + '"]');
            if (card.length === 0) {
                return;
            }
            
            // 상태에 따른 아이콘, 텍스트, 클래스 결정
            var statusIcon, statusText, statusClass;
            if (conn.status === 'connected') {
                statusIcon = 'fa-check-circle';
                statusText = '연결됨';
                statusClass = 'connected';
            } else if (conn.status === 'checking') {
                statusIcon = 'fa-spinner fa-spin';
                statusText = '확인중';
                statusClass = 'checking';
            } else if (conn.status === 'error') {
                statusIcon = 'fa-exclamation-triangle';
                statusText = '오류';
                statusClass = 'error';
            } else {
                statusIcon = 'fa-times-circle';
                statusText = '연결실패';
                statusClass = 'disconnected';
            }
            
            var formattedTime = formatDateTime(conn.lastChecked);
            
            // 상태 클래스 업데이트
            card.removeClass('connected checking error disconnected')
                .addClass(statusClass);
            
            // 상태 텍스트 및 아이콘 업데이트
            var statusElement = $('#status-' + conn.connectionId);
            if (statusElement.length > 0) {
                statusElement.html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
            }
            
            // 마지막 확인 시간 업데이트
            var lastCheckedElement = $('#lastChecked-' + conn.connectionId);
            if (lastCheckedElement.length > 0) {
                lastCheckedElement.text(formattedTime);
            }
            
            // 클릭 이벤트 업데이트 (connected 상태일 때만 클릭 가능)
            card.off('click');
            if (conn.status === 'connected') {
                card.css('cursor', 'pointer');
                card.attr('onclick', 'selectConnection(\'' + conn.connectionId + '\')');
            } else {
                card.css('cursor', 'default');
                card.removeAttr('onclick');
            }
        }
        
        // 상태에 따른 색상 반환
        function getStatusColor(status) {
            switch(status) {
                case '정상': return COLORS.green;
                case '경고': return COLORS.yellow;
                case '심각': return COLORS.red;
                default: return COLORS.gray;
            }
        }

        function getStatusColor2(status) {
            switch(status) {
                case '정상': return COLORS2.green;
                case '경고': return COLORS2.yellow;
                case '심각': return COLORS2.red;
                default: return COLORS2.gray;
            }
        }
        
        // 연결 선택
        function selectConnection(connectionId) {
            selectedConnectionId = connectionId;
            
            // 모든 카드에서 선택 상태 제거
            $('.connection-card').removeClass('selected');
            
            // 선택된 카드에 선택 상태 추가
            $('.connection-card[data-connection-id="' + connectionId + '"]').addClass('selected');
            
            // 캐시된 데이터가 있으면 해당 연결의 차트만 업데이트
            if (cachedApiResponse && cachedApiResponse.connections) {
                var selectedConnection = cachedApiResponse.connections.find(function(conn) {
                    return conn.connectionId === connectionId;
                });
                
                if (selectedConnection && selectedConnection.charts) {
                    createChartsFromServerData(selectedConnection.charts);
            } else {
                    // 선택된 연결에 차트 데이터가 없으면 차트 영역 비우기
                    $('#dynamic-charts-container').empty();
            }
        }
        }

        // 다음 새로고침 예약
        function scheduleNextRefresh() {
            clearConnectionRefreshTimeout();
            if (!isDashboardRunning()) {
                return;
            }
            connectionRefreshTimeout = setTimeout(function() {
                refreshConnectionStatus();
            }, 10000); // 10초마다 연결 상태 확인
        }

        // 연결 모니터링 중지
        function stopConnectionMonitoring() {
            clearConnectionRefreshTimeout();
        }
        
        // 차트 설정 초기 로딩
        function loadChartConfig() {
            $.ajax({
                url: '/Dashboard/chart-config',
                type: 'GET',
                success: function(response) {
                    if (response.success && response.charts) {
                        chartConfigData = { charts: response.charts };
                        chartConfigHash = response.chartConfigHash;
                    } else {
                        chartConfigData = null;
                        chartConfigHash = null;
                    }
                    
                    // 차트 설정 로딩 완료 후 차트 모니터링 시작
                    startChartMonitoring();
                },
                error: function() {
                    console.error('차트 설정 로드 실패');
                    chartConfigData = null;
                    chartConfigHash = null;
                    startChartMonitoring();
                }
            });
        }
        
        // 모니터링 템플릿 설정 초기 로딩
        function loadMonitoringTemplateConfig() {
            $.ajax({
                url: '/SystemConfig/getMonitoringTemplateConfig',
                type: 'GET',
                success: function(response) {
                    if (response.success && response.monitoringConfig) {
                        try {
                            monitoringConfigData = JSON.parse(response.monitoringConfig);
                            // hash는 monitoringTemplate API 응답에서 받아옴
                        } catch (e) {
                            console.error('모니터링 템플릿 설정 파싱 실패:', e);
                            monitoringConfigData = null;
                        }
                    } else {
                        monitoringConfigData = null;
                    }
                },
                error: function() {
                    console.error('모니터링 템플릿 설정 로드 실패');
                    monitoringConfigData = null;
                }
            });
        }
        
        // 차트 모니터링 시작
        function startChartMonitoring() {
            updateDynamicCharts();
        }
        
        // 동적 차트 업데이트 (통합 API 사용)
        function updateDynamicCharts() {
            if (!isDashboardRunning()) {
                scheduleChartUpdate();
                return;
            }

            // 이전 요청이 있으면 취소
            if (chartUpdateAbortController) {
                chartUpdateAbortController.abort();
            }
            chartUpdateAbortController = new AbortController();

            $.ajax({
                url: '/Dashboard/getIntegratedData',
                type: 'POST',
                signal: chartUpdateAbortController.signal,
                success: function(response) {
                    if (!isDashboardRunning()) {
                        return;
                    }

                    if (response.success && response.connections) {
                        // API 응답 데이터 캐시에 저장
                        cachedApiResponse = response;
                        
                        // hash 변경 감지 및 재렌더링
                        if (response.chartConfigHash && response.chartConfigHash !== chartConfigHash) {
                            // hash가 변경되었으면 차트 설정 다시 로드하고 전체 재렌더링
                            chartConfigHash = response.chartConfigHash;
                            
                            // 차트 설정 다시 로드
                            $.ajax({
                                url: '/Dashboard/chart-config',
                                type: 'GET',
                                success: function(configResponse) {
                                    if (configResponse.success && configResponse.charts) {
                                        chartConfigData = { charts: configResponse.charts };
                                        
                                        // 모든 차트 제거 후 재생성
                                        $('#dynamicChartsContainer').empty();
                                        if (window.chartInstances) {
                                            for (var chartId in window.chartInstances) {
                                                if (window.chartInstances.hasOwnProperty(chartId)) {
                                                    try {
                                                        window.chartInstances[chartId].destroy();
                                                    } catch (e) {
                                                        console.error('차트 인스턴스 정리 중 오류:', e);
                                                    }
                                                    delete window.chartInstances[chartId];
                                                }
                                            }
                                            window.chartInstances = {};
                                        }
                                        
                                        // 차트 재생성
                                        if (selectedConnectionId) {
                                            var selectedConnection = response.connections.find(function(conn) {
                                                return conn.connectionId === selectedConnectionId;
                                            });
                                            if (selectedConnection && selectedConnection.charts) {
                                                createChartsFromServerData(selectedConnection.charts);
                                            }
                                        } else if (response.connections.length > 0) {
                                            var connectedConnection = response.connections.find(function(conn) {
                                                return conn.status === 'connected';
                                            });
                                            if (connectedConnection && connectedConnection.charts) {
                                                selectConnection(connectedConnection.connectionId);
                                            } else if (connectedConnection) {
                                                selectConnection(connectedConnection.connectionId);
                                            }
                                        }
                                        
                                        // 차트 설정 변경 시 모든 연결의 신호등 재생성 및 색상 업데이트
                                        response.connections.forEach(function(conn) {
                                            createDynamicTrafficLights(conn.connectionId);
                                            // 신호등 재생성 후 즉시 색상 업데이트
                                            if (conn.charts) {
                                                updateTrafficLightsFromDynamicData(conn.charts, conn.connectionId);
                                            }
                                        });
                                    }
                                },
                                error: function() {
                                    console.error('차트 설정 재로드 실패');
                                }
                            });
                            return;
                        }
                        
                        // 신호등이 없을 때만 생성 (차트 설정 변경 시에는 이미 재생성됨)
                        response.connections.forEach(function(conn) {
                            var trafficLightContainer = $('#traffic-lights-' + conn.connectionId);
                            if (trafficLightContainer.length === 0 || trafficLightContainer.children().length === 0) {
                                createDynamicTrafficLights(conn.connectionId);
                            }
                        });
                        
                        // 차트는 선택된 연결만 업데이트
                        if (selectedConnectionId) {
                            var selectedConnection = response.connections.find(function(conn) {
                                return conn.connectionId === selectedConnectionId;
                            });
                            
                            if (selectedConnection && selectedConnection.charts) {
                                createChartsFromServerData(selectedConnection.charts);
                            }
                        } else if (response.connections.length > 0) {
                            // 선택된 연결이 없으면 "연결됨" 상태인 첫 번째 연결을 기본으로 선택
                            var connectedConnection = response.connections.find(function(conn) {
                                return conn.status === 'connected';
                            });
                            
                            if (connectedConnection && connectedConnection.charts) {
                                selectConnection(connectedConnection.connectionId);
                            } else if (connectedConnection) {
                                // 연결됨 상태이지만 차트가 없는 경우에도 선택
                                selectConnection(connectedConnection.connectionId);
                            }
                        }
                        
                        // 신호등은 모든 연결 업데이트
                        response.connections.forEach(function(conn) {
                            if (conn.charts) {
                                updateTrafficLightsFromDynamicData(conn.charts, conn.connectionId);
                            }
                        });
                    }
                    chartUpdateAbortController = null;
                },
                error: function(xhr, status, error) {
                    // AbortError는 무시 (요청이 취소된 경우)
                    if (xhr.statusText === 'abort' || error === 'abort') {
                        return;
                    }
                    console.error('통합 데이터 조회 실패');
                    chartUpdateAbortController = null;
                },
                complete: function() {
                    scheduleChartUpdate();
                }
            });
        }

        function scheduleChartUpdate() {
            clearChartRefreshTimeout();
            if (!isDashboardRunning()) {
                return;
            }
            chartRefreshTimeout = setTimeout(function() {
                updateDynamicCharts();
            }, 10000);
        }
        
        // 차트 데이터만 업데이트 (기존 차트 유지)
        function updateChartData(elementId, chartType, data) {
            // elementId는 이미 templateId__chart_type__chartType 형식
            
            // 차트 데이터 형식 검증
            var validationResult = validateChartDataFormat(data);
            if (!validationResult.isValid) {
                // validation 실패 시 에러 차트로 변경
                var existingChart = $('#' + elementId);
                if (existingChart.length > 0) {
                    var chartElement = existingChart.closest('.col-md-3');
                    if (window.chartInstances && window.chartInstances[elementId]) {
                        window.chartInstances[elementId].destroy();
                        delete window.chartInstances[elementId];
                    }
                    chartElement.remove();
                }
                
                // 에러 차트 생성
                var chartsContainer = $('#dynamicChartsContainer');
                var chartIndex = chartsContainer.find('.col-md-3').length;
                        var chartHtml = createErrorChartHtml(elementId, chartIndex, validationResult.message);
                var $newErrorChart = $(chartHtml);
                chartsContainer.append($newErrorChart);
                return;
            }
            
            if (chartType === 'text') {
                // 텍스트 차트 업데이트
                var container = document.getElementById(elementId);
                if (container) {
                    var chartData = parseChartData(data);
                    if (chartData.error) {
                        return; // 에러가 있으면 업데이트하지 않음
                    }
                    var value = chartData.values[0];
                    var color = chartData.colors[0];
                    container.innerHTML = '<div class="text-center" style="padding: 20px;">' +
                        '<div style="margin: 10px 0; font-size: 96px; cursor: pointer;" onclick="handleChartElementClick(\'' + elementId + '\', null)">' +
                        '<span style="color: ' + color + '; font-weight: bold;">' + value + '</span> ' +
                        '</div>' +
                        '</div>';
                }
            } else {
                // Chart.js 차트 업데이트
                var chartInstance = window.chartInstances && window.chartInstances[elementId];
                if (chartInstance) {
                    var chartData = parseChartData(data);
                    if (chartData.error) {
                        return; // 에러가 있으면 업데이트하지 않음
                    }
                    
                    if (chartType === 'gauge') {
                        // 게이지 차트는 특별한 데이터 구조 사용
                        var value = chartData.values[0] || 0;
                        chartInstance.data.labels = chartData.labels;
                        chartInstance.data.datasets[0].data = [value, 100 - value];
                        chartInstance.data.datasets[0].backgroundColor = [chartData.colors[0], '#f0f0f0'];
                    } else {
                        // 일반 차트 (도넛, 바)
                        chartInstance.data.labels = chartData.labels;
                        chartInstance.data.datasets[0].data = chartData.values;
                        chartInstance.data.datasets[0].backgroundColor = chartData.colors2;
                        chartInstance.data.datasets[0].borderColor = chartData.colors;
                    }
                    
                    // annotation 플러그인이 로드되어 있고 gauge 차트가 아니면 비활성화
                    if (chartPluginsLoaded.annotation && chartType !== 'gauge') {
                        if (!chartInstance.options.plugins) {
                            chartInstance.options.plugins = {};
                        }
                        chartInstance.options.plugins.annotation = false;
                    }
                    
                    chartInstance.update();
                }
            }
        }

        // 서버 데이터로 차트 동적 생성
        function createChartsFromServerData(chartsData) {
            var chartsContainer = $('#dynamicChartsContainer');
            
            // chartConfig의 order 기준으로 정렬된 차트 목록 생성
            var sortedCharts = [];
            if (chartConfigData && chartConfigData.charts) {
                // chartConfig에서 차트 목록을 order 기준으로 정렬
                var configCharts = chartConfigData.charts.slice().sort(function(a, b) {
                    return (a.order || 0) - (b.order || 0);
                });
                
                // 정렬된 차트 목록을 순회하며 데이터가 있는 차트만 추가
                configCharts.forEach(function(chart) {
                    var templateId = chart.templateId;
                    var chartType = chart.chartType || 'text';
                    // 차트 키: templateId__chart_type__chartType
                    var chartKey = templateId + '__chart_type__' + chartType;
                    var data = chartsData[chartKey];
                    if (data) {
                        sortedCharts.push({
                            templateId: templateId,
                            chartType: chartType,
                            chartKey: chartKey,
                            data: data
                        });
                    }
                });
            } else {
                // chartConfig가 없으면 기존 방식으로 처리 (하위 호환성)
                for (var chartKey in chartsData) {
                    var data = chartsData[chartKey];
                    if (data) {
                        // chartKey에서 templateId와 chartType 추출 시도
                        var templateId = null;
                        var chartType = 'text';
                        if (chartKey.includes('__chart_type__')) {
                            var parts = chartKey.split('__chart_type__');
                            templateId = parts[0];
                            chartType = parts[1] || 'text';
                        }
                        sortedCharts.push({
                            templateId: templateId,
                            chartType: chartType,
                            chartKey: chartKey,
                            data: data
                        });
                    }
                }
            }
            
            // 기존 차트 요소들을 맵으로 저장 (chartKey -> DOM 요소)
            var existingCharts = {};
            chartsContainer.find('[class*="col-md-"]').each(function() {
                var $chartElement = $(this);
                // 차트 ID 추출
                var elementId = $chartElement.find('[id^="chart_"], [id^="error_"], [id*="__chart_type__"]').attr('id');
                if (elementId) {
                    var chartKey;
                    if (elementId.startsWith('error_')) {
                        // error_ 접두사 제거
                        chartKey = elementId.substring(6); // 'error_'.length = 6
                    } else {
                        // elementId를 그대로 사용 (templateId__chart_type__chartType 형식)
                        chartKey = elementId;
                    }
                    existingCharts[chartKey] = $chartElement;
                }
            });
            
            // 전체 차트 개수 계산
            var totalChartCount = sortedCharts.length;
            // 줄 구성 계산
            var rowSizes = computeChartRowSizes(totalChartCount);
            
            // 정렬된 순서대로 차트 처리
            var chartIndex = 0;
            var hasValidCharts = false;
            var chartElementsInOrder = []; // 순서대로 정렬된 차트 요소들
            
            sortedCharts.forEach(function(chart) {
                var templateId = chart.templateId;
                var chartType = chart.chartType;
                var chartKey = chart.chartKey;
                var data = chart.data;
                
                if (!data) return;
                
                hasValidCharts = true;
                
                // DOM ID 생성: templateId__chart_type__chartType 형식
                var elementId = templateId + '__chart_type__' + chartType;
                
                // 기존 차트 요소 확인
                var existingChartElement = existingCharts[chartKey];
                var existingChart = $('#' + elementId);
                var existingChartType = existingChart.data('chart-type');
                
                // 에러 데이터 처리
                if (data.error) {
                    var existingErrorChart = $('#error_' + elementId);
                    
                    if (existingErrorChart.length > 0) {
                        // 기존 에러 차트가 있으면 내용만 업데이트하고 순서에 추가
                        var errorMessage = data.error;
                        var $boxBody = existingErrorChart.find('.box-body');
                        $boxBody.css({
                            'height': '249px',
                            'align-items': 'top',
                            'justify-content': 'center'
                        });
                        $boxBody.html(
                            '<div class="alert alert-danger" style="margin: 0;line-break: anywhere;height: 100%;">' +
                            '<i class="fa fa-exclamation-circle"></i> ' + errorMessage +
                            '</div>'
                        );
                        // 기존 클래스 제거 후 새로운 클래스 적용
                        var $errorChartWrapper = existingErrorChart.closest('[class*="col-md-"]');
                        var currentRow = 0;
                        var currentIndex = 0;
                        for (var i = 0; i < rowSizes.length; i++) {
                            if (chartIndex < currentIndex + rowSizes[i]) {
                                currentRow = i;
                                break;
                            }
                            currentIndex += rowSizes[i];
                        }
                        var rowSize = rowSizes[currentRow] || 4;
                        var newColClass = getChartColumnClassByRowSize(rowSize);
                        $errorChartWrapper.removeClass().addClass(newColClass);
                        chartElementsInOrder.push($errorChartWrapper);
                    } else {
                        // 기존 차트가 있으면 제거
                        if (existingChart.length > 0) {
                            var chartElement = existingChart.closest('[class*="col-md-"]');
                            if (window.chartInstances && window.chartInstances[elementId]) {
                                window.chartInstances[elementId].destroy();
                                delete window.chartInstances[elementId];
                            }
                            chartElement.remove();
                        }
                        
                        // 새 에러 차트 생성
                        var chartHtml = createErrorChartHtml(elementId, chartIndex, data.error, rowSizes);
                        var $newErrorChart = $(chartHtml);
                        chartsContainer.append($newErrorChart);
                        chartElementsInOrder.push($newErrorChart);
                    }
                    chartIndex++;
                    return;
                }
                
                // 차트 데이터 형식 검증
                var validationResult = validateChartDataFormat(data);
                if (!validationResult.isValid) {
                    var existingErrorChart = $('#error_' + elementId);
                    
                    if (existingErrorChart.length > 0) {
                        // 기존 에러 차트가 있으면 내용만 업데이트하고 순서에 추가
                        var errorMessage = validationResult.message;
                        var $boxBody = existingErrorChart.find('.box-body');
                        $boxBody.css({
                            'height': '249px',
                            'align-items': 'top',
                            'justify-content': 'center'
                        });
                        $boxBody.html(
                            '<div class="alert alert-danger" style="margin: 0;line-break: anywhere;height: 100%;">' +
                            '<i class="fa fa-exclamation-circle"></i> ' + errorMessage +
                            '</div>'
                        );
                        // 기존 클래스 제거 후 새로운 클래스 적용
                        var $errorChartWrapper = existingErrorChart.closest('[class*="col-md-"]');
                        var currentRow = 0;
                        var currentIndex = 0;
                        for (var j = 0; j < rowSizes.length; j++) {
                            if (chartIndex < currentIndex + rowSizes[j]) {
                                currentRow = j;
                                break;
                            }
                            currentIndex += rowSizes[j];
                        }
                        var rowSize2 = rowSizes[currentRow] || 4;
                        var newColClass2 = getChartColumnClassByRowSize(rowSize2);
                        $errorChartWrapper.removeClass().addClass(newColClass2);
                        chartElementsInOrder.push($errorChartWrapper);
                    } else {
                        // 기존 차트가 있으면 제거
                        if (existingChart.length > 0) {
                            var chartElement = existingChart.closest('[class*="col-md-"]');
                            if (window.chartInstances && window.chartInstances[elementId]) {
                                window.chartInstances[elementId].destroy();
                                delete window.chartInstances[elementId];
                            }
                            chartElement.remove();
                        }
                        
                        // 새 에러 차트 생성
                        var chartHtml = createErrorChartHtml(elementId, chartIndex, validationResult.message, rowSizes);
                        var $newErrorChart = $(chartHtml);
                        chartsContainer.append($newErrorChart);
                        chartElementsInOrder.push($newErrorChart);
                    }
                    chartIndex++;
                    return;
                }
                
                // 기존 오류 차트가 있는지 확인하고 제거
                var existingErrorChart = $('#error_' + elementId);
                if (existingErrorChart.length > 0) {
                    existingErrorChart.closest('[class*="col-md-"]').remove();
                }
                
                // 기존 차트가 없거나 타입이 다르면 새로 생성
                if (existingChart.length === 0 || existingChartType !== chartType) {
                    // 기존 차트가 있으면 제거
                    if (existingChart.length > 0) {
                        var chartElement = existingChart.closest('[class*="col-md-"]');
                        if (window.chartInstances && window.chartInstances[elementId]) {
                            window.chartInstances[elementId].destroy();
                            delete window.chartInstances[elementId];
                        }
                        chartElement.remove();
                    }
                    
                    // 차트 HTML 생성
                    var chartHtml = createChartHtml(elementId, chartType, chartIndex, rowSizes);
                    var $newChart = $(chartHtml);
                    chartsContainer.append($newChart);
                    chartElementsInOrder.push($newChart);
                    // 차트 초기화
                    initializeChart(elementId, chartType, data);
                } else {
                    // 기존 차트가 있고 타입이 같으면 값만 업데이트하고 순서에 추가
                    // 클래스도 업데이트 필요
                    var $chartWrapper = existingChart.closest('[class*="col-md-"]');
                    var currentRow3 = 0;
                    var currentIndex3 = 0;
                    for (var k = 0; k < rowSizes.length; k++) {
                        if (chartIndex < currentIndex3 + rowSizes[k]) {
                            currentRow3 = k;
                            break;
                        }
                        currentIndex3 += rowSizes[k];
                    }
                    var rowSize3 = rowSizes[currentRow3] || 4;
                    var newColClass3 = getChartColumnClassByRowSize(rowSize3);
                    $chartWrapper.removeClass().addClass(newColClass3);
                    updateChartData(elementId, chartType, data);
                    chartElementsInOrder.push($chartWrapper);
                }
                
                chartIndex++;
            });
            
            // 순서대로 차트 요소 재배치
            // 모든 차트 요소를 detach하고 순서대로 다시 append
            var $allChartElements = chartsContainer.find('[class*="col-md-"]').detach();
            chartElementsInOrder.forEach(function($chartElement) {
                if ($chartElement.length > 0) {
                    chartsContainer.append($chartElement);
                }
            });
            
            // 더 이상 존재하지 않는 차트 제거
            chartsContainer.find('[class*="col-md-"]').each(function() {
                var $chartElement = $(this);
                var elementId = $chartElement.find('[id^="chart_"], [id^="error_"], [id*="__chart_type__"]').attr('id');
                if (elementId) {
                    var chartKey;
                    if (elementId.startsWith('error_')) {
                        // error_ 접두사 제거
                        chartKey = elementId.substring(6); // 'error_'.length = 6
                    } else {
                        // elementId를 그대로 사용
                        chartKey = elementId;
                    }
                    
                    // sortedCharts에 없는 차트는 제거
                    var found = sortedCharts.some(function(chart) {
                        return chart.chartKey === chartKey;
                    });
                    
                    if (!found) {
                        // Chart.js 인스턴스 정리
                        if (window.chartInstances && window.chartInstances[elementId]) {
                            window.chartInstances[elementId].destroy();
                            delete window.chartInstances[elementId];
                        }
                        $chartElement.remove();
                    }
                }
            });
            
            // 유효한 차트가 없으면 메시지 표시
            if (!hasValidCharts) {
                chartsContainer.html(
                    '<div class="alert alert-info text-center" style="margin: 20px;" id="noChartMessage">' +
                    '<i class="fa fa-info-circle"></i> 설정된 차트가 없습니다. ' +
                    '<a href="/SystemConfig" target="_blank">환경설정</a>에서 차트를 설정해주세요.' +
                    '</div>'
                );
            } else {
                // 차트 정보가 있으면 noChartMessage 삭제
                $('#noChartMessage').remove();
            }
        }
        
        // 차트 설정에서 차트 타입 가져오기
        function getChartTypeFromConfig(elementId) {
            if (!chartConfigData || !chartConfigData.charts) {
                return 'text'; // 기본값
            }
            
            // elementId가 templateId__chart_type__chartType 형식인 경우 파싱
            if (elementId.includes('__chart_type__')) {
                var parts = elementId.split('__chart_type__');
                return parts[1] || 'text';
            }
            
            for (var i = 0; i < chartConfigData.charts.length; i++) {
                var chart = chartConfigData.charts[i];
                var templateId = elementId.includes('__chart_type__') ? elementId.split('__chart_type__')[0] : null;
                if (templateId && chart.templateId === templateId) {
                    return chart.chartType || 'text';
                }
            }
            
            return 'text'; // 찾지 못하면 기본값
        }
        
        // 차트 개수에 따라 줄 구성 계산
        function computeChartRowSizes(totalCount) {
            if (totalCount <= 0) return [];
            if (totalCount <= 4) return [totalCount];
            if (totalCount === 5) return [5];
            if (totalCount === 6) return [3, 3];

            var rows = [];
            var q = Math.floor(totalCount / 4);
            var r = totalCount % 4;

            if (r === 0) {
                for (var i = 0; i < q; i++) rows.push(4);
                return rows;
            }

            if (r === 3) {
                for (var j = 0; j < q; j++) rows.push(4);
                rows.push(3);
                return rows;
            }

            if (r === 1) {
                // 9 같은 케이스는 4+5
                if (totalCount === 9) return [4, 5];

                // ... + 4 + 4 + 4 + 1  ->  ... + 5 + 5 + 3
                for (var k = 0; k < q - 3; k++) rows.push(4);
                rows.push(5, 5, 3);
                return rows;
            }

            // r === 2
            // 10 같은 케이스는 5+5
            if (totalCount === 10) return [5, 5];

            // ... + 4 + 4 + 2  ->  ... + 5 + 5
            for (var m = 0; m < q - 2; m++) rows.push(4);
            rows.push(5, 5);
            return rows;
        }

        // 줄 크기에 따라 Bootstrap 컬럼 클래스 반환
        function getChartColumnClassByRowSize(rowSize) {
            if (rowSize === 1) return 'col-md-12';
            if (rowSize === 2) return 'col-md-6';
            if (rowSize === 3) return 'col-md-4';
            if (rowSize === 4) return 'col-md-3';
            if (rowSize === 5) return 'col-md-2 chart-col-5'; // 20%는 CSS로 보정
            return 'col-md-3';
        }

        // 차트 HTML 생성
        function createChartHtml(elementId, chartType, index, rowSizes) {
            var chartTitle = getChartTitle(elementId);
            
            var contentElement;
            if (chartType === 'text') {
                // 텍스트 차트는 div 요소 사용
                contentElement = '<div id="' + elementId + '" data-chart-type="' + chartType + '"></div>';
                } else {
                // 다른 차트는 canvas 요소 사용
                contentElement = '<canvas id="' + elementId + '" data-chart-type="' + chartType + '"></canvas>';
            }
            
            // 현재 차트가 속한 줄의 크기 계산
            var currentRow = 0;
            var currentIndex = 0;
            for (var i = 0; i < rowSizes.length; i++) {
                if (index < currentIndex + rowSizes[i]) {
                    currentRow = i;
                    break;
                }
                currentIndex += rowSizes[i];
            }
            var rowSize = rowSizes[currentRow] || 4;
            var colClass = getChartColumnClassByRowSize(rowSize);
            
            var html = '<div class="' + colClass + '">' +
                '<div class="box box-default">' +
                '<div class="box-header with-border">' +
                '<h3 class="box-title">' + chartTitle + '</h3>' +
                '</div>' +
                '<div class="box-body" style="height:250px; display:flex; align-items:center; justify-content:center">' +
                contentElement +
                '</div>' +
                '</div>' +
                '</div>';
            
            return html;
        }
        
        // 에러 차트 HTML 생성
        function createErrorChartHtml(elementId, index, errorMessage, rowSizes) {
            var chartTitle = getChartTitle(elementId) + ' (오류)';
            
            // 현재 차트가 속한 줄의 크기 계산
            var currentRow = 0;
            var currentIndex = 0;
            for (var i = 0; i < rowSizes.length; i++) {
                if (index < currentIndex + rowSizes[i]) {
                    currentRow = i;
                    break;
                }
                currentIndex += rowSizes[i];
            }
            var rowSize = rowSizes[currentRow] || 4;
            var colClass = getChartColumnClassByRowSize(rowSize);
            
            var html = '<div class="' + colClass + '">' +
                '<div class="box box-danger" id="error_' + elementId + '">' +
                '<div class="box-header with-border">' +
                '<h3 class="box-title"><i class="fa fa-exclamation-triangle"></i> ' + chartTitle + '</h3>' +
                '</div>' +
                '<div class="box-body" style="height:250px; align-items:top; justify-content:center">' +
                '<div class="alert alert-danger" style="margin: 0;line-break: anywhere;height: 100%;">' +
                '<i class="fa fa-exclamation-circle"></i> ' + errorMessage +
                '</div>' +
                '</div>' +
                '</div>' +
                '</div>';
            
            return html;
        }
        
        // 차트 초기화
        function initializeChart(elementId, chartType, data) {
            // elementId는 이미 templateId__chart_type__chartType 형식
            var canvasId = elementId;
            
            // Chart.js 인스턴스를 전역으로 저장
            if (!window.chartInstances) {
                window.chartInstances = {};
            }
            
            // 필요한 플러그인 로드 후 차트 초기화
            ensureChartPlugins(chartType, function() {
                if (chartType === 'doughnut') {
                    window.chartInstances[canvasId] = initializeDoughnutChart(canvasId, data);
                } else if (chartType === 'text') {
                    initializeTextChart(canvasId, data);
                } else if (chartType === 'gauge') {
                    window.chartInstances[canvasId] = initializeGaugeChart(canvasId, data);
                } else if (chartType === 'bar') {
                    window.chartInstances[canvasId] = initializeBarChart(canvasId, data);
                }
            });
        }
        
        // 도넛 차트 초기화
        function initializeDoughnutChart(canvasId, data) {
            var canvasElement = document.getElementById(canvasId);
            if (!canvasElement) {
                console.error('Canvas element not found: ' + canvasId);
                    return;
            }
            
            var ctx = canvasElement.getContext('2d');
            
            // Chart.js 인스턴스를 전역으로 저장
            if (!window.chartInstances) {
                window.chartInstances = {};
            }
            
            // 기존 차트 인스턴스가 있으면 정리
            if (window.chartInstances[canvasId]) {
                window.chartInstances[canvasId].destroy();
                delete window.chartInstances[canvasId];
            }
            
            var chartData = parseChartData(data);
            if (chartData.error) {
                console.error('Chart data validation failed:', chartData.errorMessage);
                return null;
            }
            var chartInstance = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        data: chartData.values,
                        backgroundColor: chartData.colors2,
                        borderColor: chartData.colors,
                        borderWidth: 2,
                        datalabels: {
                        	align: 'end',
                            anchor: 'end',
                            color: function(ctx) {
                                return ctx.dataset.borderColor;
                              },
                              font: {size: 18, weight:'bold'},
                              offset: 8,
                              opacity: function(ctx) {
                                return ctx.active ? 1 : 0.8;
                              }
                          }
                    }]
                },
                options: {
                    responsive: true,
                    //maintainAspectRatio: false,
                    plugins: {
                        legend: {
                        	display : false,
                            position: 'chartArea',
                            offset:9
                        },
                        // annotation 플러그인 비활성화 (doughnut 차트에서는 사용하지 않음)
                        annotation: false
                    },
                    //aspectRatio: 3 / 4,
                    layout: {
                        padding: 30
                    },
                    onClick: function(evt, elements) {
                        if (elements.length > 0) {
                            const element = elements[0];
                            const dataIndex = element.index;
                            
                            // 클릭한 요소의 데이터 행 정보 생성
                            const clickedData = {
                                index: dataIndex,
                                label: chartInstance.data.labels[dataIndex],
                                value: chartInstance.data.datasets[element.datasetIndex].data[dataIndex],
                                backgroundColor: chartInstance.data.datasets[element.datasetIndex].backgroundColor[dataIndex]
                            };
                            
                            handleChartElementClick(canvasId, clickedData);
                        }
                    }
                }
            });
            
            window.chartInstances[canvasId] = chartInstance;
            return chartInstance;
        }

        // 텍스트 차트 초기화
        function initializeTextChart(canvasId, data) {
            var container = document.getElementById(canvasId);
            if (!container) {
                console.error('Container element not found: ' + canvasId);
                return;
            }
            container.innerHTML = '';
            
            var chartData = parseChartData(data);
            if (chartData.error) {
                console.error('Chart data validation failed:', chartData.errorMessage);
                return;
            }
            var html = '<div class="text-center" style="padding: 20px;">';
            
            var value = chartData.values[0];
            var color = chartData.colors[0];
            html += '<div style="margin: 10px 0; font-size: 96px; cursor: pointer;" onclick="handleChartElementClick(\'' + canvasId + '\', null)">' +
                '<span style="color: ' + color + '; font-weight: bold;">' + value + '</span> ' +
                '</div>';
            
            html += '</div>';
            container.innerHTML = html;
        }
        
        // 게이지 차트 초기화
        function initializeGaugeChart(canvasId, data) {
            var canvasElement = document.getElementById(canvasId);
            if (!canvasElement) {
                console.error('Canvas element not found: ' + canvasId);
                        return;
            }
            
            var ctx = canvasElement.getContext('2d');
            
            // Chart.js 인스턴스를 전역으로 저장
            if (!window.chartInstances) {
                window.chartInstances = {};
            }
            
            // 기존 차트 인스턴스가 있으면 정리
            if (window.chartInstances[canvasId]) {
                window.chartInstances[canvasId].destroy();
                delete window.chartInstances[canvasId];
            }
            
            var chartData = parseChartData(data);
            if (chartData.error) {
                console.error('Chart data validation failed:', chartData.errorMessage);
                return null;
            }
            var value = chartData.values[0] || 0;
            
            var chartInstance = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        data: [value, 100 - value],
                        backgroundColor: [chartData.colors[0], '#f0f0f0'],
                        borderWidth: 0
                    }]
                },
                options: {
                    aspectRatio: 2,
                    circumference: 180,
                    rotation: -90,
                    plugins: {
                    	legend: {
                            display: false
                        },
                        annotation: {
                            annotations: {
                            	annotation:{
                            	  type: 'doughnutLabel',
                            	  content: ({chart}) => [
                               	   ( chart.data.datasets[0].data[0] || 0),
                               	  ],
                            	  drawTime: 'beforeDraw',
                            	  position: {
                            	    y: '-50%'
                            	  },
                                    font: [{size: 24, weight: 'bold'}, {size: 12}],
                                    color: ({chart}) => {
                                        return chart.data.datasets[0].backgroundColor[0];
                                    }
                            	}
                            }
                        }
                    },
                    onClick: function(evt, elements) {
                        if (elements.length > 0) {
                            const dataIndex = elements[0].index;
                            handleChartElementClick(canvasId, dataIndex);
                        }
                    }
                }
            });
            
            window.chartInstances[canvasId] = chartInstance;
            return chartInstance;
        }
        
        // 가로막대 차트 초기화
        function initializeBarChart(canvasId, data) {
            var canvasElement = document.getElementById(canvasId);
            if (!canvasElement) {
                console.error('Canvas element not found: ' + canvasId);
                return;
            }
            
            var ctx = canvasElement.getContext('2d');
            
            // Chart.js 인스턴스를 전역으로 저장
            if (!window.chartInstances) {
                window.chartInstances = {};
            }
            
            // 기존 차트 인스턴스가 있으면 정리
            if (window.chartInstances[canvasId]) {
                window.chartInstances[canvasId].destroy();
                delete window.chartInstances[canvasId];
            }
            
            var chartData = parseChartData(data);
            if (chartData.error) {
                console.error('Chart data validation failed:', chartData.errorMessage);
                return null;
            }
            var chartInstance = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        data: chartData.values,
                        backgroundColor: chartData.colors,
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y', // 가로막대 차트를 위해 추가
                    plugins: {
                        legend: {
                            display: false
                        },
                        // annotation 플러그인 비활성화 (bar 차트에서는 사용하지 않음)
                        annotation: false
                    },
                    onClick: function(evt, elements) {
                        if (elements.length > 0) {
                            const dataIndex = elements[0].index;
                            handleChartElementClick(canvasId, dataIndex);
                        }
                    },
                    scales: {
                        x: {
                            beginAtZero: true
                        }
                    }
                }
            });
            
            window.chartInstances[canvasId] = chartInstance;
            return chartInstance;
        }
        
        // 차트 데이터 형식 검증
        function validateChartDataFormat(data) {
        	
            // 기본 데이터 구조 확인
            if (!data) {
                return {
                    isValid: false,
                    templateId : data.templateId,
                    message: '차트 데이터가 없습니다. SQL 템플릿이 올바른 데이터를 반환하는지 확인해주세요.'
                };
            }
            
            if (data.result.length === 0) {
                return {
                    isValid: false,
                    templateId : data.templateId,
                    message: '차트 데이터가 비어있습니다. SQL 템플릿이 데이터를 반환하는지 확인해주세요.'
                };
            }
            
            // 각 행의 형식 확인
            for (var i = 0; i < data.result.length; i++) {
                var row = data.result[i];
                
                if (row.length < 3) {
                    return {
                        isValid: false,
                        templateId : data.templateId,
                        message: '차트 데이터 형식이 올바르지 않습니다. 각 행은 3개의 컬럼이 필요합니다. (상태, 이름, 값)'
                    };
                }
                
                // 세 번째 컬럼이 숫자인지 확인
                if (isNaN(parseFloat(row[2]))) {
                    return {
                        isValid: false,
                        templateId : data.templateId,
                        message: '차트 데이터 형식이 올바르지 않습니다. 세 번째 컬럼은 숫자여야 합니다.)'
                    };
                }
            }
            
            return { isValid: true };
        }
        
        // 차트 데이터 파싱
        function parseChartData(data) {
            // 데이터 형식 검증
            var validationResult = validateChartDataFormat(data);
            if (!validationResult.isValid) {
                // validation 실패 시 에러 정보 반환
                return {
                    error: true,
                    errorMessage: validationResult.message
                };
            }
            
            var labels = [];
            var values = [];
            var colors = [];
            var colors2 = [];
            
            if (data && data.result && Array.isArray(data.result)) {
                // 데이터를 10개로 제한
                var limitedData = data.result.slice(0, 10);
                
                limitedData.forEach(function(row) {
                    if (row && row.length >= 3) {
                        var status = row[0]; // 상태
                        var name = row[1];   // 이름
                        var value = parseFloat(row[2]) || 0; // 값
                        
                        labels.push(name);
                        values.push(value);
                        colors.push(getStatusColor(status));
                        colors2.push(getStatusColor2(status));
                    }
                });
            }
            
            return { 
                labels: labels,
                values: values,
                colors: colors,
                colors2: colors2
            };
        }
        
        
        
        // 차트 ID로 차트 제목 가져오기 (하위 호환성을 위한 중복 함수)
        function getChartTitle(elementId) {
            if (!chartConfigData || !chartConfigData.charts) {
                return elementId; // 설정이 없으면 elementId 반환
            }
            
            // elementId가 templateId__chart_type__chartType 형식인 경우 파싱
            var templateId = null;
            var chartType = null;
            if (elementId.includes('__chart_type__')) {
                var parts = elementId.split('__chart_type__');
                templateId = parts[0];
                chartType = parts[1];
            }
            
            for (var i = 0; i < chartConfigData.charts.length; i++) {
                var chart = chartConfigData.charts[i];
                if (chart.templateId === templateId && chart.chartType === chartType) {
                    // templateName이 있으면 사용, 없으면 templateId 사용
                    return chart.templateName || chart.templateId || elementId;
                }
            }
            
            return elementId; // 찾지 못하면 elementId 반환
        }
        
        // 동적 신호등 생성
        function createDynamicTrafficLights(connectionId) {
            var trafficLightContainer = $('#traffic-lights-' + connectionId);
            if (trafficLightContainer.length === 0) return;
            
            var lightHtml = '';
            
            if (chartConfigData && chartConfigData.charts) {
                // 차트 설정에 따라 신호등 생성
                chartConfigData.charts.forEach(function(chart, index) {
                    var title = chart.templateName || chart.templateId || '차트 ' + (index + 1);
                    lightHtml += '<div class="light chart-' + index + ' gray" title="' + title + '"></div>';
                });
            } else {
                // 차트 설정이 없으면 기본 신호등
                lightHtml = '<div class="light chart-0 gray" title="차트 1"></div>';
            }
            
            trafficLightContainer.html(lightHtml);
        }
        
        // 신호등 업데이트 (동적 차트 데이터 기반)
        function updateTrafficLightsFromDynamicData(chartsData, connectionId) {
            
            if (!connectionId) return;
            
            var trafficLightContainer = $('#traffic-light-' + connectionId);
            if (trafficLightContainer.length === 0) return;
            
            // 동적 차트 데이터를 신호등 형식으로 변환 (order 기준으로 정렬)
            var convertedData = {};
            var chartIndex = 0;
            
            // chartConfig의 order 기준으로 정렬된 차트 목록 생성
            var sortedCharts = [];
            if (chartConfigData && chartConfigData.charts) {
                // chartConfig에서 차트 목록을 order 기준으로 정렬
                var configCharts = chartConfigData.charts.slice().sort(function(a, b) {
                    return (a.order || 0) - (b.order || 0);
                });
                
                // 정렬된 차트 목록을 순회하며 데이터가 있는 차트만 추가
                configCharts.forEach(function(chart) {
                    // 새로운 키 형식: templateId__chart_type__chartType
                    var chartKey = chart.chartKey || (chart.templateId + '__chart_type__' + chart.chartType);
                    var data = chartsData[chartKey];
                    if (data) {
                        sortedCharts.push({
                            chartKey: chartKey,
                            data: data
                        });
                    }
                });
            } else {
                // chartConfig가 없으면 기존 방식으로 처리
                for (var chartKey in chartsData) {
                    var data = chartsData[chartKey];
                    if (data) {
                        sortedCharts.push({
                            chartKey: chartKey,
                            data: data
                        });
                    }
                }
            }
            
            // 정렬된 차트 목록을 순회하며 신호등 데이터 생성
            sortedCharts.forEach(function(chart) {
                var data = chart.data;
                if (data) {
                    if (data.result) {
                        // 정상 데이터
                        convertedData['chart_' + chartIndex] = { result: data.result };
                    } else if (data.error) {
                        // 에러 데이터 - 빨간색으로 표시
                        convertedData['chart_' + chartIndex] = { 
                            result: [['심각', '오류', data.error]]
                        };
                    }
                    chartIndex++;
                }
            });
            
            // 신호등 업데이트
            var allData = {};
            allData[connectionId] = convertedData;
            updateTrafficLightColors(allData);
        }
        
        // 신호등 색상 업데이트 함수 (동적 차트 기반)
        function updateTrafficLightColors(allData) {
        	
            for (var connectionId in allData) {
                var dbData = allData[connectionId];
                var trafficLightContainer = $('#traffic-light-' + connectionId);
                
                if (trafficLightContainer.length > 0) {
                    
                    // chartConfig의 order 기준으로 정렬된 차트 목록 생성
                    var sortedCharts = [];
                    if (chartConfigData && chartConfigData.charts) {
                        // chartConfig에서 차트 목록을 order 기준으로 정렬
                        var configCharts = chartConfigData.charts.slice().sort(function(a, b) {
                            return (a.order || 0) - (b.order || 0);
                        });
                        
                        // 정렬된 차트 목록을 순회하며 데이터가 있는 차트만 추가
                        configCharts.forEach(function(chart, index) {
                            var chartDataKey = 'chart_' + index;
                            var chartData = dbData[chartDataKey];
                            
                            if (chartData) {
                                // 새로운 키 형식: templateId__chart_type__chartType
                                var chartKey = chart.chartKey || (chart.templateId + '__chart_type__' + chart.chartType);
                                sortedCharts.push({
                                    chartKey: chartKey,
                                    data: chartData
                                });
                            }
                        });
                    } else {
                        // chartConfig가 없으면 기존 방식으로 처리
                        for (var chartKey in dbData) {
                            var chartData = dbData[chartKey];
                            if (chartData) {
                                sortedCharts.push({
                                    chartKey: chartKey,
                                    data: chartData
                                });
                            }
                        }
                    }
                    
                    // 정렬된 차트 목록을 순회하며 신호등 업데이트
                    var chartIndex = 0;
                    sortedCharts.forEach(function(chart) {
                        var lightClass = 'chart-' + chartIndex;
                        updateSingleTrafficLight(trafficLightContainer, lightClass, chart.data?.result, 'doughnut');
                        chartIndex++;
                    });
                }
            }
        }
        
        // 단일 신호등 업데이트
        function updateSingleTrafficLight(container, lightClass, data, chartType) {
            var light = container.find('.' + lightClass);
            if (light.length === 0) return;
            
            var status = 'gray';
            
            if (data && Array.isArray(data) && data.length > 0) {
                var firstRow = data[0];
                if (firstRow && firstRow.length >= 3) {
                    var statusValue = firstRow[0];
                    switch(statusValue) {
                        case '정상': status = 'green'; break;
                        case '경고': status = 'yellow'; break;
                        case '심각': status = 'red'; break;
                        default: status = 'gray';
                    }
                }
            }
            
            // 기존 클래스 제거
            light.removeClass('green yellow red gray');
            // 새 상태 클래스 추가
            light.addClass(status);
        }
        
        // 모니터링 템플릿 새로고침
        function refreshMonitoringTemplate() {
            if (!isDashboardRunning()) {
                clearMonitoringTemplateTimeout();
                return;
            }

            // 이전 요청이 있으면 취소
            if (monitoringTemplateAbortController) {
                monitoringTemplateAbortController.abort();
            }
            monitoringTemplateAbortController = new AbortController();

            // 모니터링 템플릿 설정 조회 (권한 체크를 위해 templateId 필요)
            $.ajax({
                url: '/SystemConfig/getMonitoringTemplateConfig',
                type: 'GET',
                success: function(configResponse) {
                    if (!isDashboardRunning()) {
                        return;
                    }
                    
                    var templateId = null;
                    if (configResponse.success && configResponse.monitoringConfig) {
                        try {
                            var config = JSON.parse(configResponse.monitoringConfig);
                            templateId = config.templateId;
                            
                            // 설정 데이터 캐시 업데이트
                            monitoringConfigData = config;
                            var templateName = config.templateName || '모니터링 템플릿';
                            
                            // box-title 업데이트
                            var section = $('#monitoringTemplateSection');
                            var boxTitle = section.find('.box-title');
                            boxTitle.html('<i class="fa fa-table"></i> ' + templateName);
                        } catch (e) {
                            console.error('모니터링 템플릿 설정 파싱 실패:', e);
                        }
                    }
                    
                    // 모니터링 템플릿 데이터 조회 (templateId와 함께 권한 체크)
                    $.ajax({
                        type: 'POST',
                        url: '/Dashboard/monitoringTemplate',
                        data: templateId ? { templateId: templateId } : {},
                        signal: monitoringTemplateAbortController.signal,
                        success: function(result) {
                            if (!isDashboardRunning()) {
                                return;
                            }
                            
                            // hash 변경 감지
                            if (result.monitoringConfigHash && result.monitoringConfigHash !== monitoringConfigHash) {
                                monitoringConfigHash = result.monitoringConfigHash;
                            } else if (!monitoringConfigHash && result.monitoringConfigHash) {
                                monitoringConfigHash = result.monitoringConfigHash;
                            }
                            
                            // 권한 없음 체크
                            if (!result.success && result.error && result.error.indexOf('권한') > -1) {
                                // 권한 없음 - 해당 템플릿 섹션 숨기기
                                $('#monitoringTemplateSection').hide();
                            } else {
                                // 권한 있음 또는 기타 응답
                                updateMonitoringTemplateDisplay(result);
                            }
                            
                            scheduleMonitoringTemplateRefresh();
                            monitoringTemplateAbortController = null;
                        },
                        error: function(xhr, status, error) {
                            // AbortError는 무시 (요청이 취소된 경우)
                            if (xhr.statusText === 'abort' || error === 'abort') {
                                return;
                            }
                            console.error('모니터링 템플릿 조회 실패:', error);
                            scheduleMonitoringTemplateRefresh();
                            monitoringTemplateAbortController = null;
                        }
                    });
                },
                error: function() {
                    console.error('모니터링 템플릿 설정 조회 실패');
                    scheduleMonitoringTemplateRefresh();
                    monitoringTemplateAbortController = null;
                }
            });
        }
        
        function scheduleMonitoringTemplateRefresh() {
            clearMonitoringTemplateTimeout();
            if (!isDashboardRunning()) {
                return;
            }
            // 5초마다 새로고침
            monitoringTemplateTimeout = setTimeout(function() {
                refreshMonitoringTemplate();
            }, 5000);
        }
        
        // 모니터링 템플릿 표시 업데이트
        function updateMonitoringTemplateDisplay(result) {
            var container = $('#monitoringTemplateContainer');
            var section = $('#monitoringTemplateSection');
            var boxTitle = section.find('.box-title');
            
            // 설정이 없으면 섹션 숨김
            if (!result || !result.hasConfig) {
                section.hide();
                return;
            }
            
            // 설정이 있으면 섹션 표시
            section.show();
            
            // 에러 체크
            if (!result.success || !result.data) {
                container.html('<div class="alert alert-warning text-center">모니터링 템플릿 데이터를 불러올 수 없습니다.</div>');
                return;
            }
            
            var data = result.data;
            
            // 에러 결과인 경우
            if (data.error) {
                container.html('<div class="alert alert-danger text-center">' + data.error + '</div>');
                return;
            }
            
            // 성공 결과인 경우
            if (data.success && data.result) {
                var rows = data.result;
                var rowhead = data.rowhead || [];
                
                if (!rows || rows.length === 0) {
                    container.html('<div class="alert alert-info text-center">데이터가 없습니다.</div>');
                    return;
                }
                
                // 컬럼명 추출 (rowhead에서 title 추출)
                var columns = [];
                if (rowhead && rowhead.length > 0) {
                    rowhead.forEach(function(head) {
                        columns.push(head.title || '컬럼');
                    });
                } else {
                    // rowhead가 없으면 첫 번째 행의 길이만큼 컬럼명 생성
                    if (rows.length > 0 && rows[0]) {
                        for (var i = 0; i < rows[0].length; i++) {
                            columns.push('컬럼' + (i + 1));
                        }
                    }
                }
                
                // 테이블 생성
                var html = '<div class="table-responsive">' +
                    '<table class="table table-striped table-hover" id="monitoringTemplateTable">' +
                    '<thead><tr>';
                
                columns.forEach(function(col) {
                    html += '<th>' + col + '</th>';
                });
                
                html += '</tr></thead><tbody>';
                
                rows.forEach(function(row, rowIndex) {
                    html += '<tr data-row-index="' + rowIndex + '" style="cursor: pointer;">';
                    if (row) {
                        row.forEach(function(cell) {
                            html += '<td>' + (cell !== null && cell !== undefined ? cell : '-') + '</td>';
                        });
                    }
                    html += '</tr>';
                });
                
                html += '</tbody></table></div>';
                container.html(html);
                
                // 행 클릭 이벤트 등록
                $('#monitoringTemplateTable tbody tr').on('click', function() {
                    var rowIndex = $(this).data('row-index');
                    handleMonitoringTemplateRowClick(rowIndex, rows[rowIndex]);
                });
            } else {
                container.html('<div class="alert alert-warning text-center">데이터 형식이 올바르지 않습니다.</div>');
            }
        }
        
        // 모니터링 템플릿 행 클릭 처리
        function handleMonitoringTemplateRowClick(rowIndex, rowData) {
            // 캐시된 모니터링 템플릿 설정에서 템플릿 ID 조회
            if (!monitoringConfigData || !monitoringConfigData.templateId) {
                showToast('템플릿 정보를 찾을 수 없습니다.', 'warning');
                return;
            }
            
            var templateId = monitoringConfigData.templateId;
            
            // 단축키 정보 조회
            $.ajax({
                type: 'POST',
                url: '/SQLTemplate/shortcuts',
                data: {
                    templateId: templateId
                },
                success: function(result) {
                    if (result.success && result.data && result.data.length > 0) {
                        // 활성화된 첫 번째 단축키 찾기
                        var firstActiveShortcut = null;
                        for (var i = 0; i < result.data.length; i++) {
                            var shortcut = result.data[i];
                            if (shortcut.IS_ACTIVE === true) {
                                firstActiveShortcut = shortcut;
                                break;
                            }
                        }
                        
                        if (firstActiveShortcut) {
                            // 단축키 실행 - 소스 컬럼 인덱스에 따라 파라미터 생성
                            var sourceColumns = firstActiveShortcut.SOURCE_COLUMN_INDEXES;
                            var parameterString = '';
                            
                            if (sourceColumns && sourceColumns.trim() !== '') {
                                var columnParts = sourceColumns.split(',');
                                var paramValues = [];
                                
                                for (var i = 0; i < columnParts.length; i++) {
                                    var trimmedPart = columnParts[i].trim();
                                    
                                    if (trimmedPart === '') {
                                        paramValues.push('');
                                        continue;
                                    }
                                    
                                    // 작은따옴표로 감싼 값은 상수값으로 처리
                                    if (trimmedPart.startsWith("'") && trimmedPart.endsWith("'") && trimmedPart.length > 1) {
                                        paramValues.push(trimmedPart.substring(1, trimmedPart.length - 1));
                                    } else if (/^\d+$/.test(trimmedPart)) {
                                        // 숫자인 경우 컬럼 인덱스로 처리 (1-based를 0-based로 변환)
                                        var columnIndex = parseInt(trimmedPart) - 1;
                                        if (rowData && rowData[columnIndex] !== undefined) {
                                            paramValues.push(rowData[columnIndex]);
                                        } else {
                                            paramValues.push('');
                                        }
                                    } else {
                                        paramValues.push('');
                                    }
                                }
                                
                                parameterString = paramValues.join(',');
                            }
                            
                            // 모니터링 템플릿 설정에서 연결 ID 가져오기
                            var connectionId = monitoringConfigData.connectionId;
                            
                            // SQLExecute.jsp의 sendSql 함수 호출 방식과 동일
                            sendSql(firstActiveShortcut.TARGET_TEMPLATE_ID + "&" + parameterString + "&" + firstActiveShortcut.AUTO_EXECUTE, connectionId);
                            
                        } else {
                            console.log('활성화된 단축키가 없습니다.');
                            showToast('활성화된 단축키가 없습니다.', 'info');
                        }
                    } else {
                        console.log('단축키 정보를 찾을 수 없습니다.');
                        showToast('단축키 정보를 찾을 수 없습니다.', 'warning');
                    }
                },
                error: function() {
                    showToast('단축키 정보 조회에 실패했습니다.', 'error');
                }
            });
        }
        
        // 모니터링 템플릿 모니터링 시작
        function startMonitoringTemplateMonitoring() {
            refreshMonitoringTemplate();
        }
        
        // 모니터링 템플릿 모니터링 중지
        function stopMonitoringTemplateMonitoring() {
            clearMonitoringTemplateTimeout();
        }
        
        function reload() {
        	
        	 var hasErrorCharts = $('#dynamicChartsContainer .box-danger').length > 0;
        	 
        	// 관리자 권한 확인
        	if (isAdmin && hasErrorCharts) {
	            if (confirm('에러 상태인 차트가 있습니다.\n\n모든 차트의 에러 상태를 리셋하시겠습니까?\n\n이 작업은 에러로 인해 중단된 차트들을 즉시 재시작합니다.')) {
	                $.ajax({
	                    url: '/SystemConfig/resetChartErrors',
	                    type: 'POST',
	                    success: function(response) {
	                        if (response.success) {
	                        	location.reload();
	                        } else {
	                            showToast('에러 상태 리셋에 실패했습니다: ' + response.message, 'error');
	                        }
	                    },
	                    error: function() {
	                        showToast('에러 상태 리셋 중 오류가 발생했습니다.', 'error');
	                    }
	                });
                }
        	}else{
        		location.reload();
        	}
		}
        
    </script>
        
<div class="content-wrapper" style="margin-left: 0">
    <section class="content-header">
        <h1><i class="fa fa-dashboard"></i> 대시보드</h1>
		</section>
		
    <section class="content">
        <!-- 연결 상태 모니터링 -->
            <div class="row">
                <div class="col-md-12">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-database"></i> 데이터베이스 연결 상태 모니터링
                            </h3>
                            <div class="box-tools pull-right">
                                <button type="button" class="btn btn-box-tool" onclick="reload()">
                                    <i class="fa fa-refresh"></i> 새로고침
                                </button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div id="connectionStatusContainer" class="row">
                                <!-- 연결 상태가 여기에 동적으로 추가됩니다 -->
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        <!-- 동적 차트 영역 -->
            <div class="row">
            	<div id="dynamicChartsContainer">
                <!-- 동적 차트들이 여기에 생성됩니다 -->
                </div>
            </div>

        <!-- 모니터링 템플릿 섹션 -->
        <div class="row" id="monitoringTemplateSection" style="display: none;">
            <div class="col-md-12">
                <div class="box box-default">
                    <div class="box-header with-border">
                        <h3 class="box-title">
                            <i class="fa fa-table"></i> 모니터링 템플릿
                        </h3>
                    </div>
                    <div class="box-body">
                        <div id="monitoringTemplateContainer">
                            <!-- 모니터링 템플릿 데이터가 여기에 표시됩니다 -->
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    
    <!-- ParamForm 추가 (sendSql 방식 지원) -->
    <form role="form-horizontal" name="ParamForm" id="ParamForm" action="javascript:void(0);" style="display: none;">
        <input type="hidden" id="sendvalue" name="sendvalue">
        <input id="Path" name="Path" value="" type="hidden">
    </form>
        </div>
    
    <style>
        /* 연결 카드 스타일 */
        .connection-card-wrapper {
            position: relative;
            margin: 5px 0;
        }
        
        .connection-card {
            transition: all 0.3s ease;
            position: relative;
        }

        .connection-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.15);
        }
        
        /* 순서 변경 컨트롤 */
        .connection-card {
            position: relative;
        }
        
        .connection-card .btn-order {
            position: absolute;
            top: 50%;
            transform: translateY(-50%);
            width: 28px;
            height: 28px;
            padding: 0;
            cursor: pointer;
            display: none;
            align-items: center;
            justify-content: center;
            text-decoration: none;
            color: #666;
            transition: all 0.2s ease;
            z-index: 10;
        }
        
        .connection-card:hover .btn-order {
            display: flex;
        }
        
        .connection-card .btn-order-left {
            left: 0px;
        }
        
        .connection-card .btn-order-right {
            right: 0px;
        }
        
        .connection-card .btn-order:hover {
            color: #333;
            transform: translateY(-50%) scale(1.2);
        }
        
        .connection-card .btn-order:active {
            transform: translateY(-50%) scale(0.9);
        }
        
        .connection-card .btn-order i {
            font-size: 16px;
        }

/* 신호등 스타일 */
.traffic-light {
            display: flex;
            justify-content: center;
            align-items: center;
    gap: 5px;
    margin-top: 10px;
        }
        
        .light {
    width: 12px;
    height: 12px;
            border-radius: 50%;
    border: 2px solid #ddd;
    transition: all 0.3s ease;
        }
        
        .light.green {
            background-color: rgba(140, 214, 16, 0.7);
    border-color: #8cd60f;
            box-shadow: 0 0 4px rgba(40, 167, 69, 0.5);
        }
        
        .light.yellow {
    background-color: rgba(255, 193, 7, 0.7);
    border-color: #ffc107;
            box-shadow: 0 0 4px rgba(255, 193, 7, 0.5);
        }
        
        .light.red {
    background-color: rgba(231, 24, 49, 0.7);
    border-color: #dc3545;
            box-shadow: 0 0 4px rgba(220, 53, 69, 0.5);
        }
        
        .light.gray {
            background-color: rgba(158, 158, 158, 0.7);
    border-color: #6c757d;
        }
        
        /* 호버 효과 */
        .traffic-light:hover .light {
            transform: scale(1.2);
        }
    </style>
    
    <script>
        // 차트 요소 클릭 처리 함수
        function handleChartElementClick(canvasId, rowIndex) {
            // rowIndex가 객체인 경우 (도넛차트에서 전달된 경우) index 추출
            var actualRowIndex = rowIndex;
            if (rowIndex && typeof rowIndex === 'object' && rowIndex.index !== undefined) {
                actualRowIndex = rowIndex.index;
            } else if (rowIndex === null || rowIndex === undefined) {
                // 텍스트 차트는 첫 번째 행(인덱스 0) 사용
                actualRowIndex = 0;
            }
            
            // canvasId에서 templateId 추출 (templateId__chart_type__chartType 형식)
            var templateId = null;
            if (canvasId && canvasId.includes('__chart_type__')) {
                templateId = canvasId.split('__chart_type__')[0];
            } else if (chartConfigData && chartConfigData.charts) {
                // 하위 호환성: 기존 형식인 경우
                for (var i = 0; i < chartConfigData.charts.length; i++) {
                    var chart = chartConfigData.charts[i];
                    if (chart.id === canvasId) {
                        templateId = chart.templateId;
                        break;
                    }
                }
            }
            
            if (!templateId) {
                console.error('템플릿 ID를 찾을 수 없습니다:', canvasId);
                showToast('차트 정보를 찾을 수 없습니다. 페이지를 새로고침해주세요.', 'warning');
                return;
            }
            
            // 클릭한 행의 데이터를 파라미터로 변환
            var parameters = convertRowIndexToParameters(canvasId, actualRowIndex);
            
            // 단축키 정보 조회
            $.ajax({
                type: 'POST',
                url: '/SQLTemplate/shortcuts',
                data: {
                    templateId: templateId
                },
                success: function(result) {
                    if (result.success && result.data && result.data.length > 0) {
                        // 활성화된 첫 번째 단축키 찾기
                        var firstActiveShortcut = null;
                        for (var i = 0; i < result.data.length; i++) {
                            var shortcut = result.data[i];
                            if (shortcut.IS_ACTIVE === true) {
                                firstActiveShortcut = shortcut;
                                break;
                            }
                        }
                        
                        if (firstActiveShortcut) {
                            
                            // 단축키 실행 - 소스 컬럼 인덱스에 따라 파라미터 생성
                            var sourceColumns = firstActiveShortcut.SOURCE_COLUMN_INDEXES;
                            var parameterString = '';
                            
                            if (sourceColumns && sourceColumns.trim() !== '') {
                                var columnParts = sourceColumns.split(',');
                                var paramValues = [];
                                
                                // 인덱스를 유지하기 위해 for 루프 사용
                                for (var i = 0; i < columnParts.length; i++) {
                                    var trimmedPart = columnParts[i].trim();
                                    
                                    // 빈 값은 빈 문자열로 추가 (위치 유지)
                                    if (trimmedPart === '') {
                                        paramValues.push('');
                                        continue;
                                    }
                                    
                                    // 작은따옴표로 감싼 값은 상수값으로 처리
                                    if (trimmedPart.startsWith("'") && trimmedPart.endsWith("'") && trimmedPart.length > 1) {
                                        // 작은따옴표 제거하고 상수값으로 사용
                                        paramValues.push(trimmedPart.substring(1, trimmedPart.length - 1));
                                    } else if (/^\d+$/.test(trimmedPart)) {
                                        // 숫자인 경우 컬럼 인덱스로 처리 (1-based를 0-based로 변환)
                                        var columnIndex = parseInt(trimmedPart) - 1;
                                        if (parameters && Object.keys(parameters).length > 0) {
                                            var paramKeys = Object.keys(parameters);
                                            if (paramKeys[columnIndex] !== undefined) {
                                                paramValues.push(parameters[paramKeys[columnIndex]]);
                                            } else {
                                                paramValues.push('');
                                            }
                                        } else {
                                            paramValues.push('');
                                        }
                                    } else {
                                        // 기타 경우 빈 문자열
                                        paramValues.push('');
                                    }
                                }
                                
                                parameterString = paramValues.join(',');
                            }
                       
                            // SQLExecute.jsp의 sendSql 함수 호출 방식과 동일
                            // common.jsp의 sendSql 함수 사용
                            // 선택된 연결 ID 가져오기
                            var selectedConnectionId = getSelectedConnectionId();
                            
                            sendSql(firstActiveShortcut.TARGET_TEMPLATE_ID + "&" + parameterString + "&" + firstActiveShortcut.AUTO_EXECUTE, selectedConnectionId);
                            
                        } else {
                            console.log('활성화된 단축키가 없습니다.');
                            showToast('이 차트에 설정된 단축키가 없습니다. 템플릿 관리에서 단축키를 설정해주세요.', 'info');
                        }
                    } else {
                        console.log('단축키가 없습니다.');
                        showToast('이 차트에 설정된 단축키가 없습니다. 템플릿 관리에서 단축키를 설정해주세요.', 'info');
                    }
                },
                error: function() {
                    console.error('단축키 정보 조회 실패');
                    showToast('단축키 정보 조회에 실패했습니다.', 'error');
                }
            });
        }
        
        // 행 인덱스를 파라미터로 변환
        function convertRowIndexToParameters(canvasId, rowIndex) {
            var parameters = {};
            
            // 원본 차트 데이터에서 해당 행의 전체 데이터 가져오기
            var chartData = getChartDataFromCache(canvasId);
            
            if (chartData && chartData.result) {
                if (rowIndex !== undefined && rowIndex !== null) {
                    if (chartData.result[rowIndex] && Array.isArray(chartData.result[rowIndex])) {
                        var originalRow = chartData.result[rowIndex];
                        
                        // 원본 데이터의 모든 컬럼을 파라미터로 추가
                        originalRow.forEach(function(value, index) {
                            parameters['param' + (index + 1)] = value;
                        });
                    }
                }
            }
            
            return parameters;
        }
        
        // 캐시에서 차트 데이터 가져오기
        function getChartDataFromCache(canvasId) {
            // 현재 선택된 연결 ID 가져오기
            var selectedConnectionId = getSelectedConnectionId();
            if (!selectedConnectionId) {
                return null;
            }
            
            // cachedApiResponse에서 차트 데이터 조회
            if (cachedApiResponse && cachedApiResponse.connections) {
                var selectedConnection = cachedApiResponse.connections.find(function(conn) {
                    return conn.connectionId === selectedConnectionId;
                });
                
                if (selectedConnection && selectedConnection.charts) {
                    return selectedConnection.charts[canvasId];
                }
            }
            
            return null;
        }
        
        // 선택된 연결 ID 가져오기
        function getSelectedConnectionId() {
            var selectedCard = $('.connection-card.selected');
            if (selectedCard.length > 0) {
                return selectedCard.data('connection-id');
            }
            return null;
        }
        
        // ========================================
        // Toast 메시지 함수
        // ========================================
        function showToast(message, type = 'info', duration = 3000) {
            var toastId = 'toast_' + Date.now();
            var iconClass = {
                'success': 'fa-check-circle',
                'error': 'fa-exclamation-circle',
                'warning': 'fa-exclamation-triangle',
                'info': 'fa-info-circle'
            }[type] || 'fa-info-circle';
            
            var bgClass = {
                'success': 'alert-success',
                'error': 'alert-danger',
                'warning': 'alert-warning',
                'info': 'alert-info'
            }[type] || 'alert-info';
            
            var toast = $('<div id="' + toastId + '" class="alert ' + bgClass + ' alert-dismissible" style="margin-bottom: 10px; animation: slideInRight 0.3s ease-out;">' +
                '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                '<i class="fa ' + iconClass + '"></i> ' + message +
                '</div>');
            
            $('#toastContainer').append(toast);
            
            // 자동 제거
            setTimeout(function() {
                $('#' + toastId).fadeOut(300, function() {
                    $(this).remove();
                });
            }, duration);
        }
    </script>
    
    <!-- Toast 메시지 컨테이너 -->
    <div id="toastContainer" style="position: fixed; top: 20px; right: 20px; z-index: 9999; width: 350px;"></div>
    
    <style>
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    /* 5개 차트일 때 20% 너비 */
    @media (min-width: 992px) {
        .chart-col-5 {
            width: 20% !important;
            flex: 0 0 20% !important;
            max-width: 20% !important;
        }
    }
    </style>
    
