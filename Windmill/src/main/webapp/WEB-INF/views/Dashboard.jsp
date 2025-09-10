<%@include file="common/common.jsp"%>
    <script>
        // 연결 상태 모니터링 관련 변수
        var connectionStatusInterval;
        var dexStatusInterval;
        var applCountChart;
        var lockWaitCountChart;
        var activeLogChart;
        var filesystemChart;
        var dexStatusChart;
        var selectedConnectionId = null; // 선택된 커넥션 ID
        var menuExecutionLogInterval = null; // 메뉴 실행 기록 새로고침 인터벌

        // 페이지 로드 시 연결 모니터링 시작
        $(document).ready(function() {
            startConnectionMonitoring();
            startMenuExecutionLogMonitoring();
            initializeCharts();
            // 차트 초기화 완료 후 차트 모니터링 시작
            setTimeout(function() {
                startChartMonitoring();
            }, 100);
        });

        // 페이지 언로드 시 연결 모니터링 중지
        $(window).on('beforeunload', function() {
            
            // 모든 타이머 즉시 정리
            stopConnectionMonitoring();
            stopDexStatusMonitoring();
            stopChartMonitoring();
            
            // 추가 정리 작업
            if (window.chartUpdateTimer) {
                clearTimeout(window.chartUpdateTimer);
                window.chartUpdateTimer = null;
            }
            
            // Chart.js 차트들 정리
            if (typeof Chart !== 'undefined') {
                var charts = Chart.instances;
                if (charts) {
                    for (var i = 0; i < charts.length; i++) {
                        if (charts[i]) {
                            charts[i].destroy();
                        }
                    }
                }
            }
            
            // 전역 변수 정리
            connectionStatusInterval = null;
            dexStatusInterval = null;
            
        });

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
                    '연결된 데이터베이스가 없습니다. <a href="/Connection" class="alert-link">연결 관리</a>에서 데이터베이스 연결을 추가하세요.' +
                    '</div></div>');
                
                // 연결이 없으면 차트 업데이트 중지
                stopChartMonitoring();
                return;
            }
            
            // 초기 로드인지 확인 (컨테이너가 비어있으면 초기 로드)
            var isInitialLoad = container.children().length === 0;
            
            if (isInitialLoad) {
                // 초기 로드: 전체 카드 생성
                connections.forEach(function(conn) {
                    createConnectionCard(conn);
                });
                
                // 첫 번째 커넥션을 기본 선택
                if (connections.length > 0) {
                    selectConnection(connections[0].connectionId);
                }
            } else {
                // 업데이트: 기존 카드의 상태만 변경
                connections.forEach(function(conn) {
                    updateConnectionCard(conn);
                });
            }
        }
        
        // 연결 카드 생성 함수
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
                '<div class="col-md-2 col-sm-3 col-xs-4" style="margin-bottom: 15px;" id="card-' + conn.connectionId + '">' +
                    '<div class="connection-card ' + statusClass + '" onclick="selectConnection(\'' + conn.connectionId + '\')">' +
                        '<div>' +
                            '<i class="fa fa-database"></i>' +
                        '</div>' +
                        '<div class="connection-name">' +
                            conn.connectionId +
                        '</div>' +
                        '<div class="status-text" id="status-' + conn.connectionId + '">' +
                            '<i class="fa ' + statusIcon + '"></i> ' + statusText +
                        '</div>' +
                        '<div class="last-checked" id="lastChecked-' + conn.connectionId + '">' +
                            formattedTime +
                        '</div>' +
                    '</div>' +
                '</div>';
            
            $('#connectionStatusContainer').append(connectionCard);
        }
        
        // 연결 카드 상태 업데이트 함수
        function updateConnectionCard(conn) {
            var card = $('#card-' + conn.connectionId);
            if (card.length === 0) {
                // 카드가 없으면 새로 생성
                createConnectionCard(conn);
                return;
            }
            
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
            
            // 상태 텍스트 업데이트
            $('#status-' + conn.connectionId).html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
            
            // 마지막 확인 시간 업데이트
            $('#lastChecked-' + conn.connectionId).text(formattedTime);
            
            // 카드 클래스 업데이트
            var connectionCard = card.find('.connection-card');
            connectionCard.removeClass('connected disconnected error checking').addClass(statusClass);
            
            // 상태 아이콘 색상 업데이트
            			//$('#status-' + conn.connectionId).css('color', conn.color);
        }

        // 커넥션 선택 함수
        function selectConnection(connectionId) {
            // 이전 선택 해제
            $('.connection-card').removeClass('selected');
            
            // 새로운 선택 적용
            $('#card-' + connectionId + ' .connection-card').addClass('selected');
            
            // 선택된 커넥션 ID 저장
            selectedConnectionId = connectionId;
            
                            logDebug('선택된 커넥션:', connectionId);
            
            // 차트 업데이트 실행
            updateCharts();
        }
        
        // 단일 연결 상태 수동 새로고침
        function refreshSingleConnection(connectionId) {
            $.ajax({
                type: 'post',
                url: '/Connection/status/refresh',
                data: {
                    connectionId: connectionId
                },
                success: function(result) {
                    if (result === 'success') {
                        // 새로고침 후 상태 다시 조회
                        setTimeout(function() {
                            refreshConnectionStatus();
                        }, 500);
                    }
                },
                error: function() {
                    console.error('연결 상태 수동 새로고침 실패');
                }
            });
        }

        // 날짜 시간 포맷팅 함수
        function formatDateTime(dateTimeStr) {
            if (!dateTimeStr) return '';
            
            // 13자리 숫자(밀리초 타임스탬프)인지 확인
            if (typeof dateTimeStr === 'number' || (typeof dateTimeStr === 'string' && /^\d{13}$/.test(dateTimeStr))) {
                // 13자리 타임스탬프를 Date 객체로 변환
                var date = new Date(parseInt(dateTimeStr));
            } else {
                // 일반 날짜 문자열 처리
                var date = new Date(dateTimeStr);
            }
            
            var hours = ('0' + date.getHours()).slice(-2);
            var minutes = ('0' + date.getMinutes()).slice(-2);
            var seconds = ('0' + date.getSeconds()).slice(-2);
            return hours + ':' + minutes + ':' + seconds;
        }

        // 자동 새로고침 시작 함수
        function startConnectionMonitoring() {
            // 초기 로드
            refreshConnectionStatus();
        }

        // 연결 상태 확인 완료 후 다음 타이머 설정
        function scheduleNextRefresh() {
            // 기존 타이머가 있으면 제거
            if (connectionStatusInterval) {
                clearTimeout(connectionStatusInterval);
            }
            
            // 3초 후에 다음 연결 상태 확인 실행
            connectionStatusInterval = setTimeout(function() {
                refreshConnectionStatus();
            }, 3000);
        }

        // 자동 새로고침 중지 함수
        function stopConnectionMonitoring() {
            if (connectionStatusInterval) {
                clearTimeout(connectionStatusInterval);
                connectionStatusInterval = null;
            }
        }

        // 메뉴 실행 기록 새로고침 함수
        function refreshMenuExecutionLog() {
            $.ajax({
                type: 'post',
                url: '/Dashboard/menuExecutionLog',
                timeout: 10000,
                success: function(result) {
                    if (result.success) {
                        updateMenuExecutionLogDisplay(result.data);
                    } else {
                        console.error('메뉴 실행 기록 조회 실패:', result.error);
                    }
                },
                error: function(xhr, status, error) {
                    console.error('메뉴 실행 기록 조회 실패:', error);
                }
            });
        }

        // 메뉴 실행 기록 표시 업데이트 함수
        function updateMenuExecutionLogDisplay(logs) {
            var tbody = $('#menuExecutionLogTableBody');
            tbody.empty();
            
            if (logs && logs.length > 0) {
                logs.forEach(function(log) {
                    var row = '<tr>' +
                        '<td>' + formatDateTime(log.executionStartTime) + '</td>' +
                        '<td>' + log.userId + '</td>' +
                        '<td>' + log.templateId + '</td>' +
                        '<td>' + log.connectionId + '</td>' +
                        '<td>' + log.sqlType + '</td>' +
                        '<td><span style="color: ' + log.statusColor + '; font-weight: bold;">' + log.executionStatus + '</span></td>' +
                        '<td>' + log.durationText + '</td>' +
                        '<td>' + (log.affectedRows || '-') + '</td>' +
                        '</tr>';
                    tbody.append(row);
                });
            } else {
                tbody.append('<tr><td colspan="8" style="text-align: center; color: #999;">실행 기록이 없습니다.</td></tr>');
            }
        }

        // 날짜/시간 포맷팅 함수
        function formatDateTime(dateTimeStr) {
            if (!dateTimeStr) return '-';
            
            var date = new Date(dateTimeStr);
            var now = new Date();
            var diffMs = now - date;
            var diffMins = Math.floor(diffMs / 60000);
            
            if (diffMins < 1) {
                return '방금 전';
            } else if (diffMins < 60) {
                return diffMins + '분 전';
            } else if (diffMins < 1440) {
                return Math.floor(diffMins / 60) + '시간 전';
            } else {
                return date.toLocaleString('ko-KR', {
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit'
                });
            }
        }

        // DEX 상태 표시 업데이트 함수
        function updateDexStatusDisplay(dexStatuses) {
            var container = $('#dexStatusContainer');
            
            if (container.children().length === 0) {
                // 초기 로드: 전체 카드 생성
                dexStatuses.forEach(function(status) {
                    createDexStatusCard(status);
                });
            } else {
                // 업데이트: 기존 카드의 상태만 변경
                dexStatuses.forEach(function(status) {
                    updateDexStatusCard(status);
                });
            }
            
            // CPU/메모리 차트 업데이트 (프로세스 상태에서)
            var processStatus = dexStatuses.find(function(status) {
                return status.statusName === 'dex_process';
            });
            
            if (processStatus) {
                updateDoughnutCharts(processStatus);
            }
        }

        // DEX 상태 카드 생성 함수
        function createDexStatusCard(status) {
            var statusIcon, statusText, statusClass;
            
            if (status.status === 'running' || status.status === 'available') {
                statusIcon = 'fa-check-circle';
                statusText = status.status === 'running' ? '실행중' : '정상';
                statusClass = 'connected';
            } else if (status.status === 'checking') {
                statusIcon = 'fa-spinner fa-spin';
                statusText = '확인중';
                statusClass = 'checking';
            } else if (status.status === 'error') {
                statusIcon = 'fa-exclamation-triangle';
                statusText = '오류';
                statusClass = 'error';
            } else {
                statusIcon = 'fa-times-circle';
                statusText = status.status === 'stopped' ? '중지됨' : '사용불가';
                statusClass = 'disconnected';
            }
            
            var formattedTime = formatDateTime(status.lastChecked);
            
            var dexStatusCard = 
                '<div class="col-md-12" style="margin-bottom: 15px;" id="dex-card-' + status.statusName + '">' +
                    '<div class="connection-card ' + statusClass + '" onclick="refreshSingleDexStatus(\'' + status.statusName + '\')">' +
                        '<div>' +
                            '<i class="fa fa-server"></i>' +
                        '</div>' +
                        '<div class="connection-name">' +
                            status.displayName +
                        '</div>' +
                        '<div class="status-text" id="dex-status-' + status.statusName + '">' +
                            '<i class="fa ' + statusIcon + '"></i> ' + statusText +
                        '</div>' +
                        '<div class="last-checked" id="dex-lastChecked-' + status.statusName + '">' +
                            formattedTime +
                        '</div>' +
                    '</div>' +
                '</div>';
            
            $('#dexStatusContainer').append(dexStatusCard);
            

        }

        // DEX 상태 카드 업데이트 함수
        function updateDexStatusCard(status) {
            var card = $('#dex-card-' + status.statusName);
            if (card.length === 0) {
                createDexStatusCard(status);
                return;
            }
            
            var statusIcon, statusText, statusClass;
            
            if (status.status === 'running' || status.status === 'available') {
                statusIcon = 'fa-check-circle';
                statusText = status.status === 'running' ? '실행중' : '정상';
                statusClass = 'connected';
            } else if (status.status === 'checking') {
                statusIcon = 'fa-spinner fa-spin';
                statusText = '확인중';
                statusClass = 'checking';
            } else if (status.status === 'error') {
                statusIcon = 'fa-exclamation-triangle';
                statusText = '오류';
                statusClass = 'error';
            } else {
                statusIcon = 'fa-times-circle';
                statusText = status.status === 'stopped' ? '중지됨' : '사용불가';
                statusClass = 'disconnected';
            }
            
            var formattedTime = formatDateTime(status.lastChecked);
            
            // 상태 텍스트 업데이트
            $('#dex-status-' + status.statusName).html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
            
            // 마지막 확인 시간 업데이트
            $('#dex-lastChecked-' + status.statusName).text(formattedTime);
            
            // 카드 클래스 업데이트
            var dexStatusCard = card.find('.connection-card');
            dexStatusCard.removeClass('connected disconnected error checking').addClass(statusClass);
        }

        // 단일 DEX 상태 수동 새로고침
        function refreshSingleDexStatus(statusName) {
            $.ajax({
                type: 'post',
                url: '/DexStatus/refresh',
                data: {
                    statusName: statusName
                },
                timeout: 10000,
                success: function(result) {
                    if (result === 'success') {
                        setTimeout(function() {
                            refreshDexStatus();
                        }, 500);
                    }
                },
                error: function(xhr, status, error) {
                    console.error('DEX 상태 새로고침 실패:', error);
                }
            });
        }

        // 메뉴 실행 기록 모니터링 시작
        function startMenuExecutionLogMonitoring() {
            refreshMenuExecutionLog();
            scheduleNextMenuExecutionLogRefresh();
        }
        
        // 메뉴 실행 기록 모니터링 중지
        function stopMenuExecutionLogMonitoring() {
            if (menuExecutionLogInterval) {
                clearTimeout(menuExecutionLogInterval);
                menuExecutionLogInterval = null;
            }
        }
        
        // 다음 메뉴 실행 기록 새로고침 스케줄링
        function scheduleNextMenuExecutionLogRefresh() {
            if (menuExecutionLogInterval) {
                clearTimeout(menuExecutionLogInterval);
            }
            menuExecutionLogInterval = setTimeout(function() {
                refreshMenuExecutionLog();
                scheduleNextMenuExecutionLogRefresh();
            }, 10000); // 10초마다 새로고침
        }

        // 도넛 차트 생성 함수
        function createDoughnutChart(canvasId, value, label, unit) {
            if (typeof Chart === 'undefined') {
                console.error('Chart.js가 로드되지 않았습니다.');
                return;
            }
            
            var canvas = document.getElementById(canvasId);
            if (!canvas) {
                console.error('Canvas 요소를 찾을 수 없습니다: ' + canvasId);
                return;
            }
            
            var ctx = canvas.getContext('2d');
            
            // 기존 차트가 있으면 제거
            var existingChart = Chart.getChart(canvas);
            if (existingChart) {
                existingChart.destroy();
            }
            
            // 값이 숫자가 아니면 0으로 설정
            if (typeof value !== 'number' || isNaN(value)) {
                value = 0;
            }
            
            // 값에 따른 색상 결정
            var color;
            if (value >= 80) {
                color = 'rgb(231, 24, 49)'; // 빨간색 (위험)
            } else if (value >= 60) {
                color = 'rgb(239, 198, 0)'; // 노란색 (경고)
            } else {
                color = 'rgb(140, 214, 16)'; // 초록색 (정상)
            }
            
            try {
                var doughnutChart = new Chart(ctx, {
                    type: 'doughnut',
                    data: {
                        labels: [label, '남은 용량'],
                        datasets: [{
                            data: [value, 100 - value],
                            backgroundColor: [color, 'rgba(255, 255, 255, 0.1)'],
                            borderColor: [color, 'rgba(255, 255, 255, 0.1)'],
                            borderWidth: 2
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '60%',
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                enabled: true,
                                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                                titleColor: 'rgba(255, 255, 255, 0.9)',
                                bodyColor: 'rgba(255, 255, 255, 0.9)',
                                borderColor: color,
                                borderWidth: 1,
                                cornerRadius: 8,
                                displayColors: true,
                                callbacks: {
                                    label: function(context) {
                                        if (context.label === label) {
                                            return context.label + ': ' + context.parsed + '%';
                                        } else {
                                            return context.label + ': ' + context.parsed + '%';
                                        }
                                    }
                                }
                            }
                        },
                        animation: {
                            duration: 1000,
                            easing: 'easeOutQuart'
                        }
                    }
                });
                
                // 차트 객체를 전역 변수에 저장
                window[canvasId + '_chart'] = doughnutChart;
                
            } catch (error) {
                console.error('도넛 차트 생성 중 오류:', error);
            }
        }

        // 도넛 차트 업데이트 함수
        function updateDoughnutChart(canvasId, value) {
            var chart;
            if (canvasId === 'cpu-doughnut') {
                chart = window['cpu-doughnut_chart'];
            } else if (canvasId === 'memory-doughnut') {
                chart = window['memory-doughnut_chart'];
            } else if (canvasId === 'activeLogChart') {
                chart = window['activeLogChart'];
            }
            
            if (chart) {
                // value가 숫자가 아닐 경우 숫자로 변환
                const numericValue = typeof value === 'number' ? value : parseFloat(value) || 0;
                
                // 차트 데이터 업데이트 (새로운 구조에 맞게)
                chart.data.datasets[0].data = [numericValue, 100 - numericValue];
                
                // 부드러운 애니메이션으로 업데이트
                chart.update('active');
            } else {
                console.warn('도넛 차트를 찾을 수 없습니다: ' + canvasId);
            }
        }



        // 도넛 차트 업데이트 함수 (DEX 상태 업데이트 시)
        function updateDoughnutCharts(status) {
            if (status.statusName === 'dex_process') {
                if (status.cpuUsage !== undefined) {
                    updateDoughnutChart('cpu-doughnut', status.cpuUsage);
                    const cpuValue = typeof status.cpuUsage === 'number' ? status.cpuUsage : parseFloat(status.cpuUsage) || 0;
                    $('#cpu-value').text(cpuValue.toFixed(1) + '%');
                }
                if (status.memoryUsage !== undefined) {
                    updateDoughnutChart('memory-doughnut', status.memoryUsage);
                    const memoryValue = typeof status.memoryUsage === 'number' ? status.memoryUsage : parseFloat(status.memoryUsage) || 0;
                    $('#memory-value').text(memoryValue.toFixed(1) + '%');
                }
            }
        }



        // 차트 초기화 함수
        function initializeCharts() {
            // Chart.js datalabels 플러그인 등록
            Chart.register(ChartDataLabels);


            const COLORS = ['rgb(140, 214, 16)', 'rgb(239, 198, 0)', 'rgb(231, 24, 49)'];
            const MIN = 0;
            const MAX = 100;
            
            function index(perc) {
            	  const numericPerc = typeof perc === 'number' ? perc : parseFloat(perc) || 0;
            	  return numericPerc < 70 ? 0 : numericPerc < 90 ? 1 : 2;
            	}
            // APPL_COUNT 차트 (도넛 그래프)
            var applCountCtx = document.getElementById('applCountChart').getContext('2d');
            applCountChart = new Chart(applCountCtx, {
                type: 'doughnut',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'APPL_COUNT',
                        data: [],
                        backgroundColor: [
                            'rgba(140, 214, 16, 0.7)',
                            'rgba(239, 198, 0, 0.7)',
                            'rgba(231, 24, 49, 0.7)',
                            'rgba(158, 158, 158, 0.7)'
                        ],
                        borderColor: [
                            'rgba(140, 214, 16, 1)',
                            'rgba(239, 198, 0, 1)',
                            'rgba(231, 24, 49, 1)',
                            'rgba(158, 158, 158, 1)'
                        ],
                        //borderWidth: 2,
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
                    onClick: function(event, elements) {
                        if (elements.length > 0) {
                            handleChartElementClick('APPL_COUNT', elements[0]);
                        }
                    }
                }
            });

            // LOCK_WAIT_COUNT 차트 초기화 (숫자만 표시)
            lockWaitCountChart = {
                currentValue: 0,
                maxValue: 100,
                previousValue: 0
            };

            // ACTIVE_LOG 차트 (가로 막대그래프 - 사용량)
            var activeLogCtx = document.getElementById('activeLogChart').getContext('2d');
            window['activeLogChart'] = new Chart(activeLogCtx, {
                type: 'doughnut',
                data: {
                    datasets: [{
                        data: [0, 100],
                        backgroundColor(ctx) {
                            if (ctx.type !== 'data') {
                                return;
                            }
                            if (ctx.index === 1) {
                                return 'rgb(234, 234, 234)';
                            }
                            return COLORS[index(ctx.raw)];
                        }
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
                        tooltip: {
                            enabled: false
                        },
                        annotation: {
                            annotations: {
                                cpuLabel: {
                                    type: 'doughnutLabel',
                                    content: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        return [
                                            value.toFixed(1) + '%',
                                            'CPU 사용률'
                                        ];
                                    },
                                    drawTime: 'beforeDraw',
                                    position: {
                                        y: '-50%'
                                    },
                                    font: [{size: 24, weight: 'bold'}, {size: 12}],
                                    color: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        return [COLORS[index(value)], 'grey'];
                                    }
                                }
                            }
                        }
                    },
                    onClick: function(event, elements) {
                        if (elements.length > 0) {
                            handleChartElementClick('ACTIVE_LOG', elements[0]);
                        }
                    }
                }
            });

            // FILESYSTEM 차트 (가로 막대그래프 - 사용량)
            var filesystemCtx = document.getElementById('filesystemChart').getContext('2d');
            filesystemChart = new Chart(filesystemCtx, {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'FILESYSTEM',
                        data: [],
                        backgroundColor: function(context) {
                            var value = context.dataset.data[context.dataIndex];
                            if (value >= 80) {
                                return 'rgba(231, 24, 49, 0.8)'; // 빨간색 (80% 이상)
                            } else if (value >= 60) {
                                return 'rgba(239, 198, 0, 0.8)'; // 노란색 (60-79%)
                            } else {
                                return 'rgba(140, 214, 16, 0.8)'; // 초록색 (60% 미만)
                            }
                        },
                        borderColor: function(context) {
                            var value = context.dataset.data[context.dataIndex];
                            if (value >= 80) {
                                return 'rgba(231, 24, 49, 1)';
                            } else if (value >= 60) {
                                return 'rgba(239, 198, 0, 1)';
                            } else {
                                return 'rgba(140, 214, 16, 1)';
                            }
                        },
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y', // 가로 막대그래프
                    scales: {
                        x: {
                            beginAtZero: true,
                            max: 100,
                            ticks: {
                                callback: function(value) {
                                    return value + '%';
                                }
                            }
                        }
                    },
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    onClick: function(event, elements) {
                        if (elements.length > 0) {
                            handleChartElementClick('FILESYSTEM', elements[0]);
                        }
                    }
                }
            });

            // DEX CPU 사용률 도넛 차트
            var cpuDoughnutCtx = document.getElementById('cpu-doughnut').getContext('2d');
            window['cpu-doughnut_chart'] = new Chart(cpuDoughnutCtx, {
                type: 'doughnut',
                data: {
                    datasets: [{
                        data: [0, 100],
                        backgroundColor(ctx) {
                            if (ctx.type !== 'data') {
                                return;
                            }
                            if (ctx.index === 1) {
                                return 'rgb(234, 234, 234)';
                            }
                            const numericValue = typeof ctx.raw === 'number' ? ctx.raw : parseFloat(ctx.raw) || 0;
                            return COLORS[index(numericValue)];
                        }
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
                        tooltip: {
                            enabled: false
                        },
                        annotation: {
                            annotations: {
                                cpuLabel: {
                                    type: 'doughnutLabel',
                                    content: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        const numericValue = typeof value === 'number' ? value : parseFloat(value) || 0;
                                        return [
                                            numericValue.toFixed(1) + '%',
                                            'CPU 사용률'
                                        ];
                                    },
                                    drawTime: 'beforeDraw',
                                    position: {
                                        y: '-50%'
                                    },
                                    font: [{size: 24, weight: 'bold'}, {size: 12}],
                                    color: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        const numericValue = typeof value === 'number' ? value : parseFloat(value) || 0;
                                        return [COLORS[index(numericValue)], 'grey'];
                                    }
                                }
                            }
                        }
                    }
                }
            });

                        // DEX 메모리 사용률 도넛 차트
            var memoryDoughnutCtx = document.getElementById('memory-doughnut').getContext('2d');
            window['memory-doughnut_chart'] = new Chart(memoryDoughnutCtx, {
                type: 'doughnut',
                data: {
                    datasets: [{
                        data: [0, 100],
                        backgroundColor(ctx) {
                            if (ctx.type !== 'data') {
                                return;
                            }
                            if (ctx.index === 1) {
                                return 'rgb(234, 234, 234)';
                            }
                            const numericValue = typeof ctx.raw === 'number' ? ctx.raw : parseFloat(ctx.raw) || 0;
                            return COLORS[index(numericValue)];
                        }
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
                        tooltip: {
                            enabled: false
                        },
                        annotation: {
                            annotations: {
                                memoryLabel: {
                                    type: 'doughnutLabel',
                                    content: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        const numericValue = typeof value === 'number' ? value : parseFloat(value) || 0;
                                        return [
                                            numericValue.toFixed(1) + '%',
                                            '메모리 사용률'
                                        ];
                                    },
                                    drawTime: 'beforeDraw',
                                    position: {
                                        y: '-50%'
                                    },
                                    font: [{size: 24, weight: 'bold'}, {size: 12}],
                                    color: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        const numericValue = typeof value === 'number' ? value : parseFloat(value) || 0;
                                        return [COLORS[index(numericValue)], 'grey'];
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }

        // LOCK_WAIT_COUNT 클릭 처리 함수
        function handleLockWaitCountClick() {
            logDebug('LOCK_WAIT_COUNT 클릭');
            
            // 저장된 템플릿 ID 사용
            var templateId = chartTemplateIds['LOCK_WAIT_COUNT'];
            if (!templateId) {
                console.error('LOCK_WAIT_COUNT 템플릿 ID를 찾을 수 없습니다');
                showToast('차트 정보를 찾을 수 없습니다. 페이지를 새로고침해주세요.', 'warning');
                return;
            }
            
            // LOCK_WAIT_COUNT는 단일 값이므로 빈 파라미터로 처리
            var parameters = {};
            
            // 단축키 정보 조회
            $.ajax({
                type: 'POST',
                url: '/SQLTemplate/shortcuts',
                data: {
                    templateId: templateId
                },
                success: function(result) {
                    logDebug('LOCK_WAIT_COUNT 단축키 결과:', result);
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
                            // 단축키가 있으면 해당 템플릿으로 이동
                            executeShortcut(firstActiveShortcut, parameters);
                        } else {
                            // 활성화된 단축키가 없으면 토스트 메시지 표시
                            showToast('이 차트에 설정된 단축키가 없습니다. 템플릿 관리에서 단축키를 설정해주세요.', 'info');
                        }
                    } else {
                        // 단축키가 없으면 토스트 메시지 표시
                        showToast('이 차트에 설정된 단축키가 없습니다. 템플릿 관리에서 단축키를 설정해주세요.', 'info');
                    }
                },
                error: function(xhr, status, error) {
                    console.error('단축키 조회 실패:', error);
                    showToast('단축키 정보를 가져오는 중 오류가 발생했습니다.', 'error');
                }
            });
        }

        // 차트 요소 클릭 처리 함수
        function handleChartElementClick(chartType, element) {
            logDebug('차트 요소 클릭:', chartType, element);
            
            // 저장된 템플릿 ID 사용
            var templateId = chartTemplateIds[chartType];
            if (!templateId) {
                console.error('템플릿 ID를 찾을 수 없습니다:', chartType);
                showToast('차트 정보를 찾을 수 없습니다. 페이지를 새로고침해주세요.', 'warning');
                return;
            }
            
            // 차트 데이터를 파라미터로 변환 (저장된 원본 데이터 사용)
            var parameters = convertChartDataToParameters(chartType, element);
            
            // 단축키 정보 조회
            $.ajax({
                type: 'POST',
                url: '/SQLTemplate/shortcuts',
                data: {
                    templateId: templateId
                },
                success: function(result) {
                    logDebug('단축키 결과:', result);
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
                            logDebug('첫 번째 단축키 정보:', firstActiveShortcut, parameters);
                            
                            // 단축키 실행 - 소스 컬럼 인덱스에 따라 파라미터 생성
                            var sourceColumns = firstActiveShortcut.SOURCE_COLUMN_INDEXES;
                            var parameterString = '';
                            
                            if (sourceColumns && sourceColumns.trim() !== '') {
                                var columnIndexes = sourceColumns.split(',').map(function(index, arrayIndex) {
                                    var trimmedIndex = index.trim();
                                    // 빈 값이면 null 반환, 숫자면 0-based index로 변환
                                    return trimmedIndex === '' ? null : parseInt(trimmedIndex) - 1;
                                }).filter(function(index) {
                                    // null이 아닌 값만 필터링
                                    return index !== null;
                                });
                                
                                var paramValues = [];
                                columnIndexes.forEach(function(index) {
                                    if (parameters && Object.keys(parameters).length > 0) {
                                        var paramKeys = Object.keys(parameters);
                                        if (paramKeys[index] !== undefined) {
                                            paramValues.push(parameters[paramKeys[index]]);
                                        }
                                    }
                                });
                                
                                parameterString = paramValues.join(',');
                            }
                            
                            sendSql(firstActiveShortcut.TARGET_TEMPLATE_ID+"&"+parameterString+"&true");
                            
                        } else {
                            logDebug('활성화된 단축키가 없습니다.');
                            showToast('이 차트에 설정된 단축키가 없습니다. 템플릿 관리에서 단축키를 설정해주세요.', 'info');
                        }
                    } else {
                        logDebug('단축키가 없습니다.');
                        showToast('이 차트에 설정된 단축키가 없습니다. 템플릿 관리에서 단축키를 설정해주세요.', 'info');
                    }
                },
                error: function() {
                    logError('단축키 정보 조회 실패');
                    showToast('단축키 정보 조회에 실패했습니다.', 'error');
                }
            });
        }
        
        // 차트 데이터를 파라미터로 변환하는 함수
        function convertChartDataToParameters(chartType, element) {
            var parameters = {};
            
            // element가 유효한지 확인
            if (!element) {
                console.warn('차트 요소가 유효하지 않습니다:', element);
                parameters.status = '정상';
                parameters.count = 0;
                return parameters;
            }
            
            // 차트에서 저장된 원본 데이터 가져오기
            var chartConfig = getChartConfig(chartType);
            if (!chartConfig || !chartConfig.chart || !chartConfig.chart.data || !chartConfig.chart.data.datasets || !chartConfig.chart.data.datasets[0]) {
                console.warn('차트 설정을 찾을 수 없습니다:', chartType);
                parameters.status = '정상';
                parameters.count = 0;
                return parameters;
            }
            
            var dataset = chartConfig.chart.data.datasets[0];
            var dataIndex = element.index;
            
            // 저장된 원본 데이터 확인
            if (dataset._dataRows && dataset._dataRows[dataIndex]) {
                var originalData = dataset._dataRows[dataIndex];
                logDebug('원본 데이터:', originalData);
                
                // 원본 데이터를 파라미터로 변환
                if (Array.isArray(originalData) && originalData.length >= 2) {
                    parameters.status = originalData[0] || '정상';  // 첫 번째 컬럼 (상태)
                    parameters.count = originalData[1] || 0;        // 두 번째 컬럼 (개수)
                } else {
                    parameters.status = '정상';
                    parameters.count = 0;
                }
            } else {
                console.warn('저장된 원본 데이터를 찾을 수 없습니다:', dataIndex);
                parameters.status = '정상';
                parameters.count = 0;
            }
            
            return parameters;
        }
        
        function executeShortcut(shortcut, parameters) {
            // 부모 창의 탭 시스템 사용
            var parentWindow = window.parent || window;
            
            var target = $(parentWindow.document).find('#pageTabContent>div:last>iframe').attr('id');
            
            for (var i = 0; i < $(parentWindow.document).find('#pageTab a').length; i++) {
                if (shortcut.TARGET_TEMPLATE_ID == $(parentWindow.document).find('#pageTab a:eq(' + i + ')').text().replace(/x$/, '')) {
                    target = $(parentWindow.document).find('#pageTabContent>div:eq(' + i + ')>iframe').attr('id');
                    break;
                }
            }
            
            // 동적 링크 생성 방식으로 변경 (templateId 사용)
            var autoExecute = shortcut.AUTO_EXECUTE ? 'true' : 'false';
            
            // 파라미터를 URL 파라미터로 변환
            var urlParams = new URLSearchParams();
            urlParams.append('templateId', shortcut.TARGET_TEMPLATE_ID);
            urlParams.append('excute', autoExecute);
            
            // 파라미터 추가
            if (parameters) {
                Object.keys(parameters).forEach(function(key) {
                    urlParams.append(key, parameters[key]);
                });
            }
            
            var url = '/SQL?' + urlParams.toString();
            
            logDebug('단축키 실행 URL:', url);
            
            // 동적 링크 생성 및 클릭
            var link = document.createElement('a');
            link.href = url;
            link.target = target;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        // 차트 데이터 해시 저장 변수
        var chartHashes = {
            'APPL_COUNT': null,
            'LOCK_WAIT_COUNT': null,
            'ACTIVE_LOG': null,
            'FILESYSTEM': null
        };
        
        // 차트 템플릿 ID 저장 변수
        var chartTemplateIds = {
            'APPL_COUNT': null,
            'LOCK_WAIT_COUNT': null,
            'ACTIVE_LOG': null,
            'FILESYSTEM': null
        };

        // 차트 오류 상태 관리 변수
        var chartErrorStates = {
            'APPL_COUNT': false,
            'LOCK_WAIT_COUNT': false,
            'ACTIVE_LOG': false,
            'FILESYSTEM': false
        };

        // 차트 업데이트 비활성화 상태 관리 변수
        var chartUpdateDisabled = {
            'APPL_COUNT': false,
            'LOCK_WAIT_COUNT': false,
            'ACTIVE_LOG': false,
            'FILESYSTEM': false
        };
        
        // 차트별 새로고침 간격 (밀리초) - 템플릿에서 동적으로 설정
        var chartRefreshIntervals = {};
        
        // 차트별 타이머 ID 저장
        var chartTimers = {
            'APPL_COUNT': null,
            'LOCK_WAIT_COUNT': null,
            'ACTIVE_LOG': null,
            'FILESYSTEM': null
        };

        // 차트 데이터 업데이트 함수 (해시 비교 방식)
        function updateCharts() {
            
            // 선택된 커넥션이 없으면 첫 번째 커넥션 사용
            if (!selectedConnectionId) {
                var firstCard = $('.connection-card').first();
                if (firstCard.length > 0) {
                    var firstConnectionId = firstCard.closest('[id^="card-"]').attr('id').replace('card-', '');
                    selectedConnectionId = firstConnectionId;
                } else {
                    // 연결된 커넥션이 없는 경우 오류 처리
                    console.warn('선택된 연결이 없습니다. 차트 업데이트를 중단합니다.');
                    // 연결이 없으면 차트 업데이트 중지하고 다음 업데이트 스케줄링하지 않음
                    stopChartMonitoring();
                    return;
                }
            }
            
            // 활성화된 차트만 필터링
            var activeCharts = [];
            
            if (!chartUpdateDisabled['APPL_COUNT']) {
                activeCharts.push(new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/APPL_COUNT',
                        data: {
                            lastHash: chartHashes['APPL_COUNT'],
                            connectionId: selectedConnectionId
                        },
                        timeout: 10000,
                        success: function(data) {
                            resolve({type: 'APPL_COUNT', data: data});
                        },
                        error: function(xhr, status, error) {
                            console.error('APPL_COUNT 데이터 조회 실패:', error);
                            resolve({type: 'APPL_COUNT', data: {error: 'APPL_COUNT 조회 실패: ' + error}});
                        }
                    });
                }));
            }
            
            if (!chartUpdateDisabled['LOCK_WAIT_COUNT']) {
                activeCharts.push(new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/LOCK_WAIT_COUNT',
                        data: {
                            lastHash: chartHashes['LOCK_WAIT_COUNT'],
                            connectionId: selectedConnectionId
                        },
                        timeout: 10000,
                        success: function(data) {
                            resolve({type: 'LOCK_WAIT_COUNT', data: data});
                        },
                        error: function(xhr, status, error) {
                            console.error('LOCK_WAIT_COUNT 데이터 조회 실패:', error);
                            resolve({type: 'LOCK_WAIT_COUNT', data: {error: 'LOCK_WAIT_COUNT 조회 실패: ' + error}});
                        }
                    });
                }));
            }
            
            if (!chartUpdateDisabled['ACTIVE_LOG']) {
                activeCharts.push(new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/ACTIVE_LOG',
                        data: {
                            lastHash: chartHashes['ACTIVE_LOG'],
                            connectionId: selectedConnectionId
                        },
                        timeout: 10000,
                        success: function(data) {
                            resolve({type: 'ACTIVE_LOG', data: data});
                        },
                        error: function(xhr, status, error) {
                            console.error('ACTIVE_LOG 데이터 조회 실패:', error);
                            resolve({type: 'ACTIVE_LOG', data: {error: 'ACTIVE_LOG 조회 실패: ' + error}});
                        }
                    });
                }));
            }
            
            if (!chartUpdateDisabled['FILESYSTEM']) {
                activeCharts.push(new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/FILESYSTEM',
                        data: {
                            lastHash: chartHashes['FILESYSTEM'],
                            connectionId: selectedConnectionId
                        },
                        timeout: 10000,
                        success: function(data) {
                            resolve({type: 'FILESYSTEM', data: data});
                        },
                        error: function(xhr, status, error) {
                            console.error('FILESYSTEM 데이터 조회 실패:', error);
                            resolve({type: 'FILESYSTEM', data: {error: 'FILESYSTEM 조회 실패: ' + error}});
                        }
                    });
                }));
            }
            
            // 활성화된 차트가 없으면 업데이트 중단
            if (activeCharts.length === 0) {
                console.warn('모든 차트가 비활성화되어 업데이트를 중단합니다.');
                stopChartMonitoring();
                return;
            }
            
            // 활성화된 차트만 병렬로 요청
            Promise.all(activeCharts).then(function(results) {
                // 각 결과를 차트 타입별로 처리
                results.forEach(function(result) {
                	
                    var chartType = result.type;
                    var data = result.data;
                    
                    // 변경 감지 확인
                    if (data && data.changed === false) {
                        // 해시 업데이트
                        if (data.hash) {
                            chartHashes[chartType] = data.hash;
                        }
                        // 템플릿 ID 저장
                        if (data.templateId) {
                            chartTemplateIds[chartType] = data.templateId;
                        }
                        return; // 이 차트는 업데이트하지 않음
                    }
                    
                    // 데이터가 변경되었거나 에러인 경우
                    var chartConfig = getChartConfig(chartType);
                    if (!chartConfig) {
                        console.error('차트 설정을 찾을 수 없어 업데이트를 건너뜁니다:', chartType);
                        return;
                    }
                    
                    try {
                        // 에러가 있는 경우 처리
                        if (data && data.error) {
                            
                            // 에러 상태 설정
                            chartErrorStates[chartType] = true;
                            chartUpdateDisabled[chartType] = true;
                            
                            // 에러 타입에 따른 처리
                            if (data.errorType === 'MONITORING_DISABLED') {
                                // 모니터링 비활성화 상태 표시
                                chartConfig.chart.data.labels = ['모니터링 비활성화'];
                                chartConfig.chart.data.datasets[0].data = [1];
                                chartConfig.chart.data.datasets[0].backgroundColor = ['#ffc107']; // 노란색
                                chartConfig.chart.update();
                                
                                // 모니터링 비활성화 메시지는 한 번만 표시
                                if (!window.monitoringDisabledShown) {
                                    showMonitoringDisabledMessage();
                                    window.monitoringDisabledShown = true;
                                }
                                return;
                            } else {
                                // 오류 메시지 표시 및 새로고침 버튼 추가
                                showChartErrorWithRefresh(chartType, data.error);
                                return;
                            }
                        }
                        
                        // 정상 데이터 처리
                        if (data && data.result && Array.isArray(data.result)) {
                            // 에러 상태 초기화
                            chartErrorStates[chartType] = false;
                            chartUpdateDisabled[chartType] = false;
                            
                            var processedData = chartConfig.processData(data.result);
                            
                            // 데이터 유효성 검사
                            if (processedData) {
                                if (chartType === 'LOCK_WAIT_COUNT') {
                                    // LOCK_WAIT_COUNT는 게이지 차트로 처리
                                    updateLockWaitCountDisplay(processedData);
                                } else if (processedData.labels && processedData.data) {
                                    chartConfig.chart.data.labels = processedData.labels;
                                    chartConfig.chart.data.datasets[0].data = processedData.data;
                                    
                                    // LOCK_WAIT_COUNT 차트의 경우 Y축 최대값 설정
                                    if (processedData.yAxisMax) {
                                        chartConfig.chart.options.scales.y.max = processedData.yAxisMax;
                                    }
                                    
                                    if(chartType=="ACTIVE_LOG"){
                                        updateDoughnutChart('activeLogChart', processedData.data[0]); 
                                    }
                                    
                                    chartConfig.chart.update();
                                }
                                
                                // 해시 업데이트
                                if (data.hash) {
                                    chartHashes[chartType] = data.hash;
                                }
                                
                                // 템플릿 ID 저장
                                if (data.templateId) {
                                    chartTemplateIds[chartType] = data.templateId;
                                }
                                
                                // 원본 SQL 결과 데이터를 차트 요소에 저장
                                if (data.result && Array.isArray(data.result)) {
                                    // 각 차트 요소에 해당하는 SQL 결과 행을 저장
                                    for (var i = 0; i < chartConfig.chart.data.datasets[0].data.length; i++) {
                                        if (data.result[i]) {
                                            // 차트 요소에 원본 데이터 저장
                                            chartConfig.chart.data.datasets[0]._dataRows = chartConfig.chart.data.datasets[0]._dataRows || [];
                                            chartConfig.chart.data.datasets[0]._dataRows[i] = data.result[i];
                                        }
                                    }
                                }
                            } else {
                                console.warn('차트 데이터 형식 오류:', chartType, processedData);
                                // 기본 데이터로 설정
                                chartConfig.chart.data.labels = ['데이터 없음'];
                                chartConfig.chart.data.datasets[0].data = [1];
                                chartConfig.chart.update();
                            }
                        } else {
                            console.warn('차트 데이터가 없습니다:', chartType, data);
                            // 기본 데이터로 설정
                            chartConfig.chart.data.labels = ['데이터 없음'];
                            chartConfig.chart.data.datasets[0].data = [1];
                            chartConfig.chart.update();
                        }
                    } catch (error) {
                        console.error('차트 업데이트 중 오류 발생:', chartType, error);
                        // 에러 발생 시 기본 데이터로 설정
                        try {
                            chartConfig.chart.data.labels = ['오류'];
                            chartConfig.chart.data.datasets[0].data = [1];
                            chartConfig.chart.update();
                        } catch (chartError) {
                            console.error('차트 복구 중 오류:', chartError);
                        }
                    }
                });

                
                // 다음 업데이트 스케줄링
                scheduleNextChartUpdate();
            }).catch(function(error) {
                console.error('차트 데이터 조회 중 전체 오류 발생:', error);
                // 전체 실패 시에도 다음 업데이트 스케줄링
                scheduleNextChartUpdate();
            });
        }

        // 차트 ID를 반환하는 함수
        function getChartId(chartType) {
            var chartIds = {
                'APPL_COUNT': 'applCountChart',
                'LOCK_WAIT_COUNT': 'lockWaitCountChart',
                'ACTIVE_LOG': 'activeLogChart',
                'FILESYSTEM': 'filesystemChart'
            };
            return chartIds[chartType] || chartType.toLowerCase().replace(/_/g, '') + 'Chart';
        }

        // 차트 설정을 반환하는 함수
        function getChartConfig(chartType) {
            var chartConfigs = {
                'APPL_COUNT': {
                    chart: applCountChart,
                    processData: function(result) {
                        var labels = [];
                        var data = [];
                        for (var i = 0; i < result.length; i++) {
                            if (result[i] && result[i].length >= 2) {
                                labels.push(result[i][0]); // color
                                data.push(result[i][1]);  // value
                            }
                        }
                        return { labels: labels, data: data };
                    }
                },
                'LOCK_WAIT_COUNT': {
                    chart: lockWaitCountChart,
                    processData: function(result) {
                        var totalValue = 0;
                        var maxValue = 100; // 기본 최대값
                        
                        // 모든 값의 합계 계산
                        for (var i = 0; i < result.length; i++) {
                            if (result[i] && result[i].length >= 1) {
                                // 단일 값인 경우 (예: [[25]])
                                if (result[i].length === 1) {
                                    totalValue += parseInt(result[i][0]) || 0;
                                }
                                // 두 개 이상의 값인 경우 (예: [["label", 25]])
                                else if (result[i].length >= 2) {
                                    totalValue += parseInt(result[i][1]) || 0;
                                }
                            }
                        }
                        
                        // 최대값 동적 설정 (현재 값의 2배 또는 100 중 큰 값)
                        maxValue = Math.max(totalValue * 2, 100);
                        
                        return { 
                            totalValue: totalValue, 
                            maxValue: maxValue,
                            percentage: Math.min(totalValue / maxValue, 1)
                        };
                    }
                },
                'ACTIVE_LOG': {
                    chart: activeLogChart,
                    processData: function(result) {
                        var labels = [];
                        var data = [];
                        for (var i = 0; i < result.length; i++) {
                            if (result[i] && result[i].length >= 2) {
                                labels.push(result[i][0]); // color
                                data.push(result[i][1]);  // value
                            }
                        }
                        return { labels: labels, data: data };
                    }
                },
                'FILESYSTEM': {
                    chart: filesystemChart,
                    processData: function(result) {
                        var labels = [];
                        var data = [];
                        for (var i = 0; i < result.length; i++) {
                            if (result[i] && result[i].length >= 2) {
                                labels.push(result[i][0]); // name
                                data.push(result[i][1]);  // value (0-100%)
                            }
                        }
                        return { labels: labels, data: data };
                    }
                }
            };
            
            var config = chartConfigs[chartType];
            if (!config) {
                console.error('차트 설정을 찾을 수 없습니다:', chartType, '사용 가능한 차트:', Object.keys(chartConfigs));
                return null;
            }
            
            // 차트 객체가 초기화되었는지 확인
            if (!config.chart) {
                console.error('차트 객체가 초기화되지 않았습니다:', chartType);
                return null;
            }
            
            return config;
        }

        // 다음 차트 업데이트 스케줄링
        function scheduleNextChartUpdate() {
            // 기존 타이머가 있으면 제거
            if (window.chartUpdateTimer) {
                clearTimeout(window.chartUpdateTimer);
            }
            
            // 에러가 발생한 차트가 있는지 확인
            var hasErrorCharts = Object.values(chartErrorStates).some(function(hasError) {
                return hasError;
            });
            
            // 모든 차트가 에러 상태이면 자동 업데이트 중단
            if (hasErrorCharts) {
                logError('차트 에러 발생으로 자동 업데이트 중단됨');
                return;
            }
            
            // 10초 후에 다시 실행
            window.chartUpdateTimer = setTimeout(function() {
                updateCharts();
            }, 10000);
        }

        // 차트 모니터링 중지
        function stopChartMonitoring() {
            if (window.chartUpdateTimer) {
                clearTimeout(window.chartUpdateTimer);
                window.chartUpdateTimer = null;
            }
        }

        // LOCK_WAIT_COUNT 표시 업데이트 함수
        function updateLockWaitCountDisplay(data) {
            var totalValue = data.totalValue || 0;
            var maxValue = data.maxValue || 100;
            var percentage = data.percentage || 0;
            
            // 큰 숫자 업데이트
            $('#lockWaitCountValue').text(totalValue);
            
            // 색상 업데이트 (값에 따라)
            var valueColor;
            if (percentage < 0.3) {
                valueColor = '#28a745'; // 녹색 (정상)
                $('#lockWaitCountStatusText').text('정상').css('color', '#28a745');
            } else if (percentage < 0.7) {
                valueColor = '#ffc107'; // 노란색 (주의)
                $('#lockWaitCountStatusText').text('주의').css('color', '#ffc107');
            } else {
                valueColor = '#dc3545'; // 빨간색 (위험)
                $('#lockWaitCountStatusText').text('위험').css('color', '#dc3545');
            }
            
            $('#lockWaitCountValue').css('color', valueColor);
        }

        // 차트 모니터링 시작 함수
        function startChartMonitoring() {
            // 초기 데이터 로드
            updateCharts();
            
            // 차트별 개별 타이머 설정 (템플릿에서 간격 읽어와서)
            scheduleChartUpdates();
        }
        
        // 차트별 개별 타이머 설정 함수
        function scheduleChartUpdates() {
            var chartTypes = ['APPL_COUNT', 'LOCK_WAIT_COUNT', 'ACTIVE_LOG', 'FILESYSTEM'];
            
            chartTypes.forEach(function(chartType) {
                if (!chartUpdateDisabled[chartType]) {
                    // 먼저 템플릿 정보를 가져와서 새로고침 간격을 설정
                    getTemplateInfoAndSchedule(chartType);
                }
            });
        }
        
        // 템플릿 정보를 가져와서 새로고침 간격 설정 후 스케줄링
        function getTemplateInfoAndSchedule(chartType) {
            $.ajax({
                type: 'POST',
                url: '/Dashboard/' + chartType,
                data: { checkTemplate: true },
                success: function(result) {
                    if (result.success && result.template && result.template.REFRESH_INTERVAL) {
                        var interval = parseInt(result.template.REFRESH_INTERVAL);
                        // 0이면 10초로 설정
                        if (interval <= 0) {
                            interval = 10;
                        }
                        // 템플릿에서 새로고침 간격 설정
                        chartRefreshIntervals[chartType] = interval * 1000;
                    } else {
                        // 기본값 설정
                        chartRefreshIntervals[chartType] = 10000; // 10초
                    }
                    // 스케줄링 시작
                    scheduleSingleChartUpdate(chartType);
                },
                error: function() {
                    // 기본값 설정
                    chartRefreshIntervals[chartType] = 10000; // 10초
                    // 스케줄링 시작
                    scheduleSingleChartUpdate(chartType);
                }
            });
        }
        
        // 단일 차트 업데이트 스케줄링
        function scheduleSingleChartUpdate(chartType) {
            // 기존 타이머 제거
            if (chartTimers[chartType]) {
                clearTimeout(chartTimers[chartType]);
            }
            
            // 새로고침 간격이 설정되지 않은 경우 기본값 사용
            var interval = chartRefreshIntervals[chartType] || 10000; // 기본 10초
            
            // 다음 업데이트 스케줄링
            chartTimers[chartType] = setTimeout(function() {
                if (!chartUpdateDisabled[chartType]) {
                    updateSingleChart(chartType);
                }
                // 재귀적으로 다음 업데이트 스케줄링
                scheduleSingleChartUpdate(chartType);
            }, interval);
        }
        
        // 단일 차트 업데이트 함수
        function updateSingleChart(chartType) {
            if (!selectedConnectionId) {
                return;
            }
            
            $.ajax({
                type: 'post',
                url: '/Dashboard/' + chartType,
                data: {
                    lastHash: chartHashes[chartType],
                    connectionId: selectedConnectionId
                },
                timeout: 10000,
                success: function(data) {
                    processChartData(chartType, data);
                },
                error: function(xhr, status, error) {
                    console.error(chartType + ' 데이터 조회 실패:', error);
                    processChartData(chartType, {error: chartType + ' 조회 실패: ' + error});
                }
            });
        }
        
        // 차트 데이터 처리 함수
        function processChartData(chartType, data) {
            var chartConfig = getChartConfig(chartType);
            if (!chartConfig) {
                return;
            }
            
            try {
                // 에러가 있는 경우 처리
                if (data && data.error) {
                    // 에러 타입에 따른 처리
                    if (data.errorType === 'CHART_NOT_FOUND') {
                        showChartTemplateWarning(chartType);
                        chartUpdateDisabled[chartType] = true;
                        return;
                    } else if (data.errorType === 'MONITORING_DISABLED') {
                        chartConfig.chart.data.labels = ['모니터링 비활성화'];
                        chartConfig.chart.data.datasets[0].data = [1];
                        chartConfig.chart.data.datasets[0].backgroundColor = ['#ffc107'];
                        chartConfig.chart.update();
                        return;
                    } else {
                        showChartErrorWithRefresh(chartType, data.error + ' (오류 발생으로 일시정지)');
                        chartUpdateDisabled[chartType] = true;
                        return;
                    }
                }
                
                // 정상 데이터 처리
                if (data && data.result && Array.isArray(data.result)) {
                    // 에러 상태 초기화
                    chartErrorStates[chartType] = false;
                    chartUpdateDisabled[chartType] = false;
                    
                    // 템플릿 정보에서 새로고침 간격 설정
                    if (data.template && data.template.REFRESH_INTERVAL) {
                        var interval = parseInt(data.template.REFRESH_INTERVAL);
                        // 0이면 10초로 설정
                        if (interval <= 0) {
                            interval = 10;
                        }
                        chartRefreshIntervals[chartType] = interval * 1000;
                    }
                    
                    var processedData = chartConfig.processData(data.result);
                    
                    if (processedData) {
                        chartConfig.chart.data.labels = processedData.labels;
                        chartConfig.chart.data.datasets[0].data = processedData.data;
                        chartConfig.chart.update();
                        
                        // 해시 업데이트
                        if (data.hash) {
                            chartHashes[chartType] = data.hash;
                        }
                    }
                }
            } catch (error) {
                console.error('차트 데이터 처리 중 오류:', error);
                showChartErrorWithRefresh(chartType, '데이터 처리 오류: ' + error.message);
                chartUpdateDisabled[chartType] = true;
            }
        }
        
        // 차트 템플릿 경고 표시 함수
        function showChartTemplateWarning(chartType) {
            var chartId = getChartId(chartType);
            var chartContainer = $('#' + chartId).closest('.box-body');
            
            if (chartContainer.length > 0) {
                // 기존 경고 메시지 제거
                chartContainer.find('.chart-template-warning').remove();
                
                // 새로운 경고 메시지 추가
                var warningHtml = '<div class="chart-template-warning" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; z-index: 10;">' +
                    '<div class="alert alert-warning" style="margin: 0; padding: 10px;">' +
                    '<i class="fa fa-exclamation-triangle"></i> ' + getChartDisplayName(chartType) + ' 템플릿이 설정되지 않았습니다.<br>' +
                    '<small>템플릿 관리에서 해당 차트의 템플릿을 설정해주세요.</small>' +
                    '</div></div>';
                
                chartContainer.css('position', 'relative').append(warningHtml);
            }
        }
        
        // 차트 표시명 반환 함수
        function getChartDisplayName(chartType) {
            var displayNames = {
                'APPL_COUNT': '애플리케이션 수',
                'LOCK_WAIT_COUNT': '락 대기 수',
                'TABLESPACE_USAGE': '테이블스페이스 사용률',
                'ACTIVE_SESSION_COUNT': '활성 세션 수',
                'ACTIVE_LOG': '활성 로그',
                'FILESYSTEM': '파일시스템'
            };
            return displayNames[chartType] || chartType;
        }

        // 차트 오류 표시 및 새로고침 버튼 추가 함수
        function showChartErrorWithRefresh(chartType, errorMessage) {
            
            // 차트 컨테이너에 오류 메시지와 새로고침 버튼 추가
            var chartId = getChartId(chartType);
            var chartContainer = $('#' + chartId).closest('.box-body');
            
            if (chartContainer.length > 0) {
                // 기존 오류 메시지 제거
                chartContainer.find('.chart-error-message').remove();
                
                // 새로운 오류 메시지 추가
                var errorHtml = '<div class="chart-error-message" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; z-index: 10;">' +
                    '<div class="alert alert-danger" style="margin: 0; padding: 10px;">' +
                    '<button type="button" class="btn btn-sm btn-danger" onclick="refreshSingleChart(\'' + chartType + '\')" style="margin-top: 5px;">' +
                    '<i class="fa fa-refresh"></i> 다시 시도</button>' +
                    '</div></div>';
                
                chartContainer.css('position', 'relative').append(errorHtml);
            }
        }

        // 단일 차트 새로고침 함수
        function refreshSingleChart(chartType) {
            // 해당 차트의 오류 상태 초기화
            chartErrorStates[chartType] = false;
            chartUpdateDisabled[chartType] = false;
            
            // 해시 초기화하여 강제 새로고침
            chartHashes[chartType] = null;
            
            // 오류 메시지 제거
            var chartId = getChartId(chartType);
            var chartContainer = $('#' + chartId).closest('.box-body');
            chartContainer.find('.chart-error-message').remove();
            
            // 해당 차트만 업데이트
            updateSingleChart(chartType);
        }

        // 단일 차트 업데이트 함수
        function updateSingleChart(chartType) {
            if (!selectedConnectionId) {
                console.warn('선택된 연결이 없습니다.');
                return;
            }
            
            $.ajax({
                type: 'post',
                url: '/Dashboard/' + chartType,
                data: {
                    lastHash: chartHashes[chartType],
                    connectionId: selectedConnectionId
                },
                timeout: 10000,
                success: function(data) {
                    var chartConfig = getChartConfig(chartType);
                    if (!chartConfig) return;
                    
                    if (data && data.error) {
                        showChartErrorWithRefresh(chartType, data.error);
                        return;
                    }
                    
                    if (data && data.result && Array.isArray(data.result)) {
                        chartErrorStates[chartType] = false;
                        chartUpdateDisabled[chartType] = false;
                        
                        // 에러 메시지 제거
                        var chartId = getChartId(chartType);
                        var chartContainer = $('#' + chartId).closest('.box-body');
                        chartContainer.find('.chart-error-message').remove();
                        
                        var processedData = chartConfig.processData(data.result);
                        if (processedData && processedData.labels && processedData.data) {
                            chartConfig.chart.data.labels = processedData.labels;
                            chartConfig.chart.data.datasets[0].data = processedData.data;
                            
                            if (processedData.yAxisMax) {
                                chartConfig.chart.options.scales.y.max = processedData.yAxisMax;
                            }
                            
                            chartConfig.chart.update();
                            
                            if (data.hash) {
                                chartHashes[chartType] = data.hash;
                            }
                        }
                    }
                },
                error: function(xhr, status, error) {
                    showChartErrorWithRefresh(chartType, '데이터 조회 실패: ' + error);
                }
            });
        }
    </script>
<head>
    <meta charset="UTF-8">
    <title>대시보드 - DeX</title>
    <%@include file="common/common.jsp"%>
    <link href="/resources/css/dashboard.css" rel="stylesheet" type="text/css" />
</head>

<body class="sidebar-mini skin-purple-light">
    <div class="wrapper">
        <div class="content-wrapper" style="margin-left: 0; padding: 20px;">
            <div class="row">
                <div class="col-md-12">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-database"></i> 데이터베이스 연결 상태 모니터링
                            </h3>
                            <div class="box-tools pull-right">
                                <button type="button" class="btn btn-box-tool" onclick="location.reload()">
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

            <!-- APPL_COUNT 차트 -->
            <div class="row">
                <div class="col-md-3">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-bar-chart"></i> APPL_COUNT
                            </h3>
                        </div>
                        <div class="box-body" style="height: 250px; display: flex; align-items: center; justify-content: center;">
                            <canvas id="applCountChart" style="max-height: 100%; max-width: 100%;"></canvas>
                        </div>
                    </div>
                </div>

                <!-- LOCK_WAIT_COUNT 대시보드 -->
                <div class="col-md-3">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-lock"></i> LOCK_WAIT_COUNT
                            </h3>
                        </div>
                        <div class="box-body lock-wait-count-container" style="height: 250px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; cursor: pointer; transition: all 0.3s ease;" onclick="handleLockWaitCountClick()">
                            <!-- 큰 숫자 표시 -->
                            <div id="lockWaitCountValue" style="font-size: 72px; font-weight: bold; color: #28a745; margin-bottom: 20px; text-align: center;">
                                0
                            </div>
                            <!-- 상태 표시 -->
                            <div id="lockWaitCountStatus" style="font-size: 14px; color: #666; text-align: center;">
                                <span id="lockWaitCountStatusText">정상</span>
                                <span id="lockWaitCountTrend" style="margin-left: 5px;"></span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- ACTIVE_LOG 차트 -->
                <div class="col-md-3">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-bar-chart"></i> ACTIVE_LOG
                            </h3>
                        </div>
                        <div class="box-body" style="height: 250px; display: flex; align-items: center; justify-content: center;">
                            <canvas id="activeLogChart" style="max-height: 200px; max-width: 100%;"></canvas>
                        </div>
                    </div>
                </div>

                <!-- FILESYSTEM 차트 -->
                <div class="col-md-3">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-bar-chart"></i> FILESYSTEM
                            </h3>
                        </div>
                        <div class="box-body" style="height: 250px; display: flex; align-items: center; justify-content: center;">
                            <canvas id="filesystemChart" style="max-height: 200px; max-width: 100%;"></canvas>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 메뉴 실행 기록 -->
            <div class="row">
                <div class="col-md-12">
                    <div class="box box-default">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-list"></i> 최근 메뉴 실행 기록
                            </h3>
                            <div class="box-tools pull-right">
                                <button type="button" class="btn btn-box-tool" onclick="refreshMenuExecutionLog()">
                                    <i class="fa fa-refresh"></i> 새로고침
                                </button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div class="table-responsive">
                                <table class="table table-striped table-hover" id="menuExecutionLogTable">
                                    <thead>
                                        <tr>
                                            <th>실행 시간</th>
                                            <th>사용자</th>
                                            <th>템플릿</th>
                                            <th>DB 연결</th>
                                            <th>SQL 타입</th>
                                            <th>상태</th>
                                            <th>실행 시간</th>
                                            <th>영향 행 수</th>
                                        </tr>
                                    </thead>
                                    <tbody id="menuExecutionLogTableBody">
                                        <!-- 메뉴 실행 기록이 여기에 동적으로 추가됩니다 -->
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 모니터링 비활성화 알림 모달 -->
    <div class="modal fade" id="monitoringDisabledModal" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">
                        <i class="fa fa-exclamation-triangle text-warning"></i> 모니터링 비활성화
                    </h4>
                </div>
                <div class="modal-body">
                    <p>현재 모니터링이 활성화된 데이터베이스 연결이 없습니다.</p>
                    <p>대시보드 차트를 정상적으로 표시하려면 다음 작업을 수행하세요:</p>
                    <ul>
                        <li>연결 관리에서 데이터베이스 연결의 모니터링을 활성화하세요.</li>
                        <li>연결 상태가 '활성'인지 확인하세요.</li>
                    </ul>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">닫기</button>
                    <button type="button" class="btn btn-primary" onclick="location.href='/Connection'">연결 관리로 이동</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 차트 오류 알림 모달 -->
    <div class="modal fade" id="chartErrorModal" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">
                        <i class="fa fa-exclamation-circle text-danger"></i> 차트 오류
                    </h4>
                </div>
                <div class="modal-body">
                    <p id="chartErrorMessage">차트 데이터를 불러오는 중 오류가 발생했습니다.</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">닫기</button>
                    <button type="button" class="btn btn-primary" onclick="refreshCharts()">다시 시도</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 모니터링 비활성화 메시지 표시 함수 -->
    <script>
        function showMonitoringDisabledMessage() {
            // 이미 표시된 경우 중복 표시하지 않음
            if ($('#monitoringDisabledModal').data('shown')) {
                return;
            }
            
            $('#monitoringDisabledModal').modal('show');
            $('#monitoringDisabledModal').data('shown', true);
            
            // 30초 후 자동으로 숨김
            setTimeout(function() {
                $('#monitoringDisabledModal').modal('hide');
                $('#monitoringDisabledModal').data('shown', false);
                window.monitoringDisabledShown = false; // 플래그 초기화
            }, 30000);
        }
        
        // 차트 오류 메시지 표시 함수
        function showChartErrorMessage(errorMessage) {
            $('#chartErrorMessage').text(errorMessage || '차트 데이터를 불러오는 중 오류가 발생했습니다.');
            $('#chartErrorModal').modal('show');
        }
        
        // 차트 새로고침 함수
        function refreshCharts() {
            // 해시 초기화하여 강제 새로고침
            chartHashes = {
                'APPL_COUNT': null,
                'LOCK_WAIT_COUNT': null,
                'ACTIVE_LOG': null,
                'FILESYSTEM': null
            };
            
            // 모니터링 비활성화 플래그 초기화
            window.monitoringDisabledShown = false;
            
            // 차트 업데이트 실행
            updateCharts();
            
            // 모달 닫기
            $('#chartErrorModal').modal('hide');
        }
        
        // ========================================
        // Toast 알림 시스템
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
        
        // 에러 로그 함수 (console.log 대체)
        function logError(message, data) {
            if (console && console.error) {
                console.error(message, data);
            }
        }
        
        // 디버그 로그 함수 (console.log 대체)
        function logDebug(message, data) {
            if (console && console.log) {
                console.log(message, data);
            }
        }
    </script>
    
    <!-- Toast 알림 컨테이너 -->
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
        /* LOCK_WAIT_COUNT 컨테이너 호버 효과 */
        .lock-wait-count-container:hover {
            background-color: #f8f9fa !important;
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
        }
        
        .lock-wait-count-container:hover #lockWaitCountValue {
            transform: scale(1.05);
            transition: transform 0.3s ease;
        }
        
        .lock-wait-count-container:hover #lockWaitCountStatus {
            color: #007bff !important;
        }
    </style>
    
    <!-- ParamForm 추가 (sendSql 방식 지원) -->
    <form role="form-horizontal" name="ParamForm" id="ParamForm" action="javascript:void(0);" style="display: none;">
        <input type="hidden" id="sendvalue" name="sendvalue">
        <input id="Path" name="Path" value="" type="hidden">
    </form>
    
</body>
