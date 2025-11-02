<%@include file="common/common.jsp"%>
    <link href="/resources/css/dashboard.css" rel="stylesheet" type="text/css" />

    <script>
        // 전역 변수
        var dynamicCharts = {}; // 동적 차트 객체들
        var selectedConnectionId = null; // 선택된 연결 ID
        var chartConfig = null; // 차트 설정
        var chartUpdateTimer = null; // 차트 업데이트 타이머
        var cachedApiResponse = null; // API 응답 데이터 캐시
        var chartConfigData = null; // 차트 설정 데이터
        var isAdmin = ${isAdmin}; // 관리자 권한 여부

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

        // 연결 관리 탭 열기 함수
        function openConnectionTab() {
            parent.tabManager.addTab('connection', '연결 관리', '/Connection');
        }

        $(document).ready(function() {
            // 연결 상태 모니터링 시작
                startConnectionMonitoring();
                
            // 차트 모니터링 시작 (통합 API 사용)
                    startChartMonitoring();
            
            // 메뉴 실행기록 모니터링 시작
            startMenuExecutionLogMonitoring();
            
            // 페이지 언로드 시 정리
            $(window).on('beforeunload', function() {
                if (chartUpdateTimer) {
                    clearInterval(chartUpdateTimer);
                }
            stopConnectionMonitoring();
            stopMenuExecutionLogMonitoring();
            });
        });
        
        // 연결 상태 모니터링 시작
        function startConnectionMonitoring() {
            refreshConnectionStatus();
        }

        // 연결 상태 새로고침 함수
        function refreshConnectionStatus() {
            $.ajax({
                type: 'post',
                url: '/Connection/status',
                success: function(result) {
                    updateConnectionStatusDisplay(result);
                    // 연결 상태 확인 완료 후 다음 타이머 설정
                    scheduleNextRefresh();
                },
                error: function(xhr, status, error) {
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
                }
            });
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
                        $('.connection-card[data-connection-id="' + connectionId + '"]').closest('.col-md-2, .col-sm-3, .col-xs-4').remove();
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
            
            var connectionCard = 
                '<div class="col-md-2 col-sm-3 col-xs-4" style="margin-bottom: 15px;" >' +
                    '<div class="card connection-card ' + statusClass + '" data-connection-id="' + conn.connectionId + '"' + 
                    (conn.status === 'connected' ? ' onclick="selectConnection(\'' + conn.connectionId + '\')"' : '') + '>' +
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
            setTimeout(function() {
                refreshConnectionStatus();
            }, 10000); // 10초마다 연결 상태 확인
        }

        // 연결 모니터링 중지
        function stopConnectionMonitoring() {
            // 타이머 정리 로직 (필요시 구현)
        }
        
        // 차트 모니터링 시작
        function startChartMonitoring() {
            if (chartUpdateTimer) {
                clearInterval(chartUpdateTimer);
            }
            
            // 즉시 한 번 실행
            updateDynamicCharts();
        }
        
        // 동적 차트 업데이트 (통합 API 사용)
        function updateDynamicCharts() {
            $.ajax({
                url: '/Dashboard/getIntegratedData',
                type: 'POST',
                success: function(response) {
                	
                    if (response.success && response.connections) {
                        // API 응답 데이터 캐시에 저장
                        cachedApiResponse = response;
                        
                        // 차트 설정 정보 저장
                        if (response.chartConfig) {
                            chartConfigData = response.chartConfig;
            } else {
                            chartConfigData = null;
                        }
                        
                        // 연결 상태 업데이트
                        updateConnectionStatusDisplay(response.connections);
                        
                        // 모든 연결의 신호등 다시 생성
                        response.connections.forEach(function(conn) {
                            createDynamicTrafficLights(conn.connectionId);
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
                            // 선택된 연결이 없으면 첫 번째 연결을 기본으로 선택
                            var firstConnection = response.connections[0];
                            if (firstConnection && firstConnection.charts) {
                                selectConnection(firstConnection.connectionId);
                            }
                        }
                        
                        // 신호등은 모든 연결 업데이트
                        response.connections.forEach(function(conn) {
                            if (conn.charts) {
                                updateTrafficLightsFromDynamicData(conn.charts, conn.connectionId);
                            }
                        });
                    }
                },
                error: function() {
                    console.error('통합 데이터 조회 실패');
                },
                complete: function() {
                    // API 호출 완료 후 10초 후에 다음 호출 예약
                    setTimeout(function() {
                        updateDynamicCharts();
                    }, 10000);
                }
            });
        }

        // 차트 데이터만 업데이트 (기존 차트 유지)
        function updateChartData(chartId, chartType, data) {

            // chartId가 이미 chart_ 접두사를 가지고 있는지 확인
            var elementId = chartId.startsWith('chart_') ? chartId : 'chart_' + chartId;
            
            if (chartType === 'text') {
                // 텍스트 차트 업데이트
                var container = document.getElementById(elementId);
                if (container) {
                    var chartData = parseChartData(data);
                    var value = chartData.values[0];
                    var color = chartData.colors[0];
                    container.innerHTML = '<div class="text-center" style="padding: 20px;">' +
                        '<div style="margin: 10px 0; font-size: 96px;">' +
                        '<span style="color: ' + color + '; font-weight: bold;">' + value + '</span> ' +
                        '</div>' +
                        '</div>';
                }
            } else {
                // Chart.js 차트 업데이트
                var chartInstance = window.chartInstances && window.chartInstances[elementId];
                if (chartInstance) {
                    var chartData = parseChartData(data);
                    
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
                    var chartId = chart.id;
                    var data = chartsData[chartId];
                    if (data) {
                        sortedCharts.push({
                            chartId: chartId,
                            data: data,
                            chartType: chart.chartType || 'text'
                        });
                    }
                });
            } else {
                // chartConfig가 없으면 기존 방식으로 처리
                for (var chartId in chartsData) {
                    var data = chartsData[chartId];
                    if (data) {
                        sortedCharts.push({
                            chartId: chartId,
                            data: data,
                            chartType: 'text'
                        });
                    }
                }
            }
            
            // 기존 차트와 새 차트 비교하여 업데이트
            var chartIndex = 0;
            var hasValidCharts = false;
            
            sortedCharts.forEach(function(chart) {
                var chartId = chart.chartId;
                var data = chart.data;
                var chartType = chart.chartType;
                
                if (!data) return;
                // 기존 차트 요소 확인
                var existingChart = $('#' + chartId);
                var existingChartType = existingChart.data('chart-type');
                
                // 에러 데이터 처리
                if (data.error) {
                    hasValidCharts = true;
                    
                    // 기존 에러 차트 확인
                    var existingErrorChart = $('#error_' + chartId);
                    
                    if (existingErrorChart.length > 0) {
                        // 기존 에러 차트가 있으면 내용만 업데이트
                        var errorMessage = data.error;
                        existingErrorChart.find('.box-body').html(
                            '<div class="alert alert-danger" style="margin: 0;">' +
                            '<i class="fa fa-exclamation-circle"></i> ' + errorMessage +
                            '</div>'
                        );
                        } else {
                        // 기존 차트가 있으면 제거
                        if (existingChart.length > 0) {
                            existingChart.closest('.col-md-3').remove();
                        }
                        
                        // 새 에러 차트 생성
                        var chartHtml = createErrorChartHtml(chartId, chartIndex, data.error);
                        chartsContainer.append(chartHtml);
                    }
                    chartIndex++;
                    return;
                }
                
                hasValidCharts = true;
                
                // 기존 오류 차트가 있는지 확인하고 제거
                var existingErrorChart = $('#error_' + chartId);
                if (existingErrorChart.length > 0) {
                    existingErrorChart.closest('.col-md-3').remove();
                }
                
                // 기존 차트가 없거나 타입이 다르면 새로 생성
                if (existingChart.length === 0 || existingChartType !== chartType) {
                    // 기존 차트가 있으면 제거
                    if (existingChart.length > 0) {
                        existingChart.closest('.col-md-3').remove();
                    }
                    
                    // 차트 HTML 생성
                    var chartHtml = createChartHtml(chartId, chartType, chartIndex);
                    chartsContainer.append(chartHtml);
                    // 차트 초기화
                    initializeChart(chartId, chartType, data);
                } else {
                    // 기존 차트가 있고 타입이 같으면 값만 업데이트
                    updateChartData(chartId, chartType, data);
                }
                
                chartIndex++;
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
        function getChartTypeFromConfig(chartId) {
            if (!chartConfigData || !chartConfigData.charts) {
                return 'text'; // 기본값
            }
            
            for (var i = 0; i < chartConfigData.charts.length; i++) {
                var chart = chartConfigData.charts[i];
                if (chart.id === chartId) {
                    return chart.chartType || 'text';
                }
            }
            
            return 'text'; // 찾지 못하면 기본값
        }
        
        // 차트 HTML 생성
        function createChartHtml(chartId, chartType, index) {
            var chartTitle = getChartTitle(chartId);
            
            // chartId가 이미 chart_ 접두사를 가지고 있는지 확인
            var elementId = chartId.startsWith('chart_') ? chartId : 'chart_' + chartId;
            
            var contentElement;
            if (chartType === 'text') {
                // 텍스트 차트는 div 요소 사용
                contentElement = '<div id="' + elementId + '" data-chart-type="' + chartType + '"></div>';
                } else {
                // 다른 차트는 canvas 요소 사용
                contentElement = '<canvas id="' + elementId + '" data-chart-type="' + chartType + '"></canvas>';
            }
            
            var html = '<div class="col-md-3">' +
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
        function createErrorChartHtml(chartId, index, errorMessage) {
            var chartTitle = getChartTitle(chartId) + ' (오류)';
            
            var html = '<div class="col-md-3" style="margin-bottom: 20px;">' +
                '<div class="box box-danger" id="error_' + chartId + '">' +
                '<div class="box-header with-border">' +
                '<h3 class="box-title"><i class="fa fa-exclamation-triangle"></i> ' + chartTitle + '</h3>' +
                '</div>' +
                '<div class="box-body">' +
                '<div class="alert alert-danger" style="margin: 0;">' +
                '<i class="fa fa-exclamation-circle"></i> ' + errorMessage +
                '</div>' +
                '</div>' +
                '</div>' +
                '</div>';
            
            return html;
        }
        
        // 차트 초기화
        function initializeChart(chartId, chartType, data) {
            // chartId가 이미 chart_ 접두사를 가지고 있는지 확인
            var canvasId = chartId.startsWith('chart_') ? chartId : 'chart_' + chartId;
            
            // Chart.js 인스턴스를 전역으로 저장
            if (!window.chartInstances) {
                window.chartInstances = {};
            }
            
            if (chartType === 'doughnut') {
                window.chartInstances[canvasId] = initializeDoughnutChart(canvasId, data);
            } else if (chartType === 'text') {
                initializeTextChart(canvasId, data);
            } else if (chartType === 'gauge') {
                window.chartInstances[canvasId] = initializeGaugeChart(canvasId, data);
            } else if (chartType === 'bar') {
                window.chartInstances[canvasId] = initializeBarChart(canvasId, data);
            }
        }
        
        // 도넛 차트 초기화
        function initializeDoughnutChart(canvasId, data) {
            var canvasElement = document.getElementById(canvasId);
            if (!canvasElement) {
                console.error('Canvas element not found: ' + canvasId);
                    return;
            }
            
            var ctx = canvasElement.getContext('2d');
            
            if (dynamicCharts[canvasId]) {
                dynamicCharts[canvasId].destroy();
            }
            
            var chartData = parseChartData(data);
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
                            
                            console.log(getChartDataFromCache(canvasId))
                            
                            
                            handleChartElementClick(canvasId, clickedData);
                        }
                    }
                }
            });
            
            dynamicCharts[canvasId] = chartInstance;
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
            
            if (dynamicCharts[canvasId]) {
                dynamicCharts[canvasId].destroy();
            }
            
            var chartData = parseChartData(data);
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
                               	   ( chart.data.datasets[0].data[0] || 0) + '%',
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
            
            dynamicCharts[canvasId] = chartInstance;
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
            
            if (dynamicCharts[canvasId]) {
                dynamicCharts[canvasId].destroy();
            }
            
            var chartData = parseChartData(data);
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
                        }
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
            
            dynamicCharts[canvasId] = chartInstance;
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
                showToast(validationResult.templateId + "<br>" + validationResult.message, 'error');

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
        
        
        
        // 차트 ID로 차트 제목 가져오기
        function getChartTitle(chartId) {
            if (!chartConfigData || !chartConfigData.charts) {
                return chartId; // 설정이 없으면 차트 ID 반환
            }
            
            for (var i = 0; i < chartConfigData.charts.length; i++) {
                var chart = chartConfigData.charts[i];
                if (chart.id === chartId) {
                    // templateName이 있으면 사용, 없으면 templateId 사용
                    return chart.templateName || chart.templateId || chartId;
                }
            }
            
            return chartId; // 찾지 못하면 차트 ID 반환
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
                    var chartId = chart.id;
                    var data = chartsData[chartId];
                    if (data) {
                        sortedCharts.push({
                            chartId: chartId,
                            data: data
                        });
                    }
                });
            } else {
                // chartConfig가 없으면 기존 방식으로 처리
                for (var chartId in chartsData) {
                    var data = chartsData[chartId];
                    if (data) {
                        sortedCharts.push({
                            chartId: chartId,
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
                            var chartId = chart.id;
                            var chartDataKey = 'chart_' + index;
                            var chartData = dbData[chartDataKey];
                            
                            if (chartData) {
                                sortedCharts.push({
                                    chartId: chartId,
                                    data: chartData
                                });
                            }
                        });
                    } else {
                        // chartConfig가 없으면 기존 방식으로 처리
                        for (var chartId in dbData) {
                            var chartData = dbData[chartId];
                            if (chartData) {
                                sortedCharts.push({
                                    chartId: chartId,
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
        
        // 메뉴 실행기록 모니터링 시작
        function startMenuExecutionLogMonitoring() {
            refreshMenuExecutionLog();
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
        
        // 메뉴 실행기록 새로고침
        function refreshMenuExecutionLog() {
        	
            $.ajax({
                type: 'POST',
                url: '/Dashboard/menuExecutionLog',
                success: function(result) {
                    updateMenuExecutionLogDisplay(result);
                    // 5초 후 다시 조회
                    setTimeout(function() {
                        refreshMenuExecutionLog();
                    }, 5000);
                },
                error: function(xhr, status, error) {
                    console.error('메뉴 실행기록 조회 실패:', error);
                    // 에러 발생 시에도 5초 후 다시 시도
                    setTimeout(function() {
                        refreshMenuExecutionLog();
                    }, 5000);
                }
            });
        }
        
        // 메뉴 실행기록 표시 업데이트
        function updateMenuExecutionLogDisplay(result) {
            var container = $('#menuExecutionLogContainer');
            
            if (!result || !result.data || result.data.length === 0) {
                container.html('<div class="alert alert-info text-center">실행된 메뉴가 없습니다.</div>');
                return;
            }
            
            var html = '<div class="table-responsive">' +
                '<table class="table table-striped table-hover">' +
                '<thead>' +
                '<tr>' +
                '<th>실행시간</th>' +
                '<th>사용자</th>' +
                '<th>템플릿명</th>' +
                '<th>연결ID</th>' +
                '<th>SQL타입</th>' +
                '<th>상태</th>' +
                '<th>실행시간</th>' +
                '<th>영향행수</th>' +
                '</tr>' +
                '</thead>' +
                '<tbody>';
            
            result.data.forEach(function(log) {
                html += '<tr>' +
                    '<td>' + formatDateTime(log.executionStartTime) + '</td>' +
                    '<td>' + (log.userId || '-') + '</td>' +
                    '<td>' + (log.templateName || '-') + '</td>' +
                    '<td>' + (log.connectionId || '-') + '</td>' +
                    '<td>' + (log.sqlType || '-') + '</td>' +
                    '<td><span style="color: ' + (log.statusColor || '#666') + ';">' + (log.executionStatus || '-') + '</span></td>' +
                    '<td>' + (log.durationText || '-') + '</td>' +
                    '<td>' + (log.affectedRows || '-') + '</td>' +
                    '</tr>';
            });
            
            html += '</tbody></table></div>';
            container.html(html);
        }
        
        // 메뉴 실행기록 모니터링 중지
        function stopMenuExecutionLogMonitoring() {
            // 타이머 정리 로직 (필요시 구현)
        }
        
        // 날짜시간 포맷팅
        function formatDateTime(dateTime) {
            if (!dateTime) return '-';
            var date = new Date(dateTime);
            return date.toLocaleString('ko-KR');
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

        <!-- 메뉴 실행기록 섹션 -->
        <div class="row">
                <div class="col-md-12">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                            <i class="fa fa-list"></i> 메뉴 실행기록
                            </h3>
                        </div>
                        <div class="box-body">
                        <div id="menuExecutionLogContainer">
                            <!-- 메뉴 실행기록이 여기에 표시됩니다 -->
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
        .connection-card {
    transition: all 0.3s ease;
}

.connection-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.15);
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
            
            // 차트 설정에서 해당 차트의 템플릿 ID 찾기
            var templateId = null;
            if (chartConfigData && chartConfigData.charts) {
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
            var parameters = convertRowIndexToParameters(canvasId, rowIndex);
            
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
                                
                                columnParts.forEach(function(part) {
                                    var trimmedPart = part.trim();
                                    
                                    // 빈 값은 무시
                                    if (trimmedPart === '') {
                                        return;
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
                                            }
                                        }
                                    }
                                });
                                
                                parameterString = paramValues.join(',');
                            }
                       
                            // SQLExecute.jsp의 sendSql 함수 호출 방식과 동일
                            // common.jsp의 sendSql 함수 사용
                            sendSql(firstActiveShortcut.TARGET_TEMPLATE_ID + "&" + parameterString + "&" + firstActiveShortcut.AUTO_EXECUTE);
                            
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
            console.log(chartData)
            if (chartData && chartData.result && rowIndex !== undefined) {
                if (chartData.result[rowIndex] && Array.isArray(chartData.result[rowIndex])) {
                    var originalRow = chartData.result[rowIndex];
                    // 원본 데이터의 모든 컬럼을 파라미터로 추가
                    originalRow.forEach(function(value, index) {
                        parameters['param' + (index + 1)] = value;
                    });
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
    </style>
    
