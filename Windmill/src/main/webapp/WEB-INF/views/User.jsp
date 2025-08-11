<%@include file="common/common.jsp"%>
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
                    </div>
                    <div class="box-body">
                        <table id="userTable" class="table table-bordered table-striped">
                            <thead>
                                <tr>
                                    <th>사용자 ID</th>
                                    <th>이름</th>
                                    <th>상태</th>
                                    <th>마지막 로그인</th>
                                    <th>로그인 실패</th>
                                    <th>생성일</th>
                                    <th>관리</th>
                                </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>

<!-- 사용자 생성/수정 모달 -->
<div class="modal fade" id="userModal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title" id="userModalTitle">사용자 생성</h4>
            </div>
            <div class="modal-body">
                <form id="userForm">
                    <input type="hidden" id="editUserId">
                    <div class="form-group">
                        <label for="userId">사용자 ID</label>
                        <input type="text" class="form-control" id="userId" required>
                    </div>
                    <div class="form-group">
                        <label for="userName">이름</label>
                        <input type="text" class="form-control" id="userName" required>
                    </div>
                    <div class="form-group">
                        <label for="password">비밀번호</label>
                        <input type="password" class="form-control" id="password">
                        <small class="text-muted">수정 시 비워두면 변경하지 않습니다.</small>
                    </div>
                    <div class="form-group">
                        <label for="status">상태</label>
                        <select class="form-control" id="status">
                            <option value="ACTIVE">활성</option>
                            <option value="INACTIVE">비활성</option>
                            <option value="LOCKED">잠금</option>
                        </select>
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

<!-- 그룹 할당 모달 -->
<div class="modal fade" id="groupModal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">그룹 할당</h4>
            </div>
            <div class="modal-body">
                <input type="hidden" id="groupUserId">
                <div class="form-group">
                    <label for="groupId">그룹</label>
                    <select class="form-control" id="groupId">
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" onclick="assignGroup()">할당</button>
            </div>
        </div>
    </div>
</div>

<script>
$(document).ready(function() {
    loadUserList();
    loadGroupList();
});

// 사용자 목록 로드
function loadUserList() {
    $.ajax({
        url: '/User/list',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                displayUserList(response.data);
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('사용자 목록 조회 중 오류가 발생했습니다.');
        }
    });
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
            '<td>' + formatDate(user.LAST_LOGIN_TIMESTAMP) + '</td>' +
            '<td>' + (user.LOGIN_FAIL_COUNT || 0) + '</td>' +
            '<td>' + formatDate(user.CREATED_TIMESTAMP) + '</td>' +
            '<td>' +
                '<button class="btn btn-xs btn-info" onclick="editUser(\'' + user.USER_ID + '\')">수정</button> ' +
                '<button class="btn btn-xs btn-warning" onclick="showGroupModal(\'' + user.USER_ID + '\')">그룹</button> ' +
                '<button class="btn btn-xs btn-danger" onclick="deleteUser(\'' + user.USER_ID + '\')">삭제</button>' +
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
                $('#password').attr('required', false);
                $('#userModal').modal('show');
            } else {
                alert(response.message);
            }
        }
    });
}

// 사용자 저장
function saveUser() {
    var editUserId = $('#editUserId').val();
    var userData = {
        userId: $('#userId').val(),
        userName: $('#userName').val(),
        status: $('#status').val()
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
                alert(response.message);
                $('#userModal').modal('hide');
                loadUserList();
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('사용자 저장 중 오류가 발생했습니다.');
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
                alert(response.message);
                loadUserList();
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('사용자 삭제 중 오류가 발생했습니다.');
        }
    });
}

// 그룹 할당 모달 표시
function showGroupModal(userId) {
    $('#groupUserId').val(userId);
    $('#groupModal').modal('show');
}

// 그룹 할당
function assignGroup() {
    var userId = $('#groupUserId').val();
    var groupId = $('#groupId').val();
    
    if (!groupId) {
        alert('그룹을 선택해주세요.');
        return;
    }
    
    $.ajax({
        url: '/User/assignGroup',
        type: 'POST',
        data: { userId: userId, groupId: groupId },
        success: function(response) {
            if (response.success) {
                alert(response.message);
                $('#groupModal').modal('hide');
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('그룹 할당 중 오류가 발생했습니다.');
        }
    });
}
</script>

</body>
