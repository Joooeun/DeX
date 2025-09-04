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
    // 폼 제출 처리
    $('#configForm').on('submit', function(e) {
        e.preventDefault();
        
        var formData = {
            sessionTimeout: $('#sessionTimeout').val(),
            downloadIpPattern: $('#downloadIpPattern').val(),
            noticeContent: $('#noticeContent').val(),
            noticeEnabled: $('#noticeEnabled').is(':checked')
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
</script>
