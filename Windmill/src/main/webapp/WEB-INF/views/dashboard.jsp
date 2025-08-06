<%@include file="common/common.jsp"%>
    <script>
        // 연결 상태 모니터링 관련 변수
        var connectionStatusInterval;
        var applCountChart;
        var lockWaitCountChart;

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
            console.log(connections);
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
            
            var connectionCard = 
                '<div class="col-md-3 col-sm-4 col-xs-6" style="margin-bottom: 15px;" id="card-' + conn.connectionName + '">' +
                    '<div class="connection-card" style="border-color: ' + conn.color + ';" onclick="refreshSingleConnection(\'' + conn.connectionName + '\')">' +
                        '<div class="fa-database" style="color: ' + conn.color + ';">' +
                            '<i class="fa fa-database"></i>' +
                        '</div>' +
                        '<div class="connection-name">' +
                            conn.connectionName +
                        '</div>' +
                        '<div class="status-text" style="color: ' + conn.color + ';" id="status-' + conn.connectionName + '">' +
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
            
            // 상태 텍스트 업데이트
            $('#status-' + conn.connectionName).html('<i class="fa ' + statusIcon + '"></i> ' + statusText);
            
            // 시간 업데이트
            $('#lastChecked-' + conn.connectionName).text(formattedTime);
            
            // 색상과 테두리 업데이트 (부드러운 전환 효과)
            var connectionCard = card.find('.connection-card');
            connectionCard.css('border-color', conn.color);
            
            // 데이터베이스 아이콘 색상 업데이트
            connectionCard.find('.fa-database').css('color', conn.color);
            
            // 상태 아이콘 색상 업데이트
            $('#status-' + conn.connectionName).css('color', conn.color);
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
            // APPL_COUNT 차트 (가로 막대그래프)
            var applCountCtx = document.getElementById('applCountChart').getContext('2d');
            applCountChart = new Chart(applCountCtx, {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'APPL_COUNT',
                        data: [],
                        backgroundColor: 'rgba(54, 162, 235, 0.8)',
                        borderColor: 'rgba(54, 162, 235, 1)',
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y', // 가로 막대그래프를 위한 설정
                    scales: {
                        x: {
                            beginAtZero: true
                        }
                    },
                    plugins: {
                        title: {
                            display: true,
                            text: 'APPL_COUNT'
                        }
                    }
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
                        borderWidth: 1
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
                        title: {
                            display: true,
                            text: 'LOCK_WAIT_COUNT'
                        }
                    }
                }
            });
        }

        // 차트 데이터 업데이트 함수
        function updateCharts() {
            // APPL_COUNT 데이터 조회
            $.ajax({
                type: 'post',
                url: '/Dashboard/applCount',
                success: function(result) {
                    if (result && result.labels && result.data) {
                        applCountChart.data.labels = result.labels;
                        applCountChart.data.datasets[0].data = result.data;
                        applCountChart.update('none'); // 애니메이션 없이 업데이트
                    }
                },
                error: function() {
                    console.error('APPL_COUNT 데이터 조회 실패');
                }
            });

            // LOCK_WAIT_COUNT 데이터 조회
            $.ajax({
                type: 'post',
                url: '/Dashboard/lockWaitCount',
                success: function(result) {
                    if (result && result.labels && result.data) {
                        lockWaitCountChart.data.labels = result.labels;
                        lockWaitCountChart.data.datasets[0].data = result.data;
                        lockWaitCountChart.update('none'); // 애니메이션 없이 업데이트
                    }
                },
                error: function() {
                    console.error('LOCK_WAIT_COUNT 데이터 조회 실패');
                }
            });
        }

        // 차트 모니터링 시작 함수
        function startChartMonitoring() {
            // 초기 데이터 로드
            updateCharts();
            
            // 30초마다 차트 데이터 업데이트
            setInterval(function() {
                updateCharts();
            }, 30000);
        }
    </script>
<head>
    <meta charset="UTF-8">
    <title>대시보드 - DeX</title>
    <%@include file="common/common.jsp"%>
    <style type="text/css">
        .connection-card {
            border: 2px solid #ddd;
            border-radius: 10px;
            padding: 15px;
            text-align: center;
            background: white;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            transition: all 0.3s ease;
            cursor: pointer;
            height: 100px;
            display: flex;
            flex-direction: column;
            justify-content: center;
        }
        
        .connection-card:hover {
            transform: scale(1.05);
        }
        
        .connection-card .fa-database {
            font-size: 24px;
            margin-bottom: 10px;
        }
        
        .connection-card .connection-name {
            font-weight: bold;
            margin-bottom: 5px;
            color: #333;
        }
        
        .connection-card .status-text {
            font-size: 12px;
            margin-bottom: 5px;
        }
        
        .connection-card .last-checked {
            font-size: 10px;
            color: #666;
            margin-top: 5px;
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
                <div class="col-md-6">
                    <div class="box box-info">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-bar-chart"></i> APPL_COUNT
                            </h3>
                        </div>
                        <div class="box-body">
                            <canvas id="applCountChart" style="height: 300px;"></canvas>
                        </div>
                    </div>
                </div>

                <!-- LOCK_WAIT_COUNT 차트 -->
                <div class="col-md-6">
                    <div class="box box-warning">
                        <div class="box-header with-border">
                            <h3 class="box-title">
                                <i class="fa fa-bar-chart"></i> LOCK_WAIT_COUNT
                            </h3>
                        </div>
                        <div class="box-body">
                            <canvas id="lockWaitCountChart" style="height: 300px;"></canvas>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    
</body>
