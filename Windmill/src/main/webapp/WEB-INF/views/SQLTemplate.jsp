<%@include file="common/common.jsp"%>
<!-- Ace Editor CDN -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ace.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ext-language_tools.js"></script>
<style>
.sql-editor {
    font-family: 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.4;
}

.config-panel {
    background-color: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 5px;
    padding: 15px;
}

.tree-node {
    cursor: pointer;
    padding: 5px;
    border-radius: 3px;
    margin: 2px 0;
}

.tree-node:hover {
    background-color: #e9ecef;
}

.tree-node.selected {
    background-color: #007bff;
    color: white;
}

.sql-preview {
    background-color: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 5px;
    padding: 10px;
    max-height: 300px;
    overflow-y: auto;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    white-space: pre-wrap;
    word-wrap: break-word;
    line-height: 1.4;
}

.tree-children {
    margin-left: 20px;
}

.folder-node .fa-folder {
    color: #ffc107;
}

.sql-node .fa-file-code-o {
    color: #28a745;
}

#sqlEditor, #configEditor {
    border: 1px solid #ccc;
    border-radius: 4px;
}
</style>

<script>
$(document).ready(function() {
    // SQL 템플릿 트리 로드
    loadSqlTemplateTree();
    
    // Ace Editor 초기화
    initSqlEditor();
    initConfigEditor();
});

// SQL 템플릿 트리 로드
function loadSqlTemplateTree() {
    $.ajax({
        type: 'post',
        url: '/SQLTemplate/tree',
        success: function(result) {
            renderSqlTree(result);
        },
        error: function() {
            alert('SQL 템플릿 트리 로드 실패');
        }
    });
}

// SQL 트리 렌더링
function renderSqlTree(data) {
    var treeContainer = $('#sqlTreeContainer');
    treeContainer.empty();
    
    if (data && data.length > 0) {
        data.forEach(function(item) {
            var node = createTreeNode(item);
            treeContainer.append(node);
        });
    } else {
        treeContainer.html('<div class="text-muted">SQL 템플릿이 없습니다.</div>');
    }
}

// 트리 노드 생성
function createTreeNode(item) {
    if (item.type === 'folder') {
        return createFolderNode(item);
    } else {
        return createSqlNode(item);
    }
}

// 폴더 노드 생성
function createFolderNode(folder) {
    var node = $('<div class="tree-node folder-node" data-id="' + folder.id + '" data-type="folder">' +
        '<i class="fa fa-folder"></i> ' + folder.name +
        '<i class="fa fa-angle-right pull-right"></i></div>');
    
    if (folder.children && folder.children.length > 0) {
        var childrenContainer = $('<div class="tree-children" style="display: none;"></div>');
        folder.children.forEach(function(child) {
            childrenContainer.append(createTreeNode(child));
        });
        node.append(childrenContainer);
    }
    
    return node;
}

// SQL 노드 생성
function createSqlNode(sql) {
    return $('<div class="tree-node sql-node" data-id="' + sql.id + '" data-type="sql">' +
        '<i class="fa fa-file-code-o"></i> ' + sql.name + '</div>');
}

// SQL 에디터 초기화
function initSqlEditor() {
    // Ace Editor가 로드되었는지 확인
    if (typeof ace !== 'undefined') {
        try {
            ace.require("ace/ext/language_tools");
            var sqlEditor = ace.edit("sqlEditor");
            sqlEditor.setTheme("ace/theme/chrome");
            sqlEditor.session.setMode("ace/mode/sql");
            sqlEditor.setOptions({
                enableBasicAutocompletion: true,
                enableSnippets: true,
                enableLiveAutocompletion: true
            });
            
            window.sqlEditor = sqlEditor;
        } catch (e) {
            console.log("Ace Editor initialization failed, using textarea");
            initTextareaEditor();
        }
    } else {
        // Ace Editor가 없는 경우 일반 textarea 사용
        console.log("Ace Editor not available, using textarea");
        initTextareaEditor();
    }
}

// 설정 에디터 초기화
function initConfigEditor() {
    // Ace Editor가 로드되었는지 확인
    if (typeof ace !== 'undefined') {
        try {
            var configEditor = ace.edit("configEditor");
            configEditor.setTheme("ace/theme/chrome");
            configEditor.session.setMode("ace/mode/properties");
            configEditor.setOptions({
                enableBasicAutocompletion: true,
                enableSnippets: true,
                enableLiveAutocompletion: true
            });
            
            window.configEditor = configEditor;
        } catch (e) {
            console.log("Ace Editor initialization failed, using textarea");
            initTextareaConfigEditor();
        }
    } else {
        // Ace Editor가 없는 경우 일반 textarea 사용
        console.log("Ace Editor not available, using textarea");
        initTextareaConfigEditor();
    }
}

// Textarea 기반 SQL 에디터 초기화
function initTextareaEditor() {
    var sqlEditorDiv = document.getElementById("sqlEditor");
    sqlEditorDiv.innerHTML = '<textarea id="sqlTextarea" style="width: 100%; height: 100%; font-family: monospace; font-size: 14px; border: none; resize: none; outline: none;"></textarea>';
    window.sqlEditor = {
        getValue: function() {
            return document.getElementById("sqlTextarea").value;
        },
        setValue: function(value) {
            document.getElementById("sqlTextarea").value = value || '';
        }
    };
}

// Textarea 기반 설정 에디터 초기화
function initTextareaConfigEditor() {
    var configEditorDiv = document.getElementById("configEditor");
    configEditorDiv.innerHTML = '<textarea id="configTextarea" style="width: 100%; height: 100%; font-family: monospace; font-size: 14px; border: none; resize: none; outline: none;"></textarea>';
    window.configEditor = {
        getValue: function() {
            return document.getElementById("configTextarea").value;
        },
        setValue: function(value) {
            document.getElementById("configTextarea").value = value || '';
        }
    };
}

// SQL 템플릿 선택
$(document).on('click', '.sql-node', function() {
    $('.tree-node').removeClass('selected');
    $(this).addClass('selected');
    
    var sqlId = $(this).data('id');
    loadSqlTemplate(sqlId);
});

// 폴더 토글
$(document).on('click', '.folder-node', function() {
    var children = $(this).find('.tree-children');
    var icon = $(this).find('.fa-angle-right');
    
    if (children.is(':visible')) {
        children.slideUp();
        icon.removeClass('fa-angle-down').addClass('fa-angle-right');
    } else {
        children.slideDown();
        icon.removeClass('fa-angle-right').addClass('fa-angle-down');
    }
});

// SQL 템플릿 로드
function loadSqlTemplate(sqlId) {
    $.ajax({
        type: 'post',
        url: '/SQLTemplate/detail',
        data: { sqlId: sqlId },
        success: function(result) {
            if (result.error) {
                alert('SQL 템플릿 로드 실패: ' + result.error);
                return;
            }
            
            $('#sqlTemplateId').val(result.menuId);
            $('#sqlTemplateName').val(result.menuName);
            $('#sqlTemplatePath').val(result.menuPath);
            
            // SQL 에디터에 내용 설정
            if (window.sqlEditor && window.sqlEditor.setValue) {
                window.sqlEditor.setValue(result.sqlContent || '');
            } else {
                $('#sqlEditor').val(result.sqlContent || '');
            }
            
            // 설정 에디터에 내용 설정
            var configText = '';
            if (result.config) {
                for (var key in result.config) {
                    configText += key + '=' + result.config[key] + '\n';
                }
            }
            
            if (window.configEditor && window.configEditor.setValue) {
                window.configEditor.setValue(configText);
            } else {
                $('#configEditor').val(configText);
            }
            
            // 미리보기 업데이트
            updateSqlPreview();
        },
        error: function() {
            alert('SQL 템플릿 로드 실패');
        }
    });
}

// SQL 미리보기 업데이트
function updateSqlPreview() {
    var sqlContent = '';
    if (window.sqlEditor && window.sqlEditor.getValue) {
        sqlContent = window.sqlEditor.getValue();
    } else {
        sqlContent = $('#sqlEditor').val();
    }
    $('#sqlPreview').text(sqlContent);
}

// 새 SQL 템플릿 생성
function createNewSqlTemplate() {
    $('#sqlTemplateId').val('');
    $('#sqlTemplateName').val('');
    $('#sqlTemplatePath').val('');
    
    if (window.sqlEditor && window.sqlEditor.setValue) {
        window.sqlEditor.setValue('');
    } else {
        $('#sqlEditor').val('');
    }
    
    if (window.configEditor && window.configEditor.setValue) {
        window.configEditor.setValue('');
    } else {
        $('#configEditor').val('');
    }
    
    updateSqlPreview();
    $('.tree-node').removeClass('selected');
}

// SQL 템플릿 저장
function saveSqlTemplate() {
    var sqlId = $('#sqlTemplateId').val();
    var sqlName = $('#sqlTemplateName').val();
    var sqlPath = $('#sqlTemplatePath').val();
    
    var sqlContent = '';
    if (window.sqlEditor && window.sqlEditor.getValue) {
        sqlContent = window.sqlEditor.getValue();
    } else {
        sqlContent = $('#sqlEditor').val();
    }
    
    var configContent = '';
    if (window.configEditor && window.configEditor.getValue) {
        configContent = window.configEditor.getValue();
    } else {
        configContent = $('#configEditor').val();
    }
    
    if (!sqlName || !sqlContent) {
        alert('SQL 이름과 내용을 입력해주세요.');
        return;
    }
    
    var data = {
        sqlId: sqlId,
        sqlName: sqlName,
        sqlPath: sqlPath,
        sqlContent: sqlContent,
        configContent: configContent
    };
    
    $.ajax({
        type: 'post',
        url: '/SQLTemplate/save',
        data: data,
        success: function(result) {
            if (result.success) {
                alert('SQL 템플릿이 저장되었습니다.');
                $('#sqlTemplateId').val(result.sqlId);
                loadSqlTemplateTree();
            } else {
                alert('저장 실패: ' + result.error);
            }
        },
        error: function() {
            alert('SQL 템플릿 저장 실패');
        }
    });
}

// SQL 템플릿 삭제
function deleteSqlTemplate() {
    var sqlId = $('#sqlTemplateId').val();
    
    if (!sqlId) {
        alert('삭제할 SQL 템플릿을 선택해주세요.');
        return;
    }
    
    if (!confirm('정말로 이 SQL 템플릿을 삭제하시겠습니까?')) {
        return;
    }
    
    $.ajax({
        type: 'post',
        url: '/SQLTemplate/delete',
        data: { sqlId: sqlId },
        success: function(result) {
            if (result.success) {
                alert('SQL 템플릿이 삭제되었습니다.');
                createNewSqlTemplate();
                loadSqlTemplateTree();
            } else {
                alert('삭제 실패: ' + result.error);
            }
        },
        error: function() {
            alert('SQL 템플릿 삭제 실패');
        }
    });
}

// SQL 테스트 실행
function testSqlTemplate() {
    var sqlContent = '';
    if (window.sqlEditor && window.sqlEditor.getValue) {
        sqlContent = window.sqlEditor.getValue();
    } else {
        sqlContent = $('#sqlEditor').val();
    }
    
    if (!sqlContent) {
        alert('테스트할 SQL을 입력해주세요.');
        return;
    }
    
    $('#testResult').html('<div class="alert alert-info">SQL 테스트 중...</div>');
    
    $.ajax({
        type: 'post',
        url: '/SQLTemplate/test',
        data: { sqlContent: sqlContent },
        success: function(result) {
            if (result.success) {
                $('#testResult').html('<div class="alert alert-success">SQL 문법 검증 성공!</div>');
            } else {
                $('#testResult').html('<div class="alert alert-danger">SQL 오류: ' + result.error + '</div>');
            }
        },
        error: function() {
            $('#testResult').html('<div class="alert alert-danger">SQL 테스트 실패</div>');
        }
    });
}

// SQL 에디터 내용 변경 시 미리보기 업데이트
$(document).on('input', '#sqlEditor', function() {
    updateSqlPreview();
});
</script>

<!-- Content Wrapper -->
<div class="content-wrapper" style="margin-left: 0">
    <!-- Content Header -->
    <section class="content-header">
        <h1>SQL 템플릿 관리</h1>
        <ol class="breadcrumb">
            <li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
            <li class="active">SQL 템플릿 관리</li>
        </ol>
    </section>
    
    <!-- Main content -->
    <section class="content">
        <div class="row">
            <!-- SQL 트리 패널 -->
            <div class="col-md-3">
                <div class="box box-primary">
                    <div class="box-header with-border">
                        <h3 class="box-title">SQL 템플릿 트리</h3>
                        <div class="box-tools pull-right">
                            <button type="button" class="btn btn-box-tool" onclick="createNewSqlTemplate()">
                                <i class="fa fa-plus"></i> 새로 만들기
                            </button>
                        </div>
                    </div>
                    <div class="box-body">
                        <div id="sqlTreeContainer">
                            <!-- SQL 트리가 여기에 로드됩니다 -->
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- SQL 편집 패널 -->
            <div class="col-md-9">
                <div class="box box-success">
                    <div class="box-header with-border">
                        <h3 class="box-title">SQL 템플릿 편집</h3>
                        <div class="box-tools pull-right">
                            <button type="button" class="btn btn-info btn-sm" onclick="testSqlTemplate()">
                                <i class="fa fa-play"></i> 테스트
                            </button>
                            <button type="button" class="btn btn-success btn-sm" onclick="saveSqlTemplate()">
                                <i class="fa fa-save"></i> 저장
                            </button>
                            <button type="button" class="btn btn-danger btn-sm" onclick="deleteSqlTemplate()">
                                <i class="fa fa-trash"></i> 삭제
                            </button>
                        </div>
                    </div>
                    <div class="box-body">
                        <!-- 기본 정보 -->
                        <div class="row">
                            <div class="col-md-4">
                                <div class="form-group">
                                    <label>SQL 이름</label>
                                    <input type="text" class="form-control" id="sqlTemplateName" placeholder="SQL 이름">
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="form-group">
                                    <label>경로</label>
                                    <input type="text" class="form-control" id="sqlTemplatePath" placeholder="경로">
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="form-group">
                                    <label>ID</label>
                                    <input type="text" class="form-control" id="sqlTemplateId" readonly>
                                </div>
                            </div>
                        </div>
                        
                        <!-- SQL 에디터 -->
                        <div class="form-group">
                            <label>SQL 내용</label>
                            <div id="sqlEditor" style="height: 300px; border: 1px solid #ccc;"></div>
                        </div>
                        
                        <!-- 설정 패널 -->
                        <div class="form-group">
                            <label>설정 (Properties)</label>
                            <div id="configEditor" style="height: 200px; border: 1px solid #ccc;"></div>
                        </div>
                        
                        <!-- 미리보기 -->
                        <div class="form-group">
                            <label>SQL 미리보기</label>
                            <div id="sqlPreview" class="sql-preview"></div>
                        </div>
                        
                        <!-- 테스트 결과 -->
                        <div id="testResult"></div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>
