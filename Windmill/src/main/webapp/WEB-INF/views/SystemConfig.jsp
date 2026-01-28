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
                            </div>
                            
                            <hr>
                            
                            <div class="row">
                                <div class="col-md-12">
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
                            </div>
                            
                            <div class="form-group text-center">
                                <button type="submit" class="btn btn-primary">
                                    <i class="fa fa-save"></i> 설정 저장
                                </button>
                            </div>
                        </form>
                        
                        <div id="alertContainer"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 대시보드 차트 설정 -->
        <div class="row" style="margin-top: 20px;">
            <div class="col-md-12">
                <div class="box box-primary">
                    <div class="box-header with-border">
                        <h3 class="box-title">
                            <i class="fa fa-bar-chart"></i> 대시보드 차트 설정
                        </h3>
                    </div>
                    <div class="box-body">
                        <div class="table-responsive">
                            <table class="table table-bordered table-striped" id="chartConfigTable">
                                <thead>
                                    <tr>
                                        <th style="width: 50px;">순서</th>
                                        <th>템플릿</th>
                                        <th>차트 타입</th>
                                        <th style="width: 80px;">삭제</th>
                                    </tr>
                                </thead>
                                <tbody id="chartConfigContainer">
                                    <!-- 동적으로 차트 설정 항목들이 추가됩니다 -->
                                </tbody>
                            </table>
                        </div>
                        <div class="text-center" style="margin-top: 15px;">
                            <button type="button" class="btn btn-success" onclick="addChartConfig()">
                                <i class="fa fa-plus"></i> 차트 추가
                            </button>
                        </div>
                        
                        <div class="row" style="margin-top: 20px;">
                            <div class="col-md-12">
                                <button type="button" class="btn btn-primary" onclick="saveChartConfig()">
                                    <i class="fa fa-save"></i> 차트 설정 저장
                                </button>
                                <button type="button" class="btn btn-warning" onclick="resetChartErrors()">
                                    <i class="fa fa-refresh"></i> 에러 상태 리셋
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 대시보드 템플릿 설정 -->
        <div class="row" style="margin-top: 20px;">
            <div class="col-md-12">
                <div class="box box-info">
                    <div class="box-header with-border">
                        <h3 class="box-title">
                            <i class="fa fa-table"></i> 대시보드 템플릿 설정
                        </h3>
                    </div>
                    <div class="box-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="form-group">
                                    <label for="monitoringTemplateSelect">모니터링 템플릿</label>
                                    <select class="form-control template-select2" id="monitoringTemplateSelect" style="width: 100%;">
                                        <option value="">템플릿을 선택하세요</option>
                                    </select>
                                    <small class="help-block">대시보드에 표시할 모니터링 템플릿을 선택하세요</small>
                                </div>
                            </div>
                            
                            <div class="col-md-6">
                                <div class="form-group">
                                    <label for="monitoringConnectionSelect">데이터베이스 연결</label>
                                    <select class="form-control" id="monitoringConnectionSelect">
                                        <option value="">연결을 선택하세요</option>
                                    </select>
                                    <small class="help-block">템플릿을 실행할 데이터베이스 연결을 선택하세요</small>
                                </div>
                            </div>
                        </div>
                        
                        <div class="row" style="margin-top: 20px;">
                            <div class="col-md-12">
                                <button type="button" class="btn btn-primary" onclick="saveMonitoringTemplateConfig()">
                                    <i class="fa fa-save"></i> 템플릿 설정 저장
                                </button>
                                <button type="button" class="btn btn-danger" onclick="deleteMonitoringTemplateConfig()">
                                    <i class="fa fa-trash"></i> 설정 삭제
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>
    
<script>
$(document).ready(function() {
    // 폼 제출 처리
    $('#configForm').on('submit', function(e) {
        e.preventDefault();
        
        var formData = {
            sessionTimeout: $('#sessionTimeout').val(),
            noticeContent: $('#noticeContent').val(),
            noticeEnabled: $('#noticeEnabled').is(':checked')
        };
        
        // 유효성 검사
        if (!formData.sessionTimeout || formData.sessionTimeout < 1 || formData.sessionTimeout > 1440) {
            showAlert('세션 타임아웃은 1-1440분 사이의 값이어야 합니다.', 'danger');
            return;
        }
        
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

// 전역 변수: 템플릿 목록 캐시
var templateListCache = [];

// 대시보드 차트 설정 관련 함수들
function addChartConfig() {
    var chartCount = $('.chart-config-item').length;
    if (chartCount >= 12) {
        showAlert('최대 12개까지 설정할 수 있습니다.', 'warning');
        return;
    }
    
    // 임시 ID 생성 (템플릿과 차트 타입 선택 후 실제 ID로 업데이트됨)
    var chartId = 'temp_chart_' + Date.now();
    
    var row = $('<tr class="chart-config-item" data-chart-id="' + chartId + '">' +
        '<td style="text-align: center;">' +
        '<div style="display: flex; flex-direction: column; align-items: center;">' +
        '<button type="button" class="btn btn-xs btn-default move-up" title="위로" onclick="moveChartUp(\'' + chartId + '\')">' +
        '<i class="fa fa-chevron-up"></i>' +
        '</button>' +
        '<button type="button" class="btn btn-xs btn-default move-down" title="아래로" onclick="moveChartDown(\'' + chartId + '\')">' +
        '<i class="fa fa-chevron-down"></i>' +
        '</button>' +
        '</div>' +
        '</td>' +
        '<td>' +
        '<select class="form-control template-select2">' +
        '<option value="">템플릿을 선택하세요</option>' +
        '</select>' +
        '</td>' +
        '<td>' +
        '<select class="form-control chart-type-select">' +
        '<option value="">차트 타입을 선택하세요</option>' +
        '<option value="doughnut">도넛차트</option>' +
        '<option value="text">텍스트</option>' +
        '<option value="gauge">게이지</option>' +
        '<option value="bar">가로막대</option>' +
        '</select>' +
        '</td>' +
        '<td style="text-align: center;">' +
        '<button type="button" class="btn btn-danger btn-xs parameter-delete-btn" onclick="removeChartConfig(this)">' +
        '<i class="fa fa-minus"></i>' +
        '</button>' +
        '</td>' +
        '</tr>');
    
    $('#chartConfigContainer').append(row);
    
    // 새로 추가된 행의 템플릿 드롭다운에 옵션 로드 및 Select2 초기화
    loadTemplateOptions(row.find('.template-select2'));
}

function removeChartConfig(button) {
    $(button).closest('tr').remove();
}

function moveChartUp(chartId) {
    var $row = $('.chart-config-item[data-chart-id="' + chartId + '"]');
    var $prevRow = $row.prev('.chart-config-item');
    
    if ($prevRow.length > 0) {
        $row.insertBefore($prevRow);
    }
}

function moveChartDown(chartId) {
    var $row = $('.chart-config-item[data-chart-id="' + chartId + '"]');
    var $nextRow = $row.next('.chart-config-item');
    
    if ($nextRow.length > 0) {
        $row.insertAfter($nextRow);
    }
}

function loadTemplateOptions(selectElement, selectedValue) {
    if (templateListCache.length > 0) {
        // 캐시된 데이터 사용
        populateTemplateOptions(selectElement, selectedValue);
    } else {
        // 캐시가 없으면 로드 후 캐시에 저장
        $.ajax({
            type: 'GET',
            url: '/SQLTemplate/list',
            success: function(result) {
                if (result.success) {
                    templateListCache = result.data;
                    populateTemplateOptions(selectElement, selectedValue);
                }
            },
            error: function() {
                console.error('템플릿 목록 로드 실패');
            }
        });
    }
}

function populateTemplateOptions(selectElement, selectedValue) {
    var options = '<option value="">템플릿을 선택하세요</option>';
    templateListCache.forEach(function(template) {
        var selected = (selectedValue && selectedValue === template.TEMPLATE_ID) ? ' selected' : '';
        options += '<option value="' + template.TEMPLATE_ID + '"' + selected + '>' + template.TEMPLATE_NAME + '</option>';
    });

    if (selectElement) {
        selectElement.html(options);
        // Select2 초기화
        selectElement.select2({
            placeholder: '템플릿을 선택하세요',
            allowClear: true,
            width: '100%',
            language: {
                noResults: function() {
                    return "검색 결과가 없습니다.";
                },
                searching: function() {
                    return "검색 중...";
                }
            }
        });
    }
}

function getChartConfigData() {
    var chartData = {
        charts: []
    };
    
    $('.chart-config-item').each(function(index) {
        var $row = $(this);
        var templateId = $row.find('.template-select2').val();
        var chartType = $row.find('.chart-type-select').val();
        
        if (templateId && chartType) {
            var chart = {
                templateId: templateId,
                chartType: chartType,
                order: index + 1
            };
            chartData.charts.push(chart);
        }
    });
    
    return chartData;
}

function saveChartConfig() {
    var chartData = getChartConfigData();
    
    $.ajax({
        url: '/SystemConfig/saveChartConfig',
        type: 'POST',
        data: { chartConfig: JSON.stringify(chartData) },
        success: function(response) {
            if (response.success) {
                showAlert('차트 설정이 저장되었습니다.', 'success');
            } else {
                showAlert('차트 설정 저장에 실패했습니다: ' + response.message, 'danger');
            }
        },
        error: function() {
            showAlert('차트 설정 저장 중 오류가 발생했습니다.', 'danger');
        }
    });
}

function resetChartErrors() {
    if (confirm('모든 차트의 에러 상태를 리셋하시겠습니까?\n\n이 작업은 에러로 인해 중단된 차트들을 즉시 재시작합니다.')) {
        $.ajax({
            url: '/SystemConfig/resetChartErrors',
            type: 'POST',
            success: function(response) {
                if (response.success) {
                    showAlert('차트 에러 상태가 리셋되었습니다. 차트들이 즉시 재시작됩니다.', 'success');
                } else {
                    showAlert('에러 상태 리셋에 실패했습니다: ' + response.message, 'danger');
                }
            },
            error: function() {
                showAlert('에러 상태 리셋 중 오류가 발생했습니다.', 'danger');
            }
        });
    }
}

// 페이지 로드 시 템플릿 목록 캐시 및 기존 차트 설정 로드
$(document).ready(function() {
    // 템플릿 목록 로드
    $.ajax({
        type: 'GET',
        url: '/SQLTemplate/list',
        success: function(result) {
            if (result.success) {
                templateListCache = result.data;
                // 모니터링 템플릿 Select2 초기화
                initializeMonitoringTemplateSelect();
                
                // 데이터베이스 연결 목록 로드 (템플릿 목록 로드 후)
                loadConnectionList(function() {
                    // 연결 목록 로드 완료 후 기존 모니터링 템플릿 설정 로드
                    loadExistingMonitoringTemplateConfig();
                });
            }
        },
        error: function() {
            console.error('템플릿 목록 로드 실패');
        }
    });
    
    // 기존 차트 설정 로드
    loadExistingChartConfig();
});

// 기존 차트 설정 로드
function loadExistingChartConfig() {
    $.ajax({
        url: '/SystemConfig/getChartConfig',
        type: 'GET',
        success: function(response) {
            if (response.success && response.chartConfig) {
                try {
                    var config = JSON.parse(response.chartConfig);
                    if (config.charts && config.charts.length > 0) {
                        // 기존 차트 설정을 테이블에 추가
                        config.charts.forEach(function(chart, index) {
                            addChartConfigWithData(chart, index);
                        });
                    }
                } catch (e) {
                    console.error('차트 설정 파싱 실패:', e);
                }
            }
        },
        error: function() {
            console.error('차트 설정 로드 실패');
        }
    });
}

// 기존 데이터로 차트 설정 추가
function addChartConfigWithData(chartData, index) {
    // 차트 ID 생성: templateId__chart_type__chartType 형식
    var templateId = chartData.templateId || '';
    var chartType = chartData.chartType || '';
    var chartId = (templateId && chartType) ? (templateId + '__chart_type__' + chartType) : ('chart_' + Date.now());
    
    var row = $('<tr class="chart-config-item" data-chart-id="' + chartId + '">' +
        '<td style="text-align: center;">' +
        '<div style="display: flex; flex-direction: column; align-items: center;">' +
        '<button type="button" class="btn btn-xs btn-default move-up" title="위로" onclick="moveChartUp(\'' + chartId + '\')">' +
        '<i class="fa fa-chevron-up"></i>' +
        '</button>' +
        '<button type="button" class="btn btn-xs btn-default move-down" title="아래로" onclick="moveChartDown(\'' + chartId + '\')">' +
        '<i class="fa fa-chevron-down"></i>' +
        '</button>' +
        '</div>' +
        '</td>' +
        '<td>' +
        '<select class="form-control template-select2">' +
        '<option value="">템플릿을 선택하세요</option>' +
        '</select>' +
        '</td>' +
        '<td>' +
        '<select class="form-control chart-type-select">' +
        '<option value="">차트 타입을 선택하세요</option>' +
        '<option value="doughnut">도넛차트</option>' +
        '<option value="text">텍스트</option>' +
        '<option value="gauge">게이지</option>' +
        '<option value="bar">가로막대</option>' +
        '</select>' +
        '</td>' +
        '<td style="text-align: center;">' +
        '<button type="button" class="btn btn-danger btn-xs parameter-delete-btn" onclick="removeChartConfig(this)">' +
        '<i class="fa fa-minus"></i>' +
        '</button>' +
        '</td>' +
        '</tr>');
    
    $('#chartConfigContainer').append(row);
    
    // 템플릿 선택
    var templateSelect = row.find('.template-select2');
    loadTemplateOptions(templateSelect, chartData.templateId);
    
    // 차트 타입 선택
    var chartTypeSelect = row.find('.chart-type-select');
    chartTypeSelect.val(chartData.chartType || '');
}

// 모니터링 템플릿 설정 관련 함수들
function initializeMonitoringTemplateSelect() {
    var $select = $('#monitoringTemplateSelect');
    
    // Select2 초기화
    $select.select2({
        placeholder: '템플릿을 선택하세요',
        allowClear: true,
        width: '100%',
        language: {
            noResults: function() {
                return "검색 결과가 없습니다.";
            },
            searching: function() {
                return "검색 중...";
            }
        }
    });
    
    // 옵션 추가
    var options = '<option value="">템플릿을 선택하세요</option>';
    templateListCache.forEach(function(template) {
        options += '<option value="' + template.TEMPLATE_ID + '">' + template.TEMPLATE_NAME + '</option>';
    });
    $select.html(options);
}

function loadConnectionList(callback) {
    $.ajax({
        type: 'GET',
        url: '/SQLTemplate/db-connections',
        data: { templateType: 'SQL' },
        success: function(result) {
            if (result.success && result.data) {
                var $select = $('#monitoringConnectionSelect');
                var options = '<option value="">연결을 선택하세요</option>';
                result.data.forEach(function(conn) {
                    options += '<option value="' + conn.CONNECTION_ID + '">' + conn.CONNECTION_ID + '</option>';
                });
                $select.html(options);
            }
            if (callback && typeof callback === 'function') {
                callback();
            }
        },
        error: function() {
            console.error('연결 목록 로드 실패');
            if (callback && typeof callback === 'function') {
                callback();
            }
        }
    });
}

function loadExistingMonitoringTemplateConfig() {
    $.ajax({
        url: '/SystemConfig/getMonitoringTemplateConfig',
        type: 'GET',
        success: function(response) {
            if (response.success && response.monitoringConfig) {
                try {
                    var config = JSON.parse(response.monitoringConfig);
                    
                    // 템플릿 선택
                    if (config.templateId) {
                        var $templateSelect = $('#monitoringTemplateSelect');
                        // Select2가 이미 초기화되어 있는지 확인
                        if ($templateSelect.data('select2')) {
                            $templateSelect.val(config.templateId).trigger('change');
                        } else {
                            // Select2 초기화가 안 되어 있으면 잠시 후 다시 시도
                            setTimeout(function() {
                                $templateSelect.val(config.templateId).trigger('change');
                            }, 100);
                        }
                    }
                    
                    // 연결 선택
                    if (config.connectionId) {
                        var $connectionSelect = $('#monitoringConnectionSelect');
                        // 옵션이 로드되었는지 확인
                        if ($connectionSelect.find('option[value="' + config.connectionId + '"]').length > 0) {
                            $connectionSelect.val(config.connectionId);
                        } else {
                            // 옵션이 아직 로드되지 않았으면 잠시 후 다시 시도
                            setTimeout(function() {
                                $connectionSelect.val(config.connectionId);
                            }, 200);
                        }
                    }
                } catch (e) {
                    console.error('모니터링 템플릿 설정 파싱 실패:', e);
                }
            }
        },
        error: function() {
            console.error('모니터링 템플릿 설정 로드 실패');
        }
    });
}

function saveMonitoringTemplateConfig() {
    var templateId = $('#monitoringTemplateSelect').val();
    var connectionId = $('#monitoringConnectionSelect').val();
    
    if (!templateId || !connectionId) {
        showAlert('템플릿과 연결을 모두 선택해주세요.', 'danger');
        return;
    }
    
    // 템플릿명 조회
    var templateName = '';
    templateListCache.forEach(function(template) {
        if (template.TEMPLATE_ID === templateId) {
            templateName = template.TEMPLATE_NAME;
        }
    });
    
    var config = {
        templateId: templateId,
        templateName: templateName,
        connectionId: connectionId
    };
    
    $.ajax({
        url: '/SystemConfig/saveMonitoringTemplateConfig',
        type: 'POST',
        data: { monitoringConfig: JSON.stringify(config) },
        success: function(response) {
            if (response.success) {
                showAlert('모니터링 템플릿 설정이 저장되었습니다.', 'success');
            } else {
                showAlert('모니터링 템플릿 설정 저장에 실패했습니다: ' + response.message, 'danger');
            }
        },
        error: function() {
            showAlert('모니터링 템플릿 설정 저장 중 오류가 발생했습니다.', 'danger');
        }
    });
}

function deleteMonitoringTemplateConfig() {
    if (!confirm('모니터링 템플릿 설정을 삭제하시겠습니까?\n\n삭제 후 대시보드에서 모니터링 템플릿 영역이 표시되지 않습니다.')) {
        return;
    }
    
    $.ajax({
        url: '/SystemConfig/deleteMonitoringTemplateConfig',
        type: 'POST',
        success: function(response) {
            if (response.success) {
                $('#monitoringTemplateSelect').val('').trigger('change');
                $('#monitoringConnectionSelect').val('');
                showAlert('모니터링 템플릿 설정이 삭제되었습니다.', 'success');
            } else {
                showAlert('모니터링 템플릿 설정 삭제에 실패했습니다: ' + response.message, 'danger');
            }
        },
        error: function() {
            showAlert('모니터링 템플릿 설정 삭제 중 오류가 발생했습니다.', 'danger');
        }
    });
}
</script>
