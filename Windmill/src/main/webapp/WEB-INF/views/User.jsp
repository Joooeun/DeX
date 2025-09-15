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
                        <div class="box-tools pull-right">
                            <button type="button" class="btn btn-primary btn-sm" onclick="showCreateUserModal()">
                                <i class="fa fa-plus"></i> 새 사용자
                            </button>
                        </div>
                        <div class="row" style="margin-top: 10px;">
                            <div class="col-sm-3">
                                <div class="input-group">
                                    <input type="text" class="form-control" id="searchKeyword" placeholder="ID/이름">
                                    <span class="input-group-btn">
                                        <button type="button" class="btn btn-default" onclick="searchUsers()">
                                            <i class="fa fa-search"></i>
                                        </button>
                                    </span>
                                </div>
                            </div>
                            <div class="col-sm-3">
                                <div class="input-group">
                                    <span class="input-group-addon">그룹</span>
                                    <select class="form-control" id="groupFilter" onchange="filterByGroup()">
                                        <option value="">전체 그룹</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="box-body">
                        <table id="userTable" class="table table-bordered table-striped">
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
                        <div class="text-center">
                            <ul class="pagination" id="pagination">
                            </ul>
                            <div class="pagination-info">
                                <span id="paginationInfo"></span>
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
                            <table class="table table-bordered table-hover" id="groupTable">
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
                        <small class="text-muted">
                            <strong>수정 시 비워두면 변경하지 않습니다.</strong><br>
                            <span class="text-warning">⚠️ 비밀번호를 입력하면 해당 비밀번호가 임시 비밀번호로 설정되며, 해당 사용자는 다음 로그인 시 비밀번호 변경이 강제됩니다.</span>
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
                        <label for="groupId" data-toggle="tooltip" data-placement="top" title="사용자가 속할 그룹을 선택합니다. 그룹별로 접근 권한과 연결 권한이 설정되며, 사용자의 역할을 결정합니다.">그룹</label>
                        <select class="form-control" id="groupId">
                            <option value="">그룹을 선택하세요</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="ipRestriction" data-toggle="tooltip" data-placement="top" title="사용자의 로그인을 허용할 IP 주소를 설정합니다. 여러 IP는 쉼표로 구분하고, 와일드카드(*) 사용 가능합니다. 비워두면 모든 IP에서 로그인 가능합니다.">IP 제한</label>
                        <input type="text" class="form-control" id="ipRestriction" placeholder="예: 192.168.1.*, 10.0.0.100">
                        <small class="text-muted">
                            <strong>예시:</strong> 192.168.1.* (192.168.1.x 대역), 10.0.0.100 (특정 IP), 192.168.1.*,10.0.0.100 (여러 IP)<br>
                            <span class="text-info">💡 비워두면 모든 IP에서 로그인 가능합니다.</span>
                        </small>
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
                            <div class="col-md-6">
                                <h5>SQL 템플릿 카테고리 권한</h5>
                                <div class="permission-section" id="groupSqlTemplatePermissions">
                                    <!-- SQL 템플릿 카테고리 권한이 여기에 로드됩니다 -->
                                </div>
                            </div>
                            <div class="col-md-6">
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
    max-height: 300px;
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

// 전역 변수로 현재 페이지 관리
var currentPage = 1;

// 사용자 목록 로드
function loadUserList(page) {
    if (page) {
        currentPage = page;
    }
    
    var searchKeyword = $('#searchKeyword').val();
    var groupFilter = $('#groupFilter').val();
    
    $.ajax({
        url: '/User/list',
        type: 'GET',
        data: { 
            searchKeyword: searchKeyword,
            groupFilter: groupFilter,
            page: currentPage,
            pageSize: 5
        },
        success: function(response) {
            if (response.success) {
                displayUserList(response.data);
                displayPagination(response.pagination);
            } else {
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('사용자 목록 조회 중 오류가 발생했습니다.', 'error', '오류');
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

// 페이징 UI 표시
function displayPagination(pagination) {
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
    var startPage = Math.max(1, currentPage - 2);
    var endPage = Math.min(totalPages, currentPage + 2);
    
    for (var i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            paginationContainer.append('<li class="active"><a href="#">' + i + '</a></li>');
        } else {
            paginationContainer.append('<li><a href="#" onclick="loadUserList(' + i + ')">' + i + '</a></li>');
        }
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

// 그룹 목록 로드
function loadGroupList() {
    $.ajax({
        url: '/User/groups',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                var select = $('#groupId');
                select.empty();
                select.append('<option value="">그룹 선택</option>');
                
                response.data.forEach(function(group) {
                    select.append('<option value="' + group.GROUP_ID + '">' + group.GROUP_NAME + '</option>');
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
    $('#password').attr('required', true);
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
                $('#userModalTitle').text('사용자 수정');
                $('#editUserId').val(user.USER_ID);
                $('#userId').val(user.USER_ID).prop('readonly', true);
                $('#userName').val(user.USER_NAME);
                $('#status').val(user.STATUS);
                $('#ipRestriction').val(user.IP_RESTRICTION || '');
                $('#password').attr('required', false);
                
                // 사용자의 현재 그룹 정보 로드
                loadUserGroup(userId);
                
                $('#userModal').modal('show');
            } else {
                showToast(response.message, 'error', '오류');
            }
        }
    });
}

// 사용자의 현재 그룹 정보 로드
function loadUserGroup(userId) {
    $.ajax({
        url: '/User/currentGroup',
        type: 'GET',
        data: { userId: userId },
        success: function(response) {
            if (response.success && response.data) {
                $('#groupId').val(response.data.GROUP_ID);
            } else {
                $('#groupId').val('');
            }
        },
        error: function() {
            $('#groupId').val('');
        }
    });
}

// 사용자 저장
function saveUser() {
    var editUserId = $('#editUserId').val();
    var userData = {
        userId: $('#userId').val(),
        userName: $('#userName').val(),
        status: $('#status').val(),
        groupId: $('#groupId').val(),
        ipRestriction: $('#ipRestriction').val()
    };
    
    var password = $('#password').val();
    if (password) {
        userData.password = password;
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
                showToast(response.message, 'success', '성공');
                // Bootstrap 3에서는 getInstance가 없으므로 직접 숨김
                // var userModal = bootstrap.Modal.getInstance(document.getElementById('userModal'));
                $('#userModal').modal('hide');
                loadUserList(currentPage);
                updateGroupLists(); // 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('사용자 저장 중 오류가 발생했습니다.', 'error', '오류');
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
                showToast(response.message, 'success', '성공');
                loadUserList(currentPage);
                updateGroupLists(); // 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('사용자 삭제 중 오류가 발생했습니다.', 'error', '오류');
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
                showToast('권한이 저장되었습니다.', 'success', '성공');
                // Bootstrap 3에서는 getInstance가 없으므로 직접 숨김
                // var permissionModal = bootstrap.Modal.getInstance(document.getElementById('permissionModal'));
                $('#permissionModal').modal('hide');
            } else {
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('권한 저장 중 오류가 발생했습니다.', 'error', '오류');
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
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('활동 로그 조회 중 오류가 발생했습니다.', 'error', '오류');
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
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('그룹 목록 조회 중 오류가 발생했습니다.', 'error', '오류');
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
                $('#groupModalTitle').text('그룹 수정');
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
                showToast(response.message, 'success', '성공');
                // Bootstrap 3에서는 getInstance가 없으므로 직접 숨김
                // var groupModal = bootstrap.Modal.getInstance(document.getElementById('groupModal'));
                $('#groupModal').modal('hide');
                loadGroupTable();
                updateGroupLists(); // 사용자 모달의 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('그룹 저장 중 오류가 발생했습니다.', 'error', '오류');
        }
    });
}

// 모든 권한 목록 로드 (단순화)
function loadAllPermissions() {
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
                        '<small class="text-muted">' + (category.CATEGORY_DESCRIPTION || '') + '</small>' +
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
                }
                
                connections.forEach(function(conn) {
                    var connId = typeof conn === 'string' ? conn : conn.CONNECTION_ID;
                    var connName = typeof conn === 'string' ? conn : (conn.HOST_IP || conn.CONNECTION_ID);
                    
                    var item = '<div class="permission-item">' +
                        '<label>' +
                        '<input type="checkbox" id="group_conn_' + connId + '" class="permission-checkbox">' +
                        connId + ' (' + connName + ')' +
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

// 그룹 권한 저장
function saveGroupPermissions(groupId) {
    // 카테고리 권한 저장
    var selectedCategories = [];
    $('#groupSqlTemplatePermissions input[type="checkbox"]:checked').each(function() {
        var categoryId = $(this).attr('id').replace('group_category_', '');
        selectedCategories.push(categoryId);
    });
    
    // 연결정보 권한 저장
    var selectedConnections = [];
    $('#groupConnectionPermissions input[type="checkbox"]:checked').each(function() {
        var connectionId = $(this).attr('id').replace('group_conn_', '');
        selectedConnections.push(connectionId);
    });
    
    // 카테고리 권한 저장
    $.ajax({
        url: '/UserGroup/grantCategoryPermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            groupId: groupId,
            categoryIds: selectedCategories
        }),
        success: function(response) {
            if (!response.success) {
                console.error('카테고리 권한 저장 실패:', response.message);
            }
        },
        error: function() {
            console.error('카테고리 권한 저장 중 오류 발생');
        }
    });
    
    // 연결정보 권한 저장
    $.ajax({
        url: '/UserGroup/grantConnectionPermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            groupId: groupId,
            connectionIds: selectedConnections
        }),
        success: function(response) {
            if (!response.success) {
                console.error('연결정보 권한 저장 실패:', response.message);
            }
        },
        error: function() {
            console.error('연결정보 권한 저장 중 오류 발생');
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
                showToast(response.message, 'success', '성공');
                loadGroupTable();
                updateGroupLists(); // 사용자 모달의 그룹 목록과 필터 업데이트
            } else {
                showToast(response.message, 'error', '오류');
            }
        },
        error: function() {
            showToast('그룹 삭제 중 오류가 발생했습니다.', 'error', '오류');
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
                        category.CATEGORY_NAME + ' (' + category.CATEGORY_ID + ')' +
                        '</label>' +
                        '<small class="text-muted">' + (category.CATEGORY_DESCRIPTION || '') + '</small>' +
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
                        connId + ' (' + connName + (dbType ? ' - ' + dbType : '') + ')' +
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
                showToast(response.message, 'error', '오류');
            }
        }
    });
}

// 그룹 권한 저장
function saveGroupPermissions(groupId) {
    // 카테고리 권한 저장
    var selectedCategories = [];
    $('#groupSqlTemplatePermissions input[type="checkbox"]:checked').each(function() {
        var categoryId = $(this).attr('id').replace('group_category_', '');
        selectedCategories.push(categoryId);
    });
    
    // 연결정보 권한 저장
    var selectedConnections = [];
    $('#groupConnectionPermissions input[type="checkbox"]:checked').each(function() {
        var connectionId = $(this).attr('id').replace('group_conn_', '');
        selectedConnections.push(connectionId);
    });
    
    // 카테고리 권한 저장
    $.ajax({
        url: '/UserGroup/grantCategoryPermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            groupId: groupId,
            categoryIds: selectedCategories
        }),
        success: function(response) {
            if (!response.success) {
                console.error('카테고리 권한 저장 실패:', response.message);
            }
        },
        error: function() {
            console.error('카테고리 권한 저장 중 오류 발생');
        }
    });
    
    // 연결정보 권한 저장
    $.ajax({
        url: '/UserGroup/grantConnectionPermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            groupId: groupId,
            connectionIds: selectedConnections
        }),
        success: function(response) {
            if (!response.success) {
                console.error('연결정보 권한 저장 실패:', response.message);
            }
        },
        error: function() {
            console.error('연결정보 권한 저장 중 오류 발생');
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
</style>

</body>
