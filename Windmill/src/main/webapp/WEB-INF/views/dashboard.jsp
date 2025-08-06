<%@include file="common/common.jsp"%>
    <script>
        // 연결 상태 모니터링 관련 변수
        var connectionStatusInterval;
        var applCountChart;
        var lockWaitCountChart;
        var activeLogChart;
        var filesystemChart;

        // 페이지 로드 시 연결 모니터링 시작
        $(document).ready(function() {
            startConnectionMonitoring();
            initializeCharts();
            startChartMonitoring();
        });

        // 페이지 언로드 시 연결 모니터링 중지
        $(window).on('beforeunload', function() {
            stopConnectionMonitoring();
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
            var statusIcon = conn.status === 'connected' ? 'fa-check-circle' : 'fa-times-circle';
            var statusText = conn.status === 'connected' ? '연결됨' : '연결실패';
            var formattedTime = formatDateTime(conn.lastChecked);
            
            // 상태에 따른 CSS 클래스 결정
            var statusClass = '';
            if (conn.status === 'connected') {
                statusClass = 'connected';
            } else if (conn.status === 'error') {
                statusClass = 'error';
            } else {
                statusClass = 'disconnected';
            }
            
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
            
            var statusIcon = conn.status === 'connected' ? 'fa-check-circle' : 'fa-times-circle';
            var statusText = conn.status === 'connected' ? '연결됨' : '연결실패';
            var formattedTime = formatDateTime(conn.lastChecked);
            
            // 상태에 따른 CSS 클래스 결정
            var statusClass = '';
            if (conn.status === 'connected') {
                statusClass = 'connected';
            } else if (conn.status === 'error') {
                statusClass = 'error';
            } else {
                statusClass = 'disconnected';
            }
            
            // 상태 텍스트 업데이트
            $('#status-' + conn.connectionName).html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
            
            // 마지막 확인 시간 업데이트
            $('#lastChecked-' + conn.connectionName).text(formattedTime);
            
            // 카드 클래스 업데이트
            var connectionCard = card.find('.connection-card');
            connectionCard.removeClass('connected disconnected error').addClass(statusClass);
            
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
                            'rgba(76, 175, 80, 0.7)',
                            'rgba(255, 193, 7, 0.7)',
                            'rgba(244, 67, 54, 0.7)',
                            'rgba(158, 158, 158, 0.7)'
                        ],
                        borderColor: [
                            'rgba(76, 175, 80, 1)',
                            'rgba(255, 193, 7, 1)',
                            'rgba(244, 67, 54, 1)',
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
                        backgroundColor: 'rgba(255, 99, 132, 0.8)',
                        borderColor: 'rgba(255, 99, 132, 1)',
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
                                return 'rgba(220, 53, 69, 0.8)'; // 빨간색 (80% 이상)
                            } else if (value >= 60) {
                                return 'rgba(255, 193, 7, 0.8)'; // 노란색 (60-79%)
                            } else {
                                return 'rgba(40, 167, 69, 0.8)'; // 초록색 (60% 미만)
                            }
                        },
                        borderColor: function(context) {
                            var value = context.dataset.data[context.dataIndex];
                            if (value >= 80) {
                                return 'rgba(220, 53, 69, 1)';
                            } else if (value >= 60) {
                                return 'rgba(255, 193, 7, 1)';
                            } else {
                                return 'rgba(40, 167, 69, 1)';
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
                                return 'rgba(220, 53, 69, 0.8)'; // 빨간색 (80% 이상)
                            } else if (value >= 60) {
                                return 'rgba(255, 193, 7, 0.8)'; // 노란색 (60-79%)
                            } else {
                                return 'rgba(40, 167, 69, 0.8)'; // 초록색 (60% 미만)
                            }
                        },
                        borderColor: function(context) {
                            var value = context.dataset.data[context.dataIndex];
                            if (value >= 80) {
                                return 'rgba(220, 53, 69, 1)';
                            } else if (value >= 60) {
                                return 'rgba(255, 193, 7, 1)';
                            } else {
                                return 'rgba(40, 167, 69, 1)';
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
        }

        // 차트 데이터 업데이트 함수
        function updateCharts() {
            // 모든 차트 데이터를 병렬로 요청
            Promise.all([
                // APPL_COUNT 데이터 조회
                $.ajax({
                    type: 'post',
                    url: '/Dashboard/APPL_COUNT'
                }),
                // LOCK_WAIT_COUNT 데이터 조회
                $.ajax({
                    type: 'post',
                    url: '/Dashboard/LOCK_WAIT_COUNT'
                }),
                // ACTIVE_LOG 데이터 조회
                $.ajax({
                    type: 'post',
                    url: '/Dashboard/ACTIVE_LOG'
                }),
                // FILESYSTEM 데이터 조회
                $.ajax({
                    type: 'post',
                    url: '/Dashboard/FILE_SYSTEM'
                })
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
                    }
                ];

                // 모든 차트 업데이트
                chartConfigs.forEach(function(config) {
                    if (config.data && config.data.result && Array.isArray(config.data.result)) {
                        var processedData = config.processData(config.data.result);
                        
                        config.chart.data.labels = processedData.labels;
                        config.chart.data.datasets[0].data = processedData.data;
                        
                        // LOCK_WAIT_COUNT 차트의 경우 Y축 최대값 설정
                        if (processedData.yAxisMax) {
                            config.chart.options.scales.y.max = processedData.yAxisMax;
                        }
                        
                        config.chart.update();
                    }
                });

                // 모든 차트 업데이트 완료 후 10초 후에 다시 실행
                setTimeout(function() {
                    updateCharts();
                }, 10000);

            }).catch(function(error) {
                console.error('차트 데이터 조회 중 오류 발생:', error);
                // 오류 발생 시에도 10초 후에 다시 시도
                setTimeout(function() {
                    updateCharts();
                }, 10000);
            });
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
    <style type="text/css">
        .connection-card {
            border: none;
            border-radius: 16px;
            padding: 20px;
            text-align: center;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            box-shadow: 0 8px 32px rgba(0,0,0,0.1);
            transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            cursor: pointer;
            height: 120px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            position: relative;
            overflow: hidden;
        }
        
        .connection-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.05) 100%);
            opacity: 0;
            transition: opacity 0.3s ease;
        }
        
        .connection-card:hover {
            transform: translateY(-8px);
            box-shadow: 0 16px 48px rgba(0,0,0,0.2);
        }
        
        .connection-card:hover::before {
            opacity: 1;
        }
        
        .connection-card.connected {
            background: #28a745;
        }
        
        .connection-card.disconnected {
            background: #dc3545;
        }
        
        .connection-card.error {
            background: #dc3545;
        }
        
        .connection-card .fa-database {
            font-size: 28px;
            margin-bottom: 3px;
            color: rgba(255,255,255,0.9);
            position: relative;
            z-index: 1;
        }
        
        .connection-card .connection-name {
            font-weight: 600;
            margin-bottom: 1px;
            color: rgba(255,255,255,0.95);
            font-size: 16px;
            position: relative;
            z-index: 1;
        }
        
        .connection-card .status-text {
            font-size: 11px;
            margin-bottom: 6px;
            color: rgba(255,255,255,0.8);
            font-weight: 500;
            position: relative;
            z-index: 1;
        }
        
        .connection-card .last-checked {
            font-size: 9px;
            color: rgba(255,255,255,0.7);
            margin-top: 2px;
            position: relative;
            z-index: 1;
        }
    </style>
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
        </div>
    </div>

    
</body>
