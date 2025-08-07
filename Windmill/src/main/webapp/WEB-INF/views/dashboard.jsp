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

        // 페이지 로드 시 연결 모니터링 시작
        $(document).ready(function() {
            startConnectionMonitoring();
            startDexStatusMonitoring();
            initializeCharts();
            startChartMonitoring();
        });

        // 페이지 언로드 시 연결 모니터링 중지
        $(window).on('beforeunload', function() {
            stopConnectionMonitoring();
            stopDexStatusMonitoring();
            stopChartMonitoring();
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
                error: function() {
                    console.error('연결 상태 조회 실패');
                    // 에러 발생 시에도 다음 타이머 설정
                    scheduleNextRefresh();
                }
            });
        }

        // 연결 상태 표시 업데이트 함수
        function updateConnectionStatusDisplay(connections) {
            var container = $('#connectionStatusContainer');
            
            // 초기 로드인지 확인 (컨테이너가 비어있으면 초기 로드)
            var isInitialLoad = container.children().length === 0;
            
            if (isInitialLoad) {
                // 초기 로드: 전체 카드 생성
                connections.forEach(function(conn) {
                    createConnectionCard(conn);
                });
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
                '<div class="col-md-2 col-sm-3 col-xs-4" style="margin-bottom: 15px;" id="card-' + conn.connectionName + '">' +
                    '<div class="connection-card ' + statusClass + '" onclick="refreshSingleConnection(\'' + conn.connectionName + '\')">' +
                        '<div>' +
                            '<i class="fa fa-database"></i>' +
                        '</div>' +
                        '<div class="connection-name">' +
                            conn.connectionName +
                        '</div>' +
                        '<div class="status-text" id="status-' + conn.connectionName + '">' +
                            '<i class="fa ' + statusIcon + '"></i> ' + statusText +
                        '</div>' +
                        '<div class="last-checked" id="lastChecked-' + conn.connectionName + '">' +
                            formattedTime +
                        '</div>' +
                    '</div>' +
                '</div>';
            
            $('#connectionStatusContainer').append(connectionCard);
        }
        
        // 연결 카드 상태 업데이트 함수
        function updateConnectionCard(conn) {
            var card = $('#card-' + conn.connectionName);
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
            $('#status-' + conn.connectionName).html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
            
            // 마지막 확인 시간 업데이트
            $('#lastChecked-' + conn.connectionName).text(formattedTime);
            
            // 카드 클래스 업데이트
            var connectionCard = card.find('.connection-card');
            connectionCard.removeClass('connected disconnected error checking').addClass(statusClass);
            
            // 상태 아이콘 색상 업데이트
            //$('#status-' + conn.connectionName).css('color', conn.color);
        }

        // 단일 연결 상태 수동 새로고침
        function refreshSingleConnection(connectionName) {
            $.ajax({
                type: 'post',
                url: '/Connection/status/refresh',
                data: {
                    connectionName: connectionName
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
            var date = new Date(dateTimeStr);
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
            
            // 10초 후에 다음 연결 상태 확인 실행
            connectionStatusInterval = setTimeout(function() {
                refreshConnectionStatus();
            }, 10000);
        }

        // 자동 새로고침 중지 함수
        function stopConnectionMonitoring() {
            if (connectionStatusInterval) {
                clearTimeout(connectionStatusInterval);
                connectionStatusInterval = null;
            }
        }

        // DEX 상태 새로고침 함수
        function refreshDexStatus() {
            $.ajax({
                type: 'post',
                url: '/DexStatus/status',
                timeout: 10000,
                success: function(result) {
                    updateDexStatusDisplay(result);
                    scheduleNextDexRefresh();
                },
                error: function(xhr, status, error) {
                    console.error('DEX 상태 조회 실패:', error);
                    scheduleNextDexRefresh();
                }
            });
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

        // DEX 상태 모니터링 시작
        function startDexStatusMonitoring() {
            console.log('DEX 상태 모니터링 시작');
            refreshDexStatus();
        }
        
        // DEX 상태 모니터링 중지
        function stopDexStatusMonitoring() {
            console.log('DEX 상태 모니터링 중지');
            if (dexStatusInterval) {
                clearTimeout(dexStatusInterval);
                dexStatusInterval = null;
            }
        }
        
        // 다음 DEX 상태 새로고침 스케줄링
        function scheduleNextDexRefresh() {
            if (dexStatusInterval) {
                clearTimeout(dexStatusInterval);
            }
            dexStatusInterval = setTimeout(function() {
                refreshDexStatus();
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
                console.log('도넛 차트 생성 완료:', canvasId);
                
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
            }
            
            if (chart) {
                // 차트 데이터 업데이트 (새로운 구조에 맞게)
                chart.data.datasets[0].data = [value, 100 - value];
                
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
                    $('#cpu-value').text(status.cpuUsage.toFixed(1) + '%');
                }
                if (status.memoryUsage !== undefined) {
                    updateDoughnutChart('memory-doughnut', status.memoryUsage);
                    $('#memory-value').text(status.memoryUsage.toFixed(1) + '%');
                }
            }
        }



        // 차트 초기화 함수
        function initializeCharts() {
            // Chart.js datalabels 플러그인 등록
            Chart.register(ChartDataLabels);
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
                }
            });

            // LOCK_WAIT_COUNT 차트 (세로 막대그래프)
            var lockWaitCountCtx = document.getElementById('lockWaitCountChart').getContext('2d');
            lockWaitCountChart = new Chart(lockWaitCountCtx, {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'LOCK_WAIT_COUNT',
                        data: [],
                        backgroundColor: 'rgba(140, 214, 16, 0.8)',
                        borderColor: 'rgba(140, 214, 16, 1)',
                        borderWidth: 1,
                        datalabels: {
                        	align: 'end',
                            anchor: 'end',
                          }
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    },
                    plugins: {
                    	legend: {
                    		display: false,
                        },
                    },
                    layout: {
                        padding: 15
                    },
                }
            });

            // ACTIVE_LOG 차트 (가로 막대그래프 - 사용량)
            var activeLogCtx = document.getElementById('activeLogChart').getContext('2d');
            activeLogChart = new Chart(activeLogCtx, {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'ACTIVE_LOG',
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
                    }
                }
            });

            const COLORS = ['rgb(140, 214, 16)', 'rgb(239, 198, 0)', 'rgb(231, 24, 49)'];
            const MIN = 0;
            const MAX = 100;
            
            function index(perc) {
            	  return perc < 70 ? 0 : perc < 90 ? 1 : 2;
            	}

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
                                memoryLabel: {
                                    type: 'doughnutLabel',
                                    content: ({chart}) => {
                                        const value = chart.data.datasets[0].data[0] || 0;
                                        return [
                                            value.toFixed(1) + '%',
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
                                        return [COLORS[index(value)], 'grey'];
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }

        // 차트 데이터 업데이트 함수
        function updateCharts() {
            console.log('차트 데이터 업데이트 시작');
            
            // 모든 차트 데이터를 병렬로 요청
            Promise.all([
                // APPL_COUNT 데이터 조회
                new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/APPL_COUNT',
                        timeout: 10000,
                        success: function(data) {
                            resolve(data);
                        },
                        error: function(xhr, status, error) {
                            console.error('APPL_COUNT 데이터 조회 실패:', error);
                            resolve({ error: 'APPL_COUNT 조회 실패', result: [] });
                        }
                    });
                }),
                // LOCK_WAIT_COUNT 데이터 조회
                new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/LOCK_WAIT_COUNT',
                        timeout: 10000,
                        success: function(data) {
                            resolve(data);
                        },
                        error: function(xhr, status, error) {
                            console.error('LOCK_WAIT_COUNT 데이터 조회 실패:', error);
                            resolve({ error: 'LOCK_WAIT_COUNT 조회 실패', result: [] });
                        }
                    });
                }),
                // ACTIVE_LOG 데이터 조회
                new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/ACTIVE_LOG',
                        timeout: 10000,
                        success: function(data) {
                            resolve(data);
                        },
                        error: function(xhr, status, error) {
                            console.error('ACTIVE_LOG 데이터 조회 실패:', error);
                            resolve({ error: 'ACTIVE_LOG 조회 실패', result: [] });
                        }
                    });
                }),
                // FILESYSTEM 데이터 조회
                new Promise(function(resolve, reject) {
                    $.ajax({
                        type: 'post',
                        url: '/Dashboard/FILE_SYSTEM',
                        timeout: 10000,
                        success: function(data) {
                            resolve(data);
                        },
                        error: function(xhr, status, error) {
                            console.error('FILESYSTEM 데이터 조회 실패:', error);
                            resolve({ error: 'FILESYSTEM 조회 실패', result: [] });
                        }
                    });
                }),

            ]).then(function(results) {
                // 차트 설정 정의
                var chartConfigs = [
                    {
                        chart: applCountChart,
                        data: results[0],
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
                    {
                        chart: lockWaitCountChart,
                        data: results[1],
                        processData: function(result) {
                            var labels = [];
                            var data = [];
                            for (var i = 0; i < result.length; i++) {
                                if (result[i] && result[i].length >= 2) {
                                    labels.push(result[i][0]); // color
                                    data.push(result[i][1]);  // value
                                }
                            }
                            // 최대값 계산하여 Y축 최대값 설정 (정수로)
                            var maxValue = Math.max(...data);
                            var yAxisMax = Math.ceil(maxValue * 1.1);
                            return { labels: labels, data: data, yAxisMax: yAxisMax };
                        }
                    },
                    {
                        chart: activeLogChart,
                        data: results[2],
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
                    {
                        chart: filesystemChart,
                        data: results[3],
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
                    },

                ];

                // 모든 차트 업데이트
                chartConfigs.forEach(function(config, index) {
                    try {
                        // 에러가 있는 경우 처리
                        if (config.data && config.data.error) {
                            console.warn('차트 데이터 에러:', config.data.error);
                            // 에러 상태를 차트에 표시 (빈 데이터로 설정)
                            config.chart.data.labels = ['데이터 없음'];
                            config.chart.data.datasets[0].data = [1];
                            config.chart.update();
                            return;
                        }
                        
                        // 정상 데이터 처리
                        if (config.data && config.data.result && Array.isArray(config.data.result)) {
                            var processedData = config.processData(config.data.result);
                            
                            // 데이터 유효성 검사
                            if (processedData && processedData.labels && processedData.data) {
                                config.chart.data.labels = processedData.labels;
                                config.chart.data.datasets[0].data = processedData.data;
                                
                                // LOCK_WAIT_COUNT 차트의 경우 Y축 최대값 설정
                                if (processedData.yAxisMax) {
                                    config.chart.options.scales.y.max = processedData.yAxisMax;
                                }
                                
                                config.chart.update();
                                console.log('차트 업데이트 성공:', index);
                            } else {
                                console.warn('차트 데이터 형식 오류:', index, processedData);
                                // 기본 데이터로 설정
                                config.chart.data.labels = ['데이터 없음'];
                                config.chart.data.datasets[0].data = [1];
                                config.chart.update();
                            }
                        } else {
                            console.warn('차트 데이터가 없습니다:', index, config.data);
                            // 기본 데이터로 설정
                            config.chart.data.labels = ['데이터 없음'];
                            config.chart.data.datasets[0].data = [1];
                            config.chart.update();
                        }
                    } catch (error) {
                        console.error('차트 업데이트 중 오류 발생:', index, error);
                        // 에러 발생 시 기본 데이터로 설정
                        try {
                            config.chart.data.labels = ['오류'];
                            config.chart.data.datasets[0].data = [1];
                            config.chart.update();
                        } catch (chartError) {
                            console.error('차트 복구 중 오류:', chartError);
                        }
                    }
                });

                console.log('모든 차트 업데이트 완료');
                
                // 다음 업데이트 스케줄링
                scheduleNextChartUpdate();
            }).catch(function(error) {
                console.error('차트 데이터 조회 중 전체 오류 발생:', error);
                // 전체 실패 시에도 다음 업데이트 스케줄링
                scheduleNextChartUpdate();
            });
        }

        // 다음 차트 업데이트 스케줄링
        function scheduleNextChartUpdate() {
            // 기존 타이머가 있으면 제거
            if (window.chartUpdateTimer) {
                clearTimeout(window.chartUpdateTimer);
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
            console.log('차트 모니터링 중지');
        }

        // 차트 모니터링 시작 함수
        function startChartMonitoring() {
            // 초기 데이터 로드
            updateCharts();
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
                    <div class="box box-primary">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-database"></i> 데이터베이스 연결 상태 모니터링
                            </h3>
                            <div class="box-tools pull-right">
                                <button type="button" class="btn btn-box-tool" onclick="refreshConnectionStatus()">
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
                <div class="col-md-2">
                    <div class="box box-info">
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

                <!-- LOCK_WAIT_COUNT 차트 -->
                <div class="col-md-4">
                    <div class="box box-warning">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-bar-chart"></i> LOCK_WAIT_COUNT
                            </h3>
                        </div>
                        <div class="box-body" style="height: 250px; display: flex; align-items: center; justify-content: center;">
                            <canvas id="lockWaitCountChart" style="max-height: 200px%; max-width: 100%;"></canvas>
                        </div>
                    </div>
                </div>

                <!-- ACTIVE_LOG 차트 -->
                <div class="col-md-3">
                    <div class="box box-success">
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
                    <div class="box box-danger">
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

            <!-- DEX 상태 모니터링 -->
            <div class="row">
                <div class="col-md-12">
                    <div class="box box-primary">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-server"></i> DEX 상태 모니터링
                            </h3>
                            <div class="box-tools pull-right">
                                <button type="button" class="btn btn-box-tool" onclick="refreshDexStatus()">
                                    <i class="fa fa-refresh"></i> 새로고침
                                </button>
                            </div>
                        </div>
                        <div class="box-body">
                            <div class="row">
                                <!-- DEX 상태 카드들 (왼쪽 세로 배치) -->
                                <div class="col-md-2">
                                    <div id="dexStatusContainer">
                                        <!-- DEX 상태가 여기에 동적으로 추가됩니다 -->
                                    </div>
                                </div>
                                <!-- DEX 도넛 차트들 (오른쪽 가로 배치) -->
                                <div class="col-md-6">
                                    <div class="row">
                                        <div class="col-md-6">
                                            <div class="doughnut-chart-item">
                                                <div class="doughnut-label">CPU 사용률</div>
                                                <canvas id="cpu-doughnut" width="1200" height="1200"></canvas>
                                            </div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="doughnut-chart-item">
                                                <div class="doughnut-label">메모리 사용률</div>
                                                <canvas id="memory-doughnut" width="1200" height="1200"></canvas>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    
</body>
