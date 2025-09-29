<%@include file="common/common.jsp"%>

<div class="content-wrapper" style="margin-left: 0">
    <section class="content-header">
        <h1><i class="fa fa-cog"></i> 환경설정 관리</h1>
    </section>
    <section class="content">
        <div class="row">
            <div class="col-md-12">
                <div class="box">
                    <div class="box-header with-border">
                        <h3 class="box-title">시스템 설정</h3>
                    </div>
                    <div class="box-body">
                        <form id="configForm">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="form-group">
                                        <label for="sessionTimeout">세션 타임아웃 (분)</label>
                                        <input type="number" class="form-control" id="sessionTimeout" name="sessionTimeout" 
                                               value="${configValues.SESSION_TIMEOUT}" min="1" max="1440" required>
                                        <small class="help-block">사용자 세션이 유지되는 시간 (1-1440분)</small>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="form-group">
                                        <label for="downloadIpPattern">다운로드 허용 IP 패턴</label>
                                        <input type="text" class="form-control" id="downloadIpPattern" name="downloadIpPattern" 
                                               value="${configValues.DOWNLOAD_IP_PATTERN}" placeholder="예: 10.240.13.* 또는 *">
                                        <small class="help-block">* = 모든 IP 허용, 특정 IP 패턴 입력 가능</small>
                                    </div>
                                </div>
                            </div>
                            
                            <hr>
                            
                            <div class="row">
                                <div class="col-md-6">
                                    <h4><i class="fa fa-bullhorn"></i> 공지사항 설정</h4>
                                    
                                    <div class="form-group">
                                        <div class="checkbox">
                                            <label>
                                                <input type="checkbox" id="noticeEnabled" name="noticeEnabled" 
                                                       ${configValues.NOTICE_ENABLED == 'true' ? 'checked' : ''}>
                                                공지사항을 화면에 표시합니다
                                            </label>
                                        </div>
                                    </div>
                                    
                                    <div class="form-group">
                                        <label for="noticeContent">공지사항 내용</label>
                                        <textarea class="form-control" id="noticeContent" name="noticeContent" rows="4" 
                                                  placeholder="공지사항 내용을 입력하세요">${configValues.NOTICE_CONTENT}</textarea>
                                        <small class="help-block">공지사항이 활성화된 경우 화면 상단에 표시됩니다</small>
                                    </div>
                                </div>
                                <div class="col-md-12">
                                    <h4><i class="fa fa-line-chart"></i> 대시보드 설정</h4>
                                    
                                    <!-- 차트 관리 테이블 -->
                                    <div class="panel panel-default" style="margin-bottom: 15px;">
                                        <div class="panel-heading" style="padding: 8px 15px;">
                                            <h4 class="panel-title" style="font-size: 14px; margin: 0;">
                                                <i class="fa fa-line-chart"></i> 차트 관리
                                            </h4>
                                        </div>
                                        <div class="panel-body" style="padding: 10px 15px;">
                                            <div class="table-responsive">
                                                <table class="table table-bordered align-middle table-condensed" id="chartTable">
                                                    <thead>
                                                        <tr>
                                                            <th style="width: 50px; font-size: 11px;">
                                                                <div data-toggle="tooltip" data-placement="top" title="차트의 표시 순서를 설정합니다. 숫자가 작을수록 먼저 표시됩니다.">순서</div>
                                                            </th>
                                                            <th style="width: 150px; font-size: 11px;">
                                                                <div data-toggle="tooltip" data-placement="top" title="대시보드에 표시할 차트의 ID입니다. SQL 템플릿 ID와 일치해야 합니다.">차트 ID</div>
                                                            </th>
                                                            <th style="width: 120px; font-size: 11px;">
                                                                <div data-toggle="tooltip" data-placement="top" title="차트의 표시 형식을 선택합니다. 선택하지 않으면 텍스트만 표시됩니다.">차트 형식</div>
                                                            </th>
                                                            <th style="width: 40px; font-size: 11px;">삭제</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody id="chartTableBody">
                                                        <!-- 차트들이 여기에 동적으로 추가됩니다 -->
                                                    </tbody>
                                                </table>
                                            </div>
                                            <button type="button" class="btn btn-primary btn-sm" id="addChartBtn">
                                                <i class="fa fa-plus"></i> 차트 추가
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <hr>
                            
                            <div class="form-group text-center">
                                <button type="submit" class="btn btn-primary">
                                    <i class="fa fa-save"></i> 설정 저장
                                </button>
                                <button type="button" class="btn btn-default" onclick="location.reload()">
                                    <i class="fa fa-refresh"></i> 새로고침
                                </button>
                            </div>
                        </form>
                        
                        <div id="alertContainer"></div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>
    
<script>
$(document).ready(function() {
    // 차트 추가 버튼 이벤트
    $('#addChartBtn').on('click', function() {
        addChart();
    });
    
    // 차트 삭제 이벤트 (이벤트 위임)
    $(document).on('click', '.chart-delete-btn', function() {
        $(this).closest('tr').remove();
        reorderCharts();
    });
    
    // 차트 위로/아래로 버튼 이벤트 (이벤트 위임)
    $(document).on('click', '.move-up', function() {
        var $row = $(this).closest('tr');
        var $prev = $row.prev();
        if ($prev.length) {
            $row.insertBefore($prev);
            reorderCharts();
        }
    });
    
    $(document).on('click', '.move-down', function() {
        var $row = $(this).closest('tr');
        var $next = $row.next();
        if ($next.length) {
            $row.insertAfter($next);
            reorderCharts();
        }
    });
    
    // 폼 제출 처리
    $('#configForm').on('submit', function(e) {
        e.preventDefault();
        
        var formData = {
            sessionTimeout: $('#sessionTimeout').val(),
            downloadIpPattern: $('#downloadIpPattern').val(),
            noticeContent: $('#noticeContent').val(),
            noticeEnabled: $('#noticeEnabled').is(':checked'),
            dashboardCharts: JSON.stringify(getDashboardChartsData())
        };
        
        // 유효성 검사
        if (!formData.sessionTimeout || formData.sessionTimeout < 1 || formData.sessionTimeout > 1440) {
            showAlert('세션 타임아웃은 1-1440분 사이의 값이어야 합니다.', 'danger');
            return;
        }
        
        if (!formData.downloadIpPattern.trim()) {
            showAlert('다운로드 IP 패턴을 입력해주세요.', 'danger');
            return;
        }
        
        console.log(getDashboardChartsData())
        
        // 저장 요청
        $.ajax({
            url: '/SystemConfig/save',
            type: 'POST',
            data: formData,
            success: function(response) {
                if (response.success) {
                    showAlert(response.message, 'success');
                } else {
                    showAlert(response.message, 'danger');
                }
            },
            error: function(xhr, status, error) {
                showAlert('서버 오류가 발생했습니다: ' + error, 'danger');
            }
        });
    });
    
    // 공지사항 활성화 체크박스 변경 시
    $('#noticeEnabled').on('change', function() {
        if ($(this).is(':checked')) {
            $('#noticeContent').prop('required', true);
        } else {
            $('#noticeContent').prop('required', false);
        }
    });
});

// 대시보드 차트 관리 변수
var chartIndex = 0;
var templateList = [];

// 페이지 로드 시 초기화
$(document).ready(function() {
    loadTemplateList();
    loadDashboardCharts();
    
    // 차트 추가 버튼 이벤트
    $('#addChartBtn').on('click', function() {
        addChartItem();
    });
    
    // 차트 삭제 이벤트 (이벤트 위임)
    $(document).on('click', '.remove-chart-btn', function() {
        $(this).closest('.chart-item').remove();
    });
});

// 템플릿 목록 로드
function loadTemplateList() {
    $.ajax({
        type: 'GET',
        url: '/SQLTemplate/list',
        success: function(result) {
            if (result.success) {
                window.templateList = result.data;
            }
        },
        error: function() {
            console.error('템플릿 목록 로드 실패');
        }
    });
}

// 대시보드 차트 설정 로드
function loadDashboardCharts() {
    $.ajax({
        type: 'GET',
        url: '/SystemConfig/getDashboardConfig',
        success: function(result) {
            if (result.success && result.dashboardConfig) {
                var charts = result.dashboardConfig.dashboardCharts || [];
                renderCharts(charts);
            }
        },
        error: function() {
            console.error('대시보드 설정 로드 실패');
        }
    });
}

// 차트 렌더링 (파라미터 관리와 동일한 방식)
function renderCharts(charts) {
    $('#chartTableBody').empty();
    if (charts.length === 0) {
        return;
    }
    
    charts.forEach(function(chart, index) {
        var order = index + 1;
        var row = createChartRow(chart, order);
        $('#chartTableBody').append(row);
    });
    
    // 툴팁 초기화
    $('#chartTableBody').find('[data-toggle="tooltip"]').tooltip({
        placement: 'top',
        trigger: 'hover'
    });
}

// 차트 행 생성 함수
function createChartRow(chart, order) {
    var rowHtml = '<tr class="chart-row">' +
        '<td><div>' +
        '<button type="button" class="btn btn-xs btn-default move-up" title="위로"><i class="fa fa-chevron-up"></i></button><br> ' +
        '<button type="button" class="btn btn-xs btn-default move-down" title="아래로"><i class="fa fa-chevron-down"></i></button>' +
        '<input type="hidden" class="chart-order" value="' + order + '">' +
        '</div></td>' +
        '<td><input type="text" class="form-control chart-id-input" value="' + 
        (chart.id || '') + '" placeholder="차트 ID 입력 또는 선택"></td>' +
        '<td><select class="form-control chart-type-select">' +
        '<option value=""' + (chart.type === '' ? ' selected' : '') + '>선택 안함 (텍스트만 표시)</option>' +
        '<option value="line"' + (chart.type === 'line' ? ' selected' : '') + '>라인 차트</option>' +
        '<option value="bar"' + (chart.type === 'bar' ? ' selected' : '') + '>바 차트</option>' +
        '<option value="pie"' + (chart.type === 'pie' ? ' selected' : '') + '>파이 차트</option>' +
        '<option value="doughnut"' + (chart.type === 'doughnut' ? ' selected' : '') + '>도넛 차트</option>' +
        '</select></td>' +
        '<td><button type="button" class="btn btn-danger btn-xs chart-delete-btn"><i class="fa fa-minus"></i></button></td>' +
        '</tr>';
    
    return $(rowHtml);
}

// 차트 추가
function addChart() {
    var currentOrder = $('#chartTableBody tr').length + 1;
    var chart = { id: '', type: '' };
    var row = createChartRow(chart, currentOrder);
    $('#chartTableBody').append(row);
    
    // 차트 ID 입력 필드에 autocomplete 적용
    initializeChartIdAutocomplete();
}


// 차트 순서 재정렬
function reorderCharts() {
    $('#chartTableBody tr').each(function(index) {
        $(this).find('.chart-order').val(index + 1);
    });
}

// 대시보드 차트 데이터 수집
function getDashboardChartsData() {
    var charts = [];
    $('#chartTableBody tr').each(function() {
        var chartId = $(this).find('.chart-id-input').val().trim();
        var chartType = $(this).find('.chart-type-select').val();
        
        if (chartId) {
            charts.push({
                id: chartId,
                type: chartType || ''
            });
        }
    });
    return charts;
}

// 차트 ID 자동완성 기능 초기화
function initializeChartIdAutocomplete() {
    $('.chart-id-input').each(function() {
        var $input = $(this);
        
        // 자동완성 드롭다운 생성
        var $dropdown = $('<div class="chart-id-dropdown" style="display: none; position: absolute; z-index: 1000; background: white; border: 1px solid #ccc; max-height: 200px; overflow-y: auto; width: 100%;"></div>');
        $input.after($dropdown);
        
        // 입력 이벤트 처리
        $input.on('input', function() {
            var query = $(this).val().toLowerCase();
            if (query.length > 0) {
                var filteredTemplates = window.templateList.filter(function(template) {
                    return template.TEMPLATE_ID.toLowerCase().includes(query) || 
                           template.TEMPLATE_NAME.toLowerCase().includes(query);
                });
                
                if (filteredTemplates.length > 0) {
                    var html = '';
                    filteredTemplates.forEach(function(template) {
                        html += '<div class="chart-id-option" data-value="' + template.TEMPLATE_ID + '" style="padding: 8px; cursor: pointer; border-bottom: 1px solid #eee;">' +
                                '<strong>' + template.TEMPLATE_ID + '</strong><br>' +
                                '<small style="color: #666;">' + template.TEMPLATE_NAME + '</small>' +
                                '</div>';
                    });
                    $dropdown.html(html).show();
                } else {
                    $dropdown.hide();
                }
            } else {
                $dropdown.hide();
            }
        });
        
        // 옵션 클릭 이벤트
        $dropdown.on('click', '.chart-id-option', function() {
            var value = $(this).data('value');
            $input.val(value);
            $dropdown.hide();
        });
        
        // 포커스 아웃 시 드롭다운 숨김
        $input.on('blur', function() {
            setTimeout(function() {
                $dropdown.hide();
            }, 200);
        });
    });
}

function showAlert(message, type) {
    var alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
    var iconClass = type === 'success' ? 'fa-check-circle' : 'fa-exclamation-triangle';
    
    var alertHtml = '<div class="alert ' + alertClass + ' alert-dismissible" role="alert">' +
                   '<button type="button" class="close" data-dismiss="alert" aria-label="Close">' +
                   '<span aria-hidden="true">&times;</span>' +
                   '</button>' +
                   '<i class="fa ' + iconClass + '"></i> ' + message +
                   '</div>';
    
    $('#alertContainer').html(alertHtml);
    
    // 성공 메시지는 3초 후 자동으로 사라짐
    if (type === 'success') {
        setTimeout(function() {
            $('.alert-success').fadeOut();
        }, 3000);
    }
}
</script>
