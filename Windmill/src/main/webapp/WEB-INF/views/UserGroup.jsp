<%@include file="common/common.jsp"%>
<div class="content-wrapper" style="margin-left: 0">
    <section class="content-header">
        <h1>사용자 그룹 관리</h1>
    </section>

    <section class="content">
        <div class="row">
            <div class="col-md-12">
                <div class="box">
                    <div class="box-header with-border">
                        <h3 class="box-title">그룹 목록</h3>
                        <div class="box-tools pull-right">
                            <button type="button" class="btn btn-primary btn-sm" onclick="showCreateGroupModal()">
                                <i class="fa fa-plus"></i> 새 그룹
                            </button>
                        </div>
                    </div>
                    <div class="box-body">
                        <table id="groupTable" class="table table-bordered table-striped">
                            <thead>
                                <tr>
                                    <th>그룹 ID</th>
                                    <th>그룹명</th>
                                    <th>설명</th>
                                    <th>상태</th>
                                    <th>멤버 수</th>
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

<!-- 그룹 생성/수정 모달 -->
<div class="modal fade" id="groupModal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title" id="groupModalTitle">그룹 생성</h4>
            </div>
            <div class="modal-body">
                <form id="groupForm">
                    <input type="hidden" id="editGroupId">
                    <div class="form-group">
                        <label for="groupId">그룹 ID</label>
                        <input type="text" class="form-control" id="groupId" required>
                    </div>
                    <div class="form-group">
                        <label for="groupName">그룹명</label>
                        <input type="text" class="form-control" id="groupName" required>
                    </div>
                    <div class="form-group">
                        <label for="groupDescription">설명</label>
                        <textarea class="form-control" id="groupDescription" rows="3"></textarea>
                    </div>
                    <div class="form-group">
                        <label for="parentGroupId">상위 그룹</label>
                        <select class="form-control" id="parentGroupId">
                            <option value="">없음</option>
                        </select>
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
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" onclick="saveGroup()">저장</button>
            </div>
        </div>
    </div>
</div>

<!-- 그룹 멤버 관리 모달 -->
<div class="modal fade" id="memberModal" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">그룹 멤버 관리</h4>
            </div>
            <div class="modal-body">
                <input type="hidden" id="memberGroupId">
                <div class="row">
                    <div class="col-md-6">
                        <h5>그룹에 속하지 않은 사용자</h5>
                        <div class="form-group">
                            <input type="text" class="form-control" id="userSearch" placeholder="사용자 검색...">
                        </div>
                        <div id="availableUsers" class="user-list">
                            <!-- 사용 가능한 사용자 목록이 여기에 로드됩니다 -->
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h5>그룹 멤버</h5>
                        <div id="groupMembers" class="user-list">
                            <!-- 그룹 멤버 목록이 여기에 로드됩니다 -->
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">닫기</button>
            </div>
        </div>
    </div>
</div>

<!-- 그룹 권한 관리 모달 -->
<div class="modal fade" id="groupPermissionModal" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">그룹 권한 관리</h4>
            </div>
            <div class="modal-body">
                <input type="hidden" id="permissionGroupId">
                <div class="row">
                    <div class="col-md-6">
                        <h5>SQL 템플릿 권한</h5>
                        <div id="groupSqlTemplatePermissions" class="permission-section">
                            <!-- SQL 템플릿 권한 목록이 여기에 로드됩니다 -->
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h5>연결 정보 권한</h5>
                        <div id="groupConnectionPermissions" class="permission-section">
                            <!-- 연결 정보 권한 목록이 여기에 로드됩니다 -->
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
                <button type="button" class="btn btn-primary" onclick="saveGroupPermissions()">저장</button>
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

.user-list {
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #ddd;
    padding: 10px;
    border-radius: 4px;
}

.user-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 5px 0;
    border-bottom: 1px solid #eee;
    cursor: pointer;
}

.user-item:hover {
    background-color: #f5f5f5;
}

.user-item:last-child {
    border-bottom: none;
}

.user-item.selected {
    background-color: #e3f2fd;
}
</style>

<script>
$(document).ready(function() {
    loadGroupList();
    loadParentGroupList();
});

// 그룹 목록 로드
function loadGroupList() {
    $.ajax({
        url: '/UserGroup/list',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                displayGroupList(response.data);
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('그룹 목록 조회 중 오류가 발생했습니다.');
        }
    });
}

// 그룹 목록 표시
function displayGroupList(groupList) {
    var tbody = $('#groupTable tbody');
    tbody.empty();
    
    groupList.forEach(function(group) {
        var row = '<tr>' +
            '<td>' + group.GROUP_ID + '</td>' +
            '<td>' + group.GROUP_NAME + '</td>' +
            '<td>' + (group.DESCRIPTION || '-') + '</td>' +
            '<td>' + getStatusBadge(group.STATUS) + '</td>' +
            '<td>' + (group.MEMBER_COUNT || 0) + '</td>' +
            '<td>' + formatDate(group.CREATED_TIMESTAMP) + '</td>' +
            '<td>' +
                '<button class="btn btn-xs btn-info" onclick="editGroup(\'' + group.GROUP_ID + '\')">수정</button> ' +
                '<button class="btn btn-xs btn-warning" onclick="showMemberModal(\'' + group.GROUP_ID + '\')">멤버</button> ' +
                '<button class="btn btn-xs btn-success" onclick="showGroupPermissionModal(\'' + group.GROUP_ID + '\')">권한</button> ' +
                '<button class="btn btn-xs btn-danger" onclick="deleteGroup(\'' + group.GROUP_ID + '\')">삭제</button>' +
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
    }
    
    return '<span class="label ' + badgeClass + '">' + statusText + '</span>';
}

// 날짜 포맷
function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('ko-KR');
}

// 상위 그룹 목록 로드
function loadParentGroupList() {
    $.ajax({
        url: '/UserGroup/parentGroups',
        type: 'GET',
        success: function(response) {
            if (response.success) {
                var select = $('#parentGroupId');
                select.find('option:not(:first)').remove();
                
                response.data.forEach(function(group) {
                    select.append('<option value="' + group.GROUP_ID + '">' + group.GROUP_NAME + '</option>');
                });
            }
        }
    });
}

// 새 그룹 모달 표시
function showCreateGroupModal() {
    $('#groupModalTitle').text('그룹 생성');
    $('#groupForm')[0].reset();
    $('#editGroupId').val('');
    $('#groupId').prop('readonly', false);
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
                $('#groupId').val(group.GROUP_ID).prop('readonly', true);
                $('#groupName').val(group.GROUP_NAME);
                $('#groupDescription').val(group.DESCRIPTION);
                $('#parentGroupId').val(group.PARENT_GROUP_ID);
                $('#groupStatus').val(group.STATUS);
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
        groupId: $('#groupId').val(),
        groupName: $('#groupName').val(),
        description: $('#groupDescription').val(),
        parentGroupId: $('#parentGroupId').val(),
        status: $('#groupStatus').val()
    };
    
    var url = editGroupId ? '/UserGroup/update?groupId=' + editGroupId : '/UserGroup/create';
    var method = editGroupId ? 'POST' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(groupData),
        success: function(response) {
            if (response.success) {
                alert(response.message);
                $('#groupModal').modal('hide');
                loadGroupList();
                loadParentGroupList();
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('그룹 저장 중 오류가 발생했습니다.');
        }
    });
}

// 그룹 삭제
function deleteGroup(groupId) {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    
    $.ajax({
        url: '/UserGroup/delete',
        type: 'POST',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                alert(response.message);
                loadGroupList();
                loadParentGroupList();
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('그룹 삭제 중 오류가 발생했습니다.');
        }
    });
}

// 멤버 관리 모달 표시
function showMemberModal(groupId) {
    $('#memberGroupId').val(groupId);
    loadAvailableUsers(groupId);
    loadGroupMembers(groupId);
    $('#memberModal').modal('show');
}

// 사용 가능한 사용자 로드
function loadAvailableUsers(groupId) {
    $.ajax({
        url: '/UserGroup/availableUsers',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                displayAvailableUsers(response.data);
            }
        }
    });
}

// 사용 가능한 사용자 표시
function displayAvailableUsers(users) {
    var container = $('#availableUsers');
    container.empty();
    
    users.forEach(function(user) {
        var item = '<div class="user-item" onclick="addUserToGroup(\'' + user.USER_ID + '\')">' +
            '<div>' + user.USER_NAME + ' (' + user.USER_ID + ')</div>' +
            '<i class="fa fa-plus text-success"></i>' +
            '</div>';
        container.append(item);
    });
}

// 그룹 멤버 로드
function loadGroupMembers(groupId) {
    $.ajax({
        url: '/UserGroup/groupMembers',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                displayGroupMembers(response.data);
            }
        }
    });
}

// 그룹 멤버 표시
function displayGroupMembers(members) {
    var container = $('#groupMembers');
    container.empty();
    
    members.forEach(function(member) {
        var item = '<div class="user-item" onclick="removeUserFromGroup(\'' + member.USER_ID + '\')">' +
            '<div>' + member.USER_NAME + ' (' + member.USER_ID + ')</div>' +
            '<i class="fa fa-minus text-danger"></i>' +
            '</div>';
        container.append(item);
    });
}

// 사용자를 그룹에 추가
function addUserToGroup(userId) {
    var groupId = $('#memberGroupId').val();
    
    $.ajax({
        url: '/UserGroup/addMember',
        type: 'POST',
        data: { groupId: groupId, userId: userId },
        success: function(response) {
            if (response.success) {
                loadAvailableUsers(groupId);
                loadGroupMembers(groupId);
            } else {
                alert(response.message);
            }
        }
    });
}

// 사용자를 그룹에서 제거
function removeUserFromGroup(userId) {
    var groupId = $('#memberGroupId').val();
    
    if (!confirm('정말 그룹에서 제거하시겠습니까?')) return;
    
    $.ajax({
        url: '/UserGroup/removeMember',
        type: 'POST',
        data: { groupId: groupId, userId: userId },
        success: function(response) {
            if (response.success) {
                loadAvailableUsers(groupId);
                loadGroupMembers(groupId);
            } else {
                alert(response.message);
            }
        }
    });
}

// 그룹 권한 관리 모달 표시
function showGroupPermissionModal(groupId) {
    $('#permissionGroupId').val(groupId);
    loadGroupPermissions(groupId);
    $('#groupPermissionModal').modal('show');
}

// 그룹 권한 로드
function loadGroupPermissions(groupId) {
    // SQL 템플릿 권한 로드
    $.ajax({
        url: '/UserGroup/sqlTemplatePermissions',
        type: 'GET',
        data: { groupId: groupId },
        success: function(response) {
            if (response.success) {
                displayGroupSqlTemplatePermissions(response.data);
            }
        }
    });
    
    // 연결 정보 권한 로드
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

// 그룹 SQL 템플릿 권한 표시
function displayGroupSqlTemplatePermissions(permissions) {
    var container = $('#groupSqlTemplatePermissions');
    container.empty();
    
    permissions.forEach(function(permission) {
        var item = '<div class="permission-item">' +
            '<div>' +
                '<input type="checkbox" class="permission-checkbox" id="group_sql_' + permission.TEMPLATE_ID + '" ' +
                (permission.HAS_PERMISSION ? 'checked' : '') + '>' +
                '<label for="group_sql_' + permission.TEMPLATE_ID + '">' + permission.TEMPLATE_NAME + '</label>' +
            '</div>' +
            '<small class="text-muted">' + permission.CATEGORY_PATH + '</small>' +
            '</div>';
        container.append(item);
    });
}

// 그룹 연결 정보 권한 표시
function displayGroupConnectionPermissions(permissions) {
    var container = $('#groupConnectionPermissions');
    container.empty();
    
    permissions.forEach(function(permission) {
        var item = '<div class="permission-item">' +
            '<div>' +
                '<input type="checkbox" class="permission-checkbox" id="group_conn_' + permission.CONNECTION_ID + '" ' +
                (permission.HAS_PERMISSION ? 'checked' : '') + '>' +
                '<label for="group_conn_' + permission.CONNECTION_ID + '">' + permission.CONNECTION_NAME + '</label>' +
            '</div>' +
            '<small class="text-muted">' + permission.DB_TYPE + '</small>' +
            '</div>';
        container.append(item);
    });
}

// 그룹 권한 저장
function saveGroupPermissions() {
    var groupId = $('#permissionGroupId').val();
    var permissions = {
        sqlTemplatePermissions: [],
        connectionPermissions: []
    };
    
    // SQL 템플릿 권한 수집
    $('#groupSqlTemplatePermissions input[type="checkbox"]').each(function() {
        var templateId = $(this).attr('id').replace('group_sql_', '');
        permissions.sqlTemplatePermissions.push({
            templateId: templateId,
            hasPermission: $(this).is(':checked')
        });
    });
    
    // 연결 정보 권한 수집
    $('#groupConnectionPermissions input[type="checkbox"]').each(function() {
        var connectionId = $(this).attr('id').replace('group_conn_', '');
        permissions.connectionPermissions.push({
            connectionId: connectionId,
            hasPermission: $(this).is(':checked')
        });
    });
    
    $.ajax({
        url: '/UserGroup/savePermissions',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            groupId: groupId,
            permissions: permissions
        }),
        success: function(response) {
            if (response.success) {
                alert('권한이 저장되었습니다.');
                $('#groupPermissionModal').modal('hide');
            } else {
                alert(response.message);
            }
        },
        error: function() {
            alert('권한 저장 중 오류가 발생했습니다.');
        }
    });
}
</script>

</body>
