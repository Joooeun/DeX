<%@include file="common/common.jsp"%>

<!-- Toast 알림 컨테이너 -->
<div id="toastContainer" style="position: fixed; top: 20px; right: 20px; z-index: 9999; width: 350px;"></div>

<div class="content-wrapper" style="margin-left: 0">
    <section class="content-header">
        <h1>사용자 관리</h1>
    </section>

    <section class="content">
        <div class="row">
            <div class="col-md-12">
                <div class="box">
                    <div class="box-header with-border">
                        <h3 class="box-title">사용자 목록</h3>
                       
                        <div class="row" style="margin-top: 10px;">
                            <div class="col-sm-3">
                                <div class="input-group input-group-sm">
                                    <input type="text" class="form-control" id="searchKeyword" placeholder="ID/이름">
                                    <span class="input-group-btn">
                                        <button type="button" class="btn btn-default" onclick="searchUsers()">
                                            <i class="fa fa-search"></i>
                                        </button>
                                    </span>
                                </div>
                            </div>
                            <div class="col-sm-3">
                                <div class="input-group input-group-sm">
                                    <span class="input-group-addon">그룹</span>
                                    <select class="form-control" id="groupFilter" onchange="filterByGroup()">
                                        <option value="">전체 그룹</option>
                                    </select>
                                </div>
                            </div>
                            <div class="col-sm-3">
                                <div class="input-group input-group-sm">
                                    <span class="input-group-addon">상태</span>
                                    <select class="form-control" id="statusFilter" onchange="filterByStatus()">
                                        <option value="ALL">전체</option>
                                        <option value="ACTIVE">활성</option>
                                        <option value="INACTIVE">비활성</option>
                                        <option value="LOCKED">잠금</option>
                                        <option value="EXPIRED">기한 만료</option>
                                    </select>
                                </div>
                            </div>
                            <div class="col-sm-3">
								<button type="button" class="btn pull-right btn-primary btn-sm"
									onclick="showCreateUserModal()">
									<i class="fa fa-plus"></i> 새 사용자
								</button>
							</div>
                        </div>
                    </div>
                    <div class="box-body">
                        <table id="userTable" class="table table-bordered table-hover table-striped">
                            <thead>
                                <tr>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자의 고유 식별자입니다. 로그인 시 사용되며, 중복되지 않습니다.">사용자 ID</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자의 실제 이름입니다. 화면에 표시되는 이름으로 사용됩니다.">이름</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자 계정의 현재 상태입니다. 활성: 정상 사용, 비활성: 로그인 불가, 잠금: 일시 제한">상태</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자가 속한 그룹입니다. 그룹별로 접근 권한이 설정됩니다.">그룹</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자의 로그인 허용 IP 주소입니다. 비워두면 모든 IP에서 로그인 가능합니다.">IP 제한</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자가 마지막으로 로그인한 시간입니다. 보안 모니터링에 활용됩니다.">마지막 로그인</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="연속 로그인 실패 횟수입니다. 일정 횟수 초과 시 계정이 잠길 수 있습니다.">로그인 실패</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자 계정이 생성된 날짜와 시간입니다.">생성일</div></th>
                                    <th><div data-toggle="tooltip" data-placement="top" title="사용자 수정, 삭제, 활동 로그 조회 등의 관리 작업을 수행할 수 있습니다.">관리</div></th>
                                </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                        
                        <!-- 페이징 컨트롤 -->
                        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-top: 10px">
                            <div style="flex: 1; text-align: center;">
                                <ul class="pagination" id="pagination" style="margin: 0; display: inline-block;">
                                </ul>
                                <div class="pagination-info">
                                    <span id="paginationInfo"></span>
                                </div>
                            </div>
                            <div style="margin-left: 15px;" class="input-group-sm">
                                <select id="pageSizeSelect" class="form-control" style="display: inline-block; width: auto;">
                                    <option value="5">5줄 보기</option>
                                    <option value="10">10줄 보기</option>
                                    <option value="20">20줄 보기</option>
                                    <option value="30">30줄 보기</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>


    <!-- 그룹 관리 섹션 -->
     <section class="content">
        <div class="row">
            <div class="col-md-12">
                <div class="box box-primary">
                    <div class="box-header with-border">
                        <h3 class="box-title">그룹 관리</h3>
                        <div class="box-tools float-end">
                            <button type="button" class="btn btn-primary btn-sm" onclick="showGroupModal()">
                                <i class="fa fa-plus"></i> 그룹 추가
                            </button>
                        </div>
                    </div>
                    <div class="box-body">
                        <div class="table-responsive">
                            <table class="table table-bordered table-hover table-striped" id="groupTable">
                                <thead>
                                    <tr>
                                        <th><div data-toggle="tooltip" data-placement="top" title="그룹의 고유 이름입니다. 사용자 권한 관리의 기본 단위로 사용되며, 중복되지 않아야 합니다.">그룹명</div></th>
                                        <th><div data-toggle="tooltip" data-placement="top" title="그룹에 대한 설명을 입력합니다. 그룹의 용도와 권한 범위를 명확하게 작성해주세요.">설명</div></th>
                                        <th><div data-toggle="tooltip" data-placement="top" title="그룹의 활성화 상태를 표시합니다. 활성: 정상 사용 가능, 비활성: 그룹 멤버 로그인 불가">상태</div></th>
                                        <th><div data-toggle="tooltip" data-placement="top" title="해당 그룹에 속한 사용자의 수를 표시합니다. 그룹 멤버 관리에서 확인할 수 있습니다.">멤버 수</div></th>
                                        <th><div data-toggle="tooltip" data-placement="top" title="그룹이 생성된 날짜와 시간을 표시합니다.">생성일</div></th>
                                        <th><div data-toggle="tooltip" data-placement="top" title="그룹 수정, 삭제 등의 관리 작업을 수행할 수 있습니다.">관리</div></th>
                                    </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>

     </section>

</div>

<!-- 사용자 생성/수정 모달 -->
<div class="modal fade" id="userModal" tabindex="-1" aria-labelledby="userModalTitle" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">&times;</button>
                <h4 class="modal-title" id="userModalTitle">사용자 생성</h4>
            </div>
            <div class="modal-body">
                <form id="userForm">
                    <input type="hidden" id="editUserId">
                    <div class="form-group">
                        <label for="userId" data-toggle="tooltip" data-placement="top" title="사용자의 고유 식별자입니다. 영문, 숫자, 언더스코어만 사용 가능하며, 중복되지 않아야 합니다.">사용자 ID</label>
                        <input type="text" class="form-control" id="userId" required>
                    </div>
                    <div class="form-group">
                        <label for="userName" data-toggle="tooltip" data-placement="top" title="사용자의 실제 이름을 입력합니다. 화면에 표시되는 이름으로 사용되며, 한글, 영문 모두 사용 가능합니다.">이름</label>
                        <input type="text" class="form-control" id="userName" required>
                    </div>
                    <div class="form-group">
                        <label for="password" data-toggle="tooltip" data-placement="top" title="사용자의 로그인 비밀번호를 입력합니다. 수정 시 비워두면 기존 비밀번호가 유지되며, 보안을 위해 암호화되어 저장됩니다.">비밀번호</label>
                        <input type="password" class="form-control" id="password">
                        <small class="text-muted" id="passwordDescription" style="display: none;">
                            <span class="text-warning">⚠️ 비밀번호를 입력하면 임시 비밀번호로 설정되며 다음 로그인 시 비밀번호 변경이 강제됩니다.</span>
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="groupId" data-toggle="tooltip" data-placement="top" 
                            title="사용자가 속할 그룹을 선택합니다. 여러 그룹을 선택할 수 있으며, 선택된 모든 그룹의 권한을 합집합으로 사용합니다. 그룹별로 접근 권한과 연결 권한이 설정되며, 사용자의 역할을 결정합니다.">
                            그룹 <span class="text-danger">*</span>
                        </label>
                        <select class="form-control" id="groupId" multiple required>
                            <!-- 그룹 목록이 동적으로 로드됩니다 -->
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="ipRestriction" data-toggle="tooltip" data-placement="top" title="사용자의 로그인을 허용할 IP 주소를 설정합니다. 여러 IP는 쉼표로 구분하고, 와일드카드(*) 사용 가능합니다. 비워두면 모든 IP에서 로그인 가능합니다.">로그인 IP 대역</label>
                        <input type="text" class="form-control" id="ipRestriction" placeholder="예: 192.168.1.*, 10.0.0.100">
                        <small class="text-muted">
                            <span class="text-info">💡 비워두면 모든 IP에서 로그인 가능합니다.</span>
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="excelDownloadIpPattern" data-toggle="tooltip" data-placement="top" title="엑셀 다운로드를 허용할 IP 주소 패턴을 설정합니다. 와일드카드(*) 사용 가능합니다. 비워두면 모든 IP에서 엑셀 다운로드 가능합니다.">엑셀 다운로드 IP 대역</label>
                        <input type="text" class="form-control" id="excelDownloadIpPattern" placeholder="예: 10.240.13.* 또는 *">
                        <small class="text-muted">
                            <span class="text-info">💡 비워두면 모든 IP에서 엑셀 다운로드 가능합니다.</span>
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="status" data-toggle="tooltip" data-placement="top" title="사용자의 계정 상태를 설정합니다. 활성: 정상 사용 가능, 비활성: 로그인 불가, 잠금: 일시적 접근 제한">상태</label>
                        <select class="form-control" id="status">
                            <option value="ACTIVE">활성</option>
                            <option value="INACTIVE">비활성</option>
                            <option value="LOCKED">잠금</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="accountPeriodEnabled" onchange="toggleAccountPeriod()"> 
                            기간제한
                        </label>
                        <small class="text-muted" style="display: block; margin-top: 5px;">
                            <span class="text-info">💡 체크하면 사용 기간을 설정할 수 있습니다. 체크하지 않으면 기간 제한이 없습니다.</span>
                        </small>
                    </div>
                    <div id="accountPeriodFields" style="display: none;">
                        <div class="row">
                            <div class="col-sm-6">
                                <div class="form-group">
                                    <label for="accountStartDate">시작일</label>
                                    <input type="date" class="form-control" id="accountStartDate">
                                </div>
                            </div>
                            <div class="col-sm-6">
                                <div class="form-group">
                                    <label for="accountEndDate">종료일</label>
                                    <input type="date" class="form-control" id="accountEndDate">
                                </div>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" onclick="saveUser()">저장</button>
            </div>
        </div>
    </div>
</div>

<!-- 사용자 활동 로그 모달 -->
<div class="modal fade" id="activityLogModal" tabindex="-1" aria-labelledby="activityLogModalTitle" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">&times;</button>
                <h4 class="modal-title">사용자 활동 로그</h4>
            </div>
            <div class="modal-body">
                <input type="hidden" id="logUserId">
                <div class="form-group">
                    <label for="logDateRange">기간 선택</label>
                    <select class="form-control" id="logDateRange" onchange="loadActivityLogs()">
                        <option value="7">최근 7일</option>
                        <option value="30">최근 30일</option>
                        <option value="90">최근 90일</option>
                        <option value="all">전체</option>
                    </select>
                </div>
                <div class="table-responsive">
                    <table id="activityLogTable" class="table table-bordered table-striped">
                        <thead>
                            <tr>
                                <th>시간</th>
                                <th>활동</th>
                                <th>IP 주소</th>
                                <th>상세 정보</th>
                            </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">닫기</button>
            </div>
        </div>
    </div>
</div>



<!-- 그룹 관리 모달 -->
<div class="modal fade" id="groupModal" tabindex="-1" aria-labelledby="groupModalTitle" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">&times;</button>
                <h4 class="modal-title" id="groupModalTitle">그룹 추가</h4>
            </div>
            <div class="modal-body">
                <!-- 탭 네비게이션 -->
                <ul class="nav nav-tabs" id="groupModalTabs" role="tablist">
                    <li class="active" role="presentation">
                        <a href="#groupInfoTab" id="groupInfoTab-tab" data-toggle="tab" role="tab" aria-controls="groupInfoTab" aria-selected="true">그룹 정보</a>
                    </li>
                    <li role="presentation">
                        <a href="#groupPermissionsTab" id="groupPermissionsTab-tab" data-toggle="tab" role="tab" aria-controls="groupPermissionsTab" aria-selected="false">권한 관리</a>
                    </li>
                </ul>
                
                <!-- 탭 콘텐츠 -->
                <div class="tab-content">
                    <!-- 그룹 정보 탭 -->
                    <div class="tab-pane active" id="groupInfoTab">
                        <form id="groupForm">
                            <input type="hidden" id="editGroupId">
                            <div class="form-group">
                                <label for="groupName">그룹명 *</label>
                                <input type="text" class="form-control" id="groupName" required>
                            </div>
                            <div class="form-group">
                                <label for="groupDescription">설명</label>
                                <textarea class="form-control" id="groupDescription" rows="3"></textarea>
                            </div>
                            <div class="form-group">
                                <label for="groupStatus">상태</label>
                                <select class="form-control" id="groupStatus">
                                    <option value="ACTIVE">활성</option>
                                    <option value="INACTIVE">비활성</option>
                                </select>
                            </div>
                        </form>
                    </div>
                    
                    <!-- 권한 관리 탭 -->
                    <div class="tab-pane" id="groupPermissionsTab">
                        <div class="row">
                            <div class="col-md-4">
                                <h5>메뉴권한</h5>
                                <div class="permission-section" id="groupMenuPermissions">
                                    <!-- 메뉴 권한이 여기에 로드됩니다 -->
                                </div>
                            </div>
                            <div class="col-md-4">
                                <h5>SQL 템플릿 카테고리 권한</h5>
                                <div class="permission-section" id="groupSqlTemplatePermissions">
                                    <!-- SQL 템플릿 카테고리 권한이 여기에 로드됩니다 -->
                                </div>
                            </div>
                            <div class="col-md-4">
                                <h5>연결 정보 권한</h5>
                                <div class="permission-section" id="groupConnectionPermissions">
                                    <!-- 연결 정보 권한이 여기에 로드됩니다 -->
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" onclick="saveGroup()">저장</button>
            </div>
        </div>
    </div>
</div>

<style>
.permission-section {
    max-height: 80vh;
    overflow-y: auto;
    border: 1px solid #ddd;
    padding: 10px;
    border-radius: 4px;
}

.permission-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 5px 0;
    border-bottom: 1px solid #eee;
}

.permission-item:last-child {
    border-bottom: none;
}

.permission-checkbox {
    margin-right: 10px;
}

.permission-category {
    margin-bottom: 15px;
    padding: 10px;
    background-color: #f9f9f9;
    border-radius: 4px;
    border-left: 3px solid #337ab7;
}

.permission-category h6 {
    margin: 0 0 10px 0;
    color: #337ab7;
    font-size: 14px;
}

.permission-category .permission-item {
    margin-left: 10px;
    padding: 3px 0;
}
</style>

<script>
// Select 옵션 렌더링 함수 (템플릿 관리와 동일)
function renderSelectOptions(config) {
    // 기본 설정
    var defaults = {
        valueField: 'id',
        textField: 'name',
        placeholder: '선택하세요',
        allowClear: true,
        width: '100%',
        initSelect2: true
    };
    
    // 설정 병합
    var options = Object.assign({}, defaults, config);
    
    // Select 요소 비우기
    options.select.empty();
    
    // 데이터가 있는 경우 옵션 추가
    if (options.data && options.data.length > 0) {
        options.data.forEach(function(item) {
            var value = item[options.valueField];
            var text;
            
            // textField가 함수인 경우와 문자열인 경우 처리
            if (typeof options.textField === 'function') {
                text = options.textField(item);
            } else {
                text = item[options.textField];
            }
            
            var option = $('<option value="' + value + '">' + text + '</option>');
            options.select.append(option);
        });
    }
    
    // Select2 초기화
    if (options.initSelect2) {
        options.select.select2({
            placeholder: options.placeholder,
            allowClear: options.allowClear,
            width: options.width
        });
    }
}

$(document).ready(function() {
    loadUserList();
    loadGroupList();
    loadGroupTable();
    loadGroupFilter();
    
    // 검색 필드에서 Enter 키 이벤트 처리
    $('#searchKeyword').on('keypress', function(e) {
        if (e.which === 13) { // Enter 키
            searchUsers();
        }
    });
    
    // 실시간 검색 (타이핑 후 500ms 대기)
    var searchTimeout;
    $('#searchKeyword').on('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(function() {
            currentPage = 1; // 검색 시 첫 페이지로 이동
            searchUsers();
        }, 500);
    });
});

// Toast 알림 시스템
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
    
    var toast = $('<div id="' + toastId + '" class="alert ' + bgClass + ' alert-dismissible" style="margin-bottom: 10px; animation: slideInDown 0.3s ease-out;">' +
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

// 전역 변수로 현재 페이지 및 페이지 사이즈 관리
var currentPage = 1;
var currentPageSize = 5;

// 페이지 사이즈 변경 이벤트
$(document).ready(function() {
    // localStorage에서 페이지 사이즈 로드
    try {
        var savedPageSize = localStorage.getItem('userPageSize');
        if (savedPageSize) {
            currentPageSize = parseInt(savedPageSize);
        }
    } catch (e) {
        console.error('페이지 사이즈 로드 실패:', e);
    }
    
    // 초기 페이지 사이즈 설정
    $('#pageSizeSelect').val(currentPageSize);
    
    // 페이지 사이즈 변경 시
    $('#pageSizeSelect').on('change', function() {
        currentPageSize = parseInt($(this).val());
        try {
            localStorage.setItem('userPageSize', currentPageSize);
        } catch (e) {
            console.error('페이지 사이즈 저장 실패:', e);
        }
        currentPage = 1; // 페이지 사이즈 변경 시 첫 페이지로 이동
        loadUserList();
    });
});

// 사용자 목록 로드
function loadUserList(page) {
    if (page) {
        currentPage = page;
    }
    
    var searchKeyword = $('#searchKeyword').val();
    var groupFilter = $('#groupFilter').val();
    var statusFilter = $('#statusFilter').val();
    
    $.ajax({
        url: '/User/list',
        type: 'GET',
        data: { 
            searchKeyword: searchKeyword,
            groupFilter: groupFilter,
            statusFilter: statusFilter,
            page: currentPage,
            pageSize: currentPageSize
        },
        success: function(response) {
            if (response.success) {
                displayUserList(response.data);
                displayPagination(response.pagination);
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('사용자 목록 조회 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 사용자 검색
function searchUsers() {
    currentPage = 1; // 검색 시 첫 페이지로 이동
    loadUserList();
}

// 그룹별 필터링
function filterByGroup() {
    currentPage = 1;
    loadUserList();
}

// 상태별 필터링
function filterByStatus() {
    currentPage = 1;
    loadUserList();
}

// 페이징 UI 표시
function displayPagination(pagination) {
    // 현재 페이지 사이즈를 셀렉트 박스에 반영
    if (pagination.pageSize) {
        $('#pageSizeSelect').val(pagination.pageSize);
        currentPageSize = pagination.pageSize;
        // localStorage에 저장
        try {
            localStorage.setItem('userPageSize', currentPageSize);
        } catch (e) {
            console.error('페이지 사이즈 저장 실패:', e);
        }
    }
    
    var paginationContainer = $('#pagination');
    var paginationInfo = $('#paginationInfo');
    
    paginationContainer.empty();
    
    var currentPage = pagination.currentPage;
    var totalPages = pagination.totalPages;
    var totalCount = pagination.totalCount;
    var pageSize = pagination.pageSize;
    
    // 페이징 정보 표시
    var startItem = (currentPage - 1) * pageSize + 1;
    var endItem = Math.min(currentPage * pageSize, totalCount);
    paginationInfo.text('전체 ' + totalCount + '개 중 ' + startItem + '-' + endItem + '개 표시');
    
    if (totalPages <= 1) {
        return; // 페이지가 1개 이하면 페이징 버튼 숨김
    }
    
    // 이전 페이지 버튼
    if (currentPage > 1) {
        paginationContainer.append('<li><a href="#" onclick="loadUserList(' + (currentPage - 1) + ')">&laquo;</a></li>');
    } else {
        paginationContainer.append('<li class="disabled"><a href="#">&laquo;</a></li>');
    }
    
    // 페이지 번호 버튼들
    // 최대 5개 페이지를 보여주되, 항상 첫 페이지와 마지막 페이지를 포함
    var startPage, endPage;
    
    if (totalPages <= 5) {
        // 전체 페이지가 5개 이하면 모두 표시
        startPage = 1;
        endPage = totalPages;
    } else {
        // 현재 페이지를 중심으로 표시하되, 첫 페이지와 마지막 페이지를 포함
        if (currentPage <= 3) {
            // 앞쪽에 있을 때: 1, 2, 3, 4, ... totalPages
            startPage = 1;
            endPage = 4;
        } else if (currentPage >= totalPages - 2) {
            // 뒤쪽에 있을 때: 1, ..., totalPages-3, totalPages-2, totalPages-1, totalPages
            startPage = totalPages - 3;
            endPage = totalPages;
        } else {
            // 중간에 있을 때: 1, ..., currentPage-1, currentPage, currentPage+1, ..., totalPages
            startPage = currentPage - 1;
            endPage = currentPage + 1;
        }
    }
    
    // 첫 페이지 표시
    if (startPage > 1) {
        paginationContainer.append('<li><a href="#" onclick="loadUserList(1)">1</a></li>');
        if (startPage > 2) {
            paginationContainer.append('<li class="disabled"><a href="#">...</a></li>');
        }
    }
    
    // 페이지 번호 버튼들
    for (var i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            paginationContainer.append('<li class="active"><a href="#">' + i + '</a></li>');
        } else {
            paginationContainer.append('<li><a href="#" onclick="loadUserList(' + i + ')">' + i + '</a></li>');
        }
    }
    
    // 마지막 페이지 표시
    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            paginationContainer.append('<li class="disabled"><a href="#">...</a></li>');
        }
        paginationContainer.append('<li><a href="#" onclick="loadUserList(' + totalPages + ')">' + totalPages + '</a></li>');
    }
    
    // 다음 페이지 버튼
    if (currentPage < totalPages) {
        paginationContainer.append('<li><a href="#" onclick="loadUserList(' + (currentPage + 1) + ')">&raquo;</a></li>');
    } else {
        paginationContainer.append('<li class="disabled"><a href="#">&raquo;</a></li>');
    }
}

// 사용자 목록 표시
function displayUserList(userList) {
    var tbody = $('#userTable tbody');
    tbody.empty();
    
    userList.forEach(function(user) {
        var row = '<tr>' +
            '<td>' + user.USER_ID + '</td>' +
            '<td>' + user.USER_NAME + '</td>' +
            '<td>' + getStatusBadge(user.STATUS) + '</td>' +
            '<td>' + (user.GROUP_NAME || '-') + '</td>' +
            '<td>' + (user.IP_RESTRICTION || '-') + '</td>' +
            '<td>' + formatDate(user.LAST_LOGIN_TIMESTAMP) + '</td>' +
            '<td>' + (user.LOGIN_FAIL_COUNT || 0) + '</td>' +
            '<td>' + formatDate(user.CREATED_TIMESTAMP) + '</td>' +
            '<td>' +
                '<button class="btn btn-sm btn-info" onclick="editUser(\'' + user.USER_ID + '\')">수정</button> ' +
                '<button class="btn btn-sm btn-primary" onclick="showActivityLogModal(\'' + user.USER_ID + '\')">로그</button> ' +
                '<button class="btn btn-sm btn-danger" onclick="deleteUser(\'' + user.USER_ID + '\')">삭제</button>' +
            '</td>' +
            '</tr>';
        tbody.append(row);
    });
}

// 상태 배지 생성
function getStatusBadge(status) {
    var badgeClass = 'label-default';
    var statusText = '알 수 없음';
    
    switch(status) {
        case 'ACTIVE':
            badgeClass = 'label-success';
            statusText = '활성';
            break;
        case 'INACTIVE':
            badgeClass = 'label-warning';
            statusText = '비활성';
            break;
        case 'LOCKED':
            badgeClass = 'label-danger';
            statusText = '잠금';
            break;
        case 'EXPIRED':
            badgeClass = 'label-danger';
            statusText = '기한 만료';
            break;
    }
    
    return '<span class="label ' + badgeClass + '">' + statusText + '</span>';
}

// 날짜 포맷
function formatDate(dateStr) {
    if (!dateStr) return '-';
    
    // 13자리 숫자(밀리초 타임스탬프)인지 확인
    if (typeof dateStr === 'number' || (typeof dateStr === 'string' && /^\d{13}$/.test(dateStr))) {
        // 13자리 타임스탬프를 Date 객체로 변환
        return new Date(parseInt(dateStr)).toLocaleString('ko-KR');
    }
    
    // 일반 날짜 문자열 처리
    return new Date(dateStr).toLocaleString('ko-KR');
}

// 그룹 목록 로드 (템플릿 관리와 동일한 방식)
function loadGroupList() {
    $.ajax({
        url: '/User/groups',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                // renderSelectOptions 함수 사용 (Select2 자동 초기화)
                renderSelectOptions({
                    select: $('#groupId'),
                    data: response.data,
                    valueField: 'GROUP_ID',
                    textField: 'GROUP_NAME',
                    placeholder: '그룹을 선택하세요'
                });
            }
        }
    });
}

// 그룹 필터 로드
function loadGroupFilter() {
    $.ajax({
        url: '/User/groups',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                var select = $('#groupFilter');
                select.empty();
                select.append('<option value="">전체 그룹</option>');
                
                response.data.forEach(function(group) {
                    select.append('<option value="' + group.GROUP_ID + '">' + group.GROUP_NAME + '</option>');
                });
            }
        }
    });
}

// 그룹 목록과 필터 동시 업데이트
function updateGroupLists() {
    loadGroupList();
    loadGroupFilter();
}

// 새 사용자 모달 표시
function showCreateUserModal() {
    $('#userModalTitle').text('사용자 생성');
    $('#userForm')[0].reset();
    $('#editUserId').val('');
    $('#userId').prop('readonly', false);
    $('#password').attr('required', true);
    $('#passwordDescription').show();
    // 기간제한 초기화
    $('#accountPeriodEnabled').prop('checked', false);
    $('#accountPeriodFields').hide();
    $('#accountStartDate').val('');
    $('#accountEndDate').val('');
    // 그룹 선택 초기화 (Select2)
    if ($('#groupId').hasClass('select2-hidden-accessible')) {
        $('#groupId').val(null).trigger('change');
    } else {
        $('#groupId').val(null);
    }
    $('#userModal').modal('show');
}

// 사용자 수정 모달 표시
function editUser(userId) {
    $.ajax({
        url: '/User/detail',
        type: 'GET',
        data: { userId: userId },
        success: function(response) {
            if (response.success) {
                var user = response.data;
                $('#userModalTitle').text('사용자 관리');
                $('#editUserId').val(user.USER_ID);
                $('#userId').val(user.USER_ID).prop('readonly', true);
                $('#userName').val(user.USER_NAME);
                $('#status').val(user.STATUS);
                $('#ipRestriction').val(user.IP_RESTRICTION || '');
                $('#excelDownloadIpPattern').val(user.EXCEL_DOWNLOAD_IP_PATTERN || '');
                $('#password').attr('required', false);
                $('#passwordDescription').show();
                
                // 사용 기간 필드 설정
                if (user.ACCOUNT_START_DATE || user.ACCOUNT_END_DATE) {
                    $('#accountPeriodEnabled').prop('checked', true);
                    $('#accountPeriodFields').show();
                    if (user.ACCOUNT_START_DATE) {
                        // 날짜 형식 변환 (YYYY-MM-DD)
                        var startDate = new Date(user.ACCOUNT_START_DATE);
                        var startDateStr = startDate.getFullYear() + '-' + 
                            String(startDate.getMonth() + 1).padStart(2, '0') + '-' + 
                            String(startDate.getDate()).padStart(2, '0');
                        $('#accountStartDate').val(startDateStr);
                    } else {
                        $('#accountStartDate').val('');
                    }
                    if (user.ACCOUNT_END_DATE) {
                        // 날짜 형식 변환 (YYYY-MM-DD)
                        var endDate = new Date(user.ACCOUNT_END_DATE);
                        var endDateStr = endDate.getFullYear() + '-' + 
                            String(endDate.getMonth() + 1).padStart(2, '0') + '-' + 
                            String(endDate.getDate()).padStart(2, '0');
                        $('#accountEndDate').val(endDateStr);
                    } else {
                        $('#accountEndDate').val('');
                    }
                } else {
                    $('#accountPeriodEnabled').prop('checked', false);
                    $('#accountPeriodFields').hide();
                    $('#accountStartDate').val('');
                    $('#accountEndDate').val('');
                }
                
                // 사용자의 현재 그룹 정보 로드 (Select2 업데이트 포함)
                loadUserGroup(userId);
                
                $('#userModal').modal('show');
            } else {
                showToast(response.message, 'error');
            }
        }
    });
}

// 사용자의 현재 그룹 정보 로드 (다중 그룹 지원)
function loadUserGroup(userId) {
    $.ajax({
        url: '/User/currentGroups',
        type: 'GET',
        data: { userId: userId },
        success: function(response) {
            if (response.success && response.data && response.data.length > 0) {
                // 여러 그룹 ID를 배열로 변환하여 선택
                var groupIds = response.data.map(function(group) {
                    return group.GROUP_ID;
                });
                $('#groupId').val(groupIds).trigger('change'); // Select2 업데이트
            } else {
                $('#groupId').val([]).trigger('change'); // Select2 업데이트
            }
        },
        error: function() {
            $('#groupId').val([]).trigger('change'); // Select2 업데이트
        }
    });
}

// 기간제한 체크박스 토글
function toggleAccountPeriod() {
    var enabled = $('#accountPeriodEnabled').is(':checked');
    if (enabled) {
        $('#accountPeriodFields').show();
    } else {
        $('#accountPeriodFields').hide();
        $('#accountStartDate').val('');
        $('#accountEndDate').val('');
    }
}

// 사용자 저장
function saveUser() {
    var editUserId = $('#editUserId').val();
    var groupIds = $('#groupId').val(); // Select2 다중 선택이므로 배열 반환
    
    // 그룹 선택 필수 검증
    if (!groupIds || (Array.isArray(groupIds) && groupIds.length === 0) || (!Array.isArray(groupIds) && !groupIds)) {
        showToast('그룹을 최소 1개 이상 선택해주세요.', 'error');
        $('#groupId').focus();
        return;
    }
    
    // Select2에서 단일 값이 문자열로 반환되는 경우 배열로 변환
    if (!Array.isArray(groupIds)) {
        groupIds = [groupIds];
    }
    
    var userData = {
        userId: $('#userId').val(),
        userName: $('#userName').val(),
        status: $('#status').val(),
        groupIds: groupIds, // 여러 그룹 ID 배열
        ipRestriction: $('#ipRestriction').val(),
        excelDownloadIpPattern: $('#excelDownloadIpPattern').val()
    };
    
    var password = $('#password').val();
    if (password) {
        userData.password = password;
    }
    
    // 사용 기간 필드 처리
    if ($('#accountPeriodEnabled').is(':checked')) {
        userData.accountStartDate = $('#accountStartDate').val() || null;
        userData.accountEndDate = $('#accountEndDate').val() || null;
    } else {
        userData.accountStartDate = null;
        userData.accountEndDate = null;
    }
    
    var url = editUserId ? '/User/update?userId=' + editUserId : '/User/create';
    var method = editUserId ? 'POST' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(userData),
        success: function(response) {
            if (response.success) {
                showToast(response.message, 'success');
                // Bootstrap 3에서는 getInstance가 없으므로 직접 숨김
                // var userModal = bootstrap.Modal.getInstance(document.getElementById('userModal'));
                $('#userModal').modal('hide');
                loadUserList(currentPage);
                loadGroupTable(); // 그룹 테이블 새로고침 (멤버 수 변경 반영)
                updateGroupLists(); // 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('사용자 저장 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 사용자 삭제
function deleteUser(userId) {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    
    $.ajax({
        url: '/User/delete',
        type: 'POST',
        data: { userId: userId },
        success: function(response) {
            if (response.success) {
                showToast(response.message, 'success');
                loadUserList(currentPage);
                loadGroupTable(); // 그룹 테이블 새로고침 (멤버 수 변경 반영)
                updateGroupLists(); // 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('사용자 삭제 중 오류가 발생했습니다.', 'error');
        }
    });
}



// 사용자 권한 로드
function loadUserPermissions(userId) {
    // SQL 템플릿 권한 로드
    $.ajax({
        url: '/User/sqlTemplatePermissions',
        type: 'GET',
        data: { userId: userId },
        success: function(response) {
            if (response.success) {
                displaySqlTemplatePermissions(response.data);
            }
        }
    });
    
    // 연결 정보 권한 로드
    $.ajax({
        url: '/User/connectionPermissions',
        type: 'GET',
        data: { userId: userId },
        success: function(response) {
            if (response.success) {
                displayConnectionPermissions(response.data);
            }
        }
    });
}

// SQL 템플릿 권한 표시
function displaySqlTemplatePermissions(permissions) {
    var container = $('#sqlTemplatePermissions');
    container.empty();
    
    permissions.forEach(function(permission) {
        var item = '<div class="permission-item">' +
            '<div>' +
                '<input type="checkbox" class="permission-checkbox" id="sql_' + permission.TEMPLATE_ID + '" ' +
                (permission.HAS_PERMISSION ? 'checked' : '') + '>' +
                '<label for="sql_' + permission.TEMPLATE_ID + '">' + permission.TEMPLATE_NAME + '</label>' +
            '</div>' +
            '<small class="text-muted">' + permission.CATEGORY_PATH + '</small>' +
            '</div>';
        container.append(item);
    });
}

// 연결 정보 권한 표시
function displayConnectionPermissions(permissions) {
    var container = $('#connectionPermissions');
    container.empty();
    
    permissions.forEach(function(permission) {
        var item = '<div class="permission-item">' +
            '<div>' +
                '<input type="checkbox" class="permission-checkbox" id="conn_' + permission.CONNECTION_ID + '" ' +
                (permission.HAS_PERMISSION ? 'checked' : '') + '>' +
                '<label for="conn_' + permission.CONNECTION_ID + '">' + permission.CONNECTION_ID + '</label>' +
            '</div>' +
            '<small class="text-muted">' + permission.DB_TYPE + '</small>' +
            '</div>';
        container.append(item);
    });
}

// 권한 저장
function savePermissions() {
    var userId = $('#permissionUserId').val();
    var permissions = {
        sqlTemplatePermissions: [],
        connectionPermissions: []
    };
    
    // SQL 템플릿 권한 수집
    $('#sqlTemplatePermissions input[type="checkbox"]').each(function() {
        var templateId = $(this).attr('id').replace('sql_', '');
        permissions.sqlTemplatePermissions.push({
            templateId: templateId,
            hasPermission: $(this).is(':checked')
        });
    });
    
    // 연결 정보 권한 수집
    $('#connectionPermissions input[type="checkbox"]').each(function() {
        var connectionId = $(this).attr('id').replace('conn_', '');
        permissions.connectionPermissions.push({
            connectionId: connectionId,
            hasPermission: $(this).is(':checked')
        });
    });
    
    $.ajax({
        url: '/User/savePermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            userId: userId,
            permissions: permissions
        }),
        success: function(response) {
            if (response.success) {
                showToast('권한이 저장되었습니다.', 'success');
                // Bootstrap 3에서는 getInstance가 없으므로 직접 숨김
                // var permissionModal = bootstrap.Modal.getInstance(document.getElementById('permissionModal'));
                $('#permissionModal').modal('hide');
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('권한 저장 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 활동 로그 모달 표시
function showActivityLogModal(userId) {
    $('#logUserId').val(userId);
    $('#activityLogModal').modal('show');
    loadActivityLogs();
}

// 활동 로그 로드
function loadActivityLogs() {
    var userId = $('#logUserId').val();
    var dateRange = $('#logDateRange').val();
    
    $.ajax({
        url: '/User/activityLogs',
        type: 'GET',
        data: { 
            userId: userId,
            dateRange: dateRange
        },
        success: function(response) {
            if (response.success) {
                displayActivityLogs(response.data);
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('활동 로그 조회 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 활동 로그 표시
function displayActivityLogs(logs) {
    var tbody = $('#activityLogTable tbody');
    tbody.empty();
    
    logs.forEach(function(log) {
        var row = '<tr>' +
            '<td>' + formatDate(log.CREATED_TIMESTAMP) + '</td>' +
            '<td>' + log.ACTION_TYPE+' '+log.STATUS + '</td>' +
            '<td>' + (log.IP_ADDRESS || '-') + '</td>' +
            '<td>' + (log.ERROR_MESSAGE || '-') + '</td>' +
            '</tr>';
        tbody.append(row);
    });
}

// 그룹 목록 로드 (테이블용)
function loadGroupTable() {
    $.ajax({
        url: '/UserGroup/list',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                displayGroupTable(response.data);
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('그룹 목록 조회 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 그룹 테이블 표시
function displayGroupTable(groupList) {
    var tbody = $('#groupTable tbody');
    tbody.empty();
    
    groupList.forEach(function(group) {
        var row = '<tr>' +
            '<td>' + group.GROUP_NAME + '</td>' +
            '<td>' + (group.GROUP_DESCRIPTION || '-') + '</td>' +
            '<td>' + getGroupStatusBadge(group.STATUS) + '</td>' +
            '<td>' + (group.MEMBER_COUNT || 0) + '</td>' +
            '<td>' + formatDate(group.CREATED_TIMESTAMP) + '</td>' +
            '<td>' +
                '<button class="btn btn-sm btn-info" onclick="editGroup(\'' + group.GROUP_ID + '\')">수정</button> ' +
                '<button class="btn btn-sm btn-warning" onclick="copyGroup(\'' + group.GROUP_ID + '\')">복사</button> ' +
                '<button class="btn btn-sm btn-danger" onclick="deleteGroup(\'' + group.GROUP_ID + '\')">삭제</button>' +
            '</td>' +
            '</tr>';
        tbody.append(row);
    });
}

// 그룹 상태 배지 생성
function getGroupStatusBadge(status) {
    var badgeClass = 'label-default';
    var statusText = '알 수 없음';
    
    switch(status) {
        case 'ACTIVE':
            badgeClass = 'label-success';
            statusText = '활성';
            break;
        case 'INACTIVE':
            badgeClass = 'label-warning';
            statusText = '비활성';
            break;
    }
    
    return '<span class="label ' + badgeClass + '">' + statusText + '</span>';
}

// 그룹 모달 표시
function showGroupModal() {
    $('#groupModalTitle').text('그룹 추가');
    $('#groupForm')[0].reset();
    $('#editGroupId').val('');
    
    // 권한 목록 로드 (카테고리별로 표시)
    loadAllPermissions();
    
        // 첫 번째 탭으로 이동
    // Bootstrap 3 문법으로 변경
    // var firstTab = new bootstrap.Tab(document.querySelector('#groupInfoTab-tab'));
    $('#groupInfoTab-tab').tab('show');
    
    $('#groupModal').modal('show');
}

// 그룹 수정 모달 표시
function editGroup(groupId) {
    $.ajax({
        url: '/UserGroup/detail',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                var group = response.data;
                $('#groupModalTitle').text('그룹 관리');
                $('#editGroupId').val(group.GROUP_ID);
                $('#groupName').val(group.GROUP_NAME);
                $('#groupDescription').val(group.GROUP_DESCRIPTION);
                $('#groupStatus').val(group.STATUS);
                
                // 권한 정보 로드
                loadGroupPermissions(groupId);
                
                $('#groupModal').modal('show');
            } else {
                alert(response.message);
            }
        }
    });
}

// 그룹 복사 함수
function copyGroup(groupId) {
    $.ajax({
        url: '/UserGroup/detail',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                var group = response.data;
                
                // 그룹 이름에 "(복사)" 접미사 추가
                var newGroupName = generateCopyName(group.GROUP_NAME, '새 그룹 (복사)');
                
                // 그룹 ID 초기화 (새 그룹으로 만들기)
                $('#editGroupId').val('');
                
                // 그룹 정보 설정
                $('#groupModalTitle').text('그룹 추가');
                $('#groupName').val(newGroupName);
                $('#groupDescription').val(group.GROUP_DESCRIPTION);
                $('#groupStatus').val(group.STATUS);
                
                // 권한 정보 로드 (복사할 그룹의 권한)
                loadGroupPermissions(groupId);
                
                // 권한 목록 로드
                loadAllPermissions();
                
                // 첫 번째 탭으로 이동
                $('#groupInfoTab-tab').tab('show');
                
                $('#groupModal').modal('show');
                
                // 그룹 이름 필드에 포커스
                setTimeout(function() {
                    $('#groupName').focus();
                    $('#groupName').select();
                }, 300);
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('그룹 정보를 불러오는데 실패했습니다.');
        }
    });
}


// 그룹 저장
function saveGroup() {
    var editGroupId = $('#editGroupId').val();
    var groupData = {
        groupName: $('#groupName').val(),
        description: $('#groupDescription').val(),
        status: $('#groupStatus').val()
    };
    
    var url = editGroupId ? '/UserGroup/update?groupId=' + editGroupId : '/UserGroup/create';
    
    $.ajax({
        url: url,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(groupData),
        success: function(response) {
            if (response.success) {
                // 그룹 정보 저장 성공 후, 수정 모드인 경우 권한도 저장
                if (editGroupId) {
                    saveGroupPermissions(editGroupId);
                } else {
                    // 새 그룹 생성 시 권한도 함께 저장
                    saveGroupPermissions(response.data.groupId);
                }
                showToast(response.message, 'success');
                // Bootstrap 3에서는 getInstance가 없으므로 직접 숨김
                // var groupModal = bootstrap.Modal.getInstance(document.getElementById('groupModal'));
                $('#groupModal').modal('hide');
                loadGroupTable();
                loadUserList(currentPage); // 사용자 목록 새로고침 (그룹 정보 변경 반영)
                updateGroupLists(); // 사용자 모달의 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('그룹 저장 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 모든 권한 목록 로드 (단순화)
function loadAllPermissions() {
    // 메뉴 권한 목록 로드 (그룹 추가 모달용 - groupId 없음)
    $.ajax({
        url: '/UserGroup/menuPermissions',
        type: 'GET',
        data: { groupId: '' }, // 빈 문자열 전달하여 모든 메뉴 목록만 가져옴
        success: function(response) {
            console.log('메뉴 권한 로드 응답:', response);
            if (response.success && response.data) {
                var container = $('#groupMenuPermissions');
                container.empty();
                
                response.data.forEach(function(menu) {
                    var item = '<div class="permission-item">' +
                        '<label>' +
                        '<input type="checkbox" id="group_menu_' + menu.MENU_ID + '" class="permission-checkbox">' +
                        menu.MENU_NAME +
                        '</label>' +
                        '<small class="text-muted">' + (menu.MENU_DESCRIPTION || '') + '</small>' +
                        '</div>';
                    container.append(item);
                });
            } else {
                console.error('메뉴 권한 로드 실패:', response.message);
            }
        },
        error: function(xhr, status, error) {
            console.error('메뉴 권한 로드 중 오류:', error);
        }
    });
    
    // SQL 템플릿 카테고리 권한 목록 로드
    $.ajax({
        url: '/UserGroup/categories',
        type: 'GET',
        success: function(response) {
            if (response.success && response.data) {
                var container = $('#groupSqlTemplatePermissions');
                container.empty();
                
                response.data.forEach(function(category) {
                    var item = '<div class="permission-item">' +
                        '<label>' +
                        '<input type="checkbox" id="group_category_' + category.CATEGORY_ID + '" class="permission-checkbox">' +
                        category.CATEGORY_NAME + ' (' + category.CATEGORY_ID + ')' +
                        '</label>' +
                        //'<small class="text-muted">' + (category.CATEGORY_DESCRIPTION || '') + '</small>' +
                        '</div>';
                    container.append(item);
                });
            }
        },
        error: function() {
            console.error('카테고리 목록 로드 실패');
        }
    });
    
    // 연결 정보 권한 목록 로드
    $.ajax({
        url: '/Connection/list',
        type: 'GET',
        data: { page: 1, pageSize: 1000 },
        success: function(response) {
            if (response.success && response.data) {
                var container = $('#groupConnectionPermissions');
                container.empty();
                
                var connections = [];
                if (Array.isArray(response.data)) {
                    connections = response.data;
                } else if (response.data.databaseConnections) {
                    connections = response.data.databaseConnections;
                } else if (response.data.connections) {
                    connections = response.data.connections;
                }
                
                connections.forEach(function(conn) {
                    var connId = typeof conn === 'string' ? conn : conn.CONNECTION_ID;
                    var connName = typeof conn === 'string' ? conn : (conn.HOST_IP || conn.CONNECTION_ID);
                    var connType = typeof conn === 'string' ? '' : (conn.TYPE || '');
                    
                    var displayName = connId;
                    if (connName && connName !== connId) {
                        displayName += ' (' + connName + ')';
                    }
                    if (connType) {
                        displayName += ' [' + connType + ']';
                    }
                    
                    var item = '<div class="permission-item">' +
                        '<label>' +
                        '<input type="checkbox" id="group_conn_' + connId + '" class="permission-checkbox">' +
                        displayName +
                        '</label>' +
                        '</div>';
                    container.append(item);
                });
            }
        },
        error: function() {
            console.error('연결 정보 목록 로드 실패');
        }
    });
}

// 그룹 삭제
function deleteGroup(groupId) {
    if (!confirm('정말 삭제하시겠습니까? 그룹에 속한 사용자들의 그룹 할당이 해제됩니다.')) return;
    
    $.ajax({
        url: '/UserGroup/delete',
        type: 'POST',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                showToast(response.message, 'success');
                loadGroupTable();
                loadUserList(currentPage); // 사용자 목록 새로고침 (그룹 정보 변경 반영)
                updateGroupLists(); // 사용자 모달의 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error');
            }
        },
        error: function() {
            showToast('그룹 삭제 중 오류가 발생했습니다.', 'error');
        }
    });
}

// 그룹 권한 로드
function loadGroupPermissions(groupId) {
    // 모든 SQL 템플릿 카테고리 목록과 그룹 권한을 함께 로드
    $.ajax({
        url: '/UserGroup/categoryPermissions',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                displayGroupCategoryPermissions(response.data);
            }
        }
    });
    
    // 모든 연결 정보 목록과 그룹 권한을 함께 로드
    $.ajax({
        url: '/UserGroup/connectionPermissions',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                displayGroupConnectionPermissions(response.data);
            }
        }
    });
    
    // 메뉴 권한 로드
    $.ajax({
        url: '/UserGroup/menuPermissions',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            console.log('메뉴 권한 로드 응답:', response);
            if (response.success) {
                displayGroupMenuPermissions(response.data);
            } else {
                console.error('메뉴 권한 로드 실패:', response.message);
            }
        },
        error: function(xhr, status, error) {
            console.error('메뉴 권한 로드 중 오류:', error);
        }
    });
}

// 그룹 SQL 템플릿 카테고리 권한 표시
function displayGroupCategoryPermissions(permissions) {
    var container = $('#groupSqlTemplatePermissions');
    container.empty();
    
    // 모든 카테고리 목록을 먼저 로드
    $.ajax({
        url: '/UserGroup/categories',
        type: 'GET',
        success: function(response) {
            if (response.success && response.data) {
                var allCategories = response.data;
                var grantedPermissions = permissions || [];
                
                // 권한이 있는 카테고리 ID 목록 생성
                var grantedCategoryIds = grantedPermissions.map(function(p) {
                    return p.CATEGORY_ID;
                });
                
                // 모든 카테고리를 표시하고 권한이 있는 것만 체크
                allCategories.forEach(function(category) {
                    var isGranted = grantedCategoryIds.includes(category.CATEGORY_ID);
                    var item = '<div class="permission-item">' +
                        '<label>' +
                        '<input type="checkbox" id="group_category_' + category.CATEGORY_ID + '" class="permission-checkbox"' + (isGranted ? ' checked' : '') + '>' +
                        category.CATEGORY_NAME +
                        '</label>' +
                        //'<small class="text-muted">' + (category.CATEGORY_DESCRIPTION || '') + '</small>' +
                        '</div>';
                    container.append(item);
                });
            }
        },
        error: function() {
            console.error('카테고리 목록 로드 실패');
        }
    });
}

// 그룹 메뉴 권한 표시
function displayGroupMenuPermissions(permissions) {
    console.log('메뉴 권한 표시:', permissions);
    var container = $('#groupMenuPermissions');
    container.empty();
    
    if (permissions && permissions.length > 0) {
        permissions.forEach(function(menu) {
            var isGranted = menu.HAS_PERMISSION;
            var dependsOn = menu.DEPENDS_ON;
            var menuId = menu.MENU_ID;
            console.log('메뉴 권한:', menuId, '권한:', isGranted, '의존성:', dependsOn);
            
            var checkboxId = 'group_menu_' + menuId;
            var disabledAttr = '';
            var dataDependsOn = '';
            var indentClass = '';
            
            // 의존성이 있는 경우 data 속성 추가 및 들여쓰기 클래스 추가
            if (dependsOn) {
                dataDependsOn = ' data-depends-on="' + dependsOn + '"';
                // 대시보드 하위 권한인 경우 들여쓰기
                if (dependsOn === 'MENU_DASHBOARD') {
                    indentClass = ' menu-sub-item';
                }
            }
            
            var item = '<div class="permission-item' + indentClass + '" data-menu-id="' + menuId + '">' +
                '<label>' +
                '<input type="checkbox" id="' + checkboxId + '" class="permission-checkbox"' + 
                (isGranted ? ' checked' : '') + disabledAttr + dataDependsOn + '>' +
                menu.MENU_NAME +
                '</label>' +
                '<small class="text-muted">' + (menu.MENU_DESCRIPTION || '') + '</small>' +
                '</div>';
            container.append(item);
        });
        
        // 의존성 설정
        setupMenuDependencies();
    } else {
        console.log('메뉴 권한 데이터가 없습니다.');
    }
}

// 메뉴 권한 로드 후 의존성 설정
function setupMenuDependencies() {
    var dashboardCheckbox = $('#group_menu_MENU_DASHBOARD');
    // MENU_DASHBOARD_MONITORING_로 시작하는 모든 체크박스 찾기
    var monitoringCheckboxes = $('input[id^="group_menu_MENU_DASHBOARD_MONITORING_"]');
    
    // 대시보드 권한 변경 시
    dashboardCheckbox.on('change', function() {
        if (!$(this).is(':checked')) {
            // 대시보드 권한 해제 시 모든 모니터링 권한도 해제 및 비활성화
            monitoringCheckboxes.prop('checked', false);
            monitoringCheckboxes.prop('disabled', true);
        } else {
            // 대시보드 권한 체크 시 모든 모니터링 권한 체크박스 활성화
            monitoringCheckboxes.prop('disabled', false);
        }
    });
    
    // 모니터링 권한 체크 시 대시보드 권한 확인
    monitoringCheckboxes.on('change', function() {
        if ($(this).is(':checked') && !dashboardCheckbox.is(':checked')) {
            alert('대시보드 권한이 먼저 필요합니다.');
            $(this).prop('checked', false);
        }
    });
    
    // 초기 상태 설정
    if (!dashboardCheckbox.is(':checked')) {
        monitoringCheckboxes.prop('disabled', true);
    }
}

// 그룹 연결 정보 권한 표시
function displayGroupConnectionPermissions(permissions) {
    var container = $('#groupConnectionPermissions');
    container.empty();
    
    // 모든 연결 정보 목록을 먼저 로드
    $.ajax({
        url: '/Connection/list',
        type: 'GET',
        data: { page: 1, pageSize: 1000 },
        success: function(response) {
            if (response.success && response.data) {
                var allConnections = [];
                if (Array.isArray(response.data)) {
                    allConnections = response.data;
                } else if (response.data.databaseConnections) {
                    allConnections = response.data.databaseConnections;
                }
                
                var grantedPermissions = permissions || [];
                
                // 권한이 있는 연결 ID 목록 생성
                var grantedConnectionIds = grantedPermissions.map(function(p) {
                	if(p.HAS_PERMISSION===1)
                    return p.CONNECTION_ID;
                });
                
                // 모든 연결 정보를 표시하고 권한이 있는 것만 체크
                allConnections.forEach(function(conn) {
                    var connId = typeof conn === 'string' ? conn : conn.CONNECTION_ID;
                    var connName = typeof conn === 'string' ? conn : (conn.HOST_IP || conn.CONNECTION_ID);
                    var dbType = typeof conn === 'string' ? '' : (conn.DB_TYPE || '');
                    
                    
                    var isGranted = grantedConnectionIds.includes(connId);
                    var item = '<div class="permission-item">' +
                        '<label>' +
                        '<input type="checkbox" id="group_conn_' + connId + '" class="permission-checkbox"' + (isGranted ? ' checked' : '') + '>' +
                        connId +
                        '</label>' +
                        '<small class="text-muted">' + connName + (dbType ? ' - ' + dbType : '') + '</small>' +
                       
                        '</div>';
                    container.append(item);
                });
            }
        },
        error: function() {
            console.error('연결 정보 목록 로드 실패');
        }
    });
}

// 그룹 권한만 편집
function editGroupPermissions(groupId) {
    // 그룹 정보 로드
    $.ajax({
        url: '/UserGroup/detail',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                var group = response.data;
                $('#groupModalTitle').text('그룹 권한 관리 - ' + group.GROUP_NAME);
                $('#editGroupId').val(group.GROUP_ID);
                $('#groupName').val(group.GROUP_NAME);
                $('#groupDescription').val(group.GROUP_DESCRIPTION);
                $('#groupStatus').val(group.STATUS);
                
                // 권한 정보 로드
                loadGroupPermissions(groupId);
                
                // 권한 탭으로 바로 이동
                $('#groupModal').modal('show');
                setTimeout(function() {
                    $('#groupPermissionsTab-tab').tab('show');
                }, 100);
            } else {
                showToast(response.message, 'error');
            }
        }
    });
}

// 그룹 권한 저장 (통합 - 메뉴, 카테고리, 연결정보를 한 번에 저장)
function saveGroupPermissions(groupId) {
    console.log('권한 저장 시작 - groupId:', groupId);
    
    // 메뉴 권한 수집
    var selectedMenus = [];
    $('#groupMenuPermissions input[type="checkbox"]:checked').each(function() {
        var menuId = $(this).attr('id').replace('group_menu_', '');
        selectedMenus.push(menuId);
    });
    console.log('선택된 메뉴 권한:', selectedMenus);
    
    // 카테고리 권한 수집
    var selectedCategories = [];
    $('#groupSqlTemplatePermissions input[type="checkbox"]:checked').each(function() {
        var categoryId = $(this).attr('id').replace('group_category_', '');
        selectedCategories.push(categoryId);
    });
    console.log('선택된 카테고리 권한:', selectedCategories);
    
    // 연결정보 권한 수집
    var selectedConnections = [];
    $('#groupConnectionPermissions input[type="checkbox"]:checked').each(function() {
        var connectionId = $(this).attr('id').replace('group_conn_', '');
        selectedConnections.push(connectionId);
    });
    console.log('선택된 연결정보 권한:', selectedConnections);
    
    // 통합 권한 저장 (한 번에 처리)
    $.ajax({
        url: '/UserGroup/saveAllPermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            groupId: groupId,
            menuIds: selectedMenus,
            categoryIds: selectedCategories,
            connectionIds: selectedConnections
        }),
        success: function(response) {
            console.log('통합 권한 저장 응답:', response);
            if (response.success) {
                showToast('모든 권한이 성공적으로 저장되었습니다.', 'success');
            } else {
                console.error('권한 저장 실패:', response.message);
                showToast('권한 저장 실패: ' + response.message, 'error');
            }
        },
        error: function(xhr, status, error) {
            console.error('권한 저장 중 오류:', error);
            showToast('권한 저장 중 오류가 발생했습니다.', 'error');
        }
    });
}

// UserGroup 관리 페이지 열기
function openUserGroupManagement() {
    // 새 탭에서 UserGroup 관리 페이지 열기
    window.open('/UserGroup', '_blank');
}
</script>

<style>
@keyframes slideInDown {
    from {
        transform: translateY(-100%);
        opacity: 0;
    }
    to {
        transform: translateY(0);
        opacity: 1;
    }
}

/* 대시보드 하위 권한 들여쓰기 */
.permission-item.menu-sub-item {
    margin-left: 20px;
    padding-left: 10px;
}
</style>

</body>
