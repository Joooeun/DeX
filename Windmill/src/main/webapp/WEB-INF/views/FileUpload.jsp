<%@include file="common/common.jsp"%>
<%@include file="common/ace.jsp"%>
<script>
	$(document).ready(function() {

		$.ajax({
			type : 'post',
			url : "/Connection/list",
			data : {
				TYPE : "HOST"
			},
			success : function(result) {
				if (result.success && result.data) {
					for (var i = 0; i < result.data.length; i++) {
						$('#connectionlist').append("<option value='" + result.data[i] + "'>" + result.data[i] + "</option>");
					}
				} else {
				// SFTP 연결 목록을 가져올 수 없습니다
			}
		},
		error : function(xhr, status, error) {
			showSystemError("SFTP 연결 목록 조회 실패", {
				error: xhr.responseText,
				status: xhr.status,
				url: '/FileUpload/sftpList'
			});
		}
		});
		
		// Ace Editor 초기화
		initAceEditor();
	});
	
	// Ace Editor 초기화 함수
	function initAceEditor() {
		try {
			// Ace Editor가 로드되었는지 확인
			if (typeof ace !== 'undefined') {
				var selectedFont = localStorage.getItem('selectedFont') || 'D2Coding';
				
				ace.require("ace/ext/language_tools");
				window.contentEditor = ace.edit("contentEditor");
				window.contentEditor.setTheme("ace/theme/chrome");
				window.contentEditor.session.setMode("ace/mode/text");
				window.contentEditor.setShowPrintMargin(false);
				window.contentEditor.setFontSize(14);
				window.contentEditor.setOptions({
					fontFamily: selectedFont,
					enableBasicAutocompletion: true,
					enableSnippets: true,
					enableLiveAutocompletion: true,
					showPrintMargin: false,
					showGutter: true,
					showInvisibles: false
				});
				
				// 에디터 리사이즈
				setTimeout(function() {
					window.contentEditor.resize();
				}, 100);
				
				// 우하단 모서리 드래그로 높이 변경(저장 없음) - 공통 함수 사용
				if (typeof window.enableAceHeightResize === 'function') {
					var editorDiv = document.getElementById("contentEditor");
					window.enableAceHeightResize(editorDiv, window.contentEditor);
				}
			} else {
				initTextareaEditor();
			}
		} catch (e) {
			initTextareaEditor();
		}
	}
	
	// Textarea 기반 에디터 초기화 (fallback)
	function initTextareaEditor() {
		var editorDiv = document.getElementById("contentEditor");
		editorDiv.innerHTML = '<textarea id="contentTextarea" style="width: 100%; height: 100%; font-family: monospace; font-size: 14px; border: none; resize: none; outline: none;"></textarea>';
	}
	
	// 에디터에서 값 가져오기
	function getEditorValue() {
		if (window.contentEditor) {
			return window.contentEditor.getValue();
		} else {
			return document.getElementById("contentTextarea").value;
		}
	}
	
	// 에디터에 값 설정하기
	function setEditorValue(value) {
		if (window.contentEditor) {
			window.contentEditor.setValue(value || '');
		} else {
			document.getElementById("contentTextarea").value = value || '';
		}
	}

	function uploadfile() {
		if ($("#connectionlist option:selected").val() == '') {
			alert("Connection을 선택하세요.");
			return;
		}

		$.ajax({
			type : 'post',
			url : '/FILE/uploadfile',
			data : {
				FilePath : $("#FilePath").val(),
				connectionId : $("#connectionlist").val(),
				Content : getEditorValue()
			},
			success : function(result) {
				if (result == 'success') {
					alert("업로드 완료")
					setEditorValue("");

				} else {
					alert(result)
				}

			},
			error : function(xhr, status, error) {
				showSystemError("파일 업로드 실패", {
					error: xhr.responseText,
					status: xhr.status,
					url: '/FileUpload/upload'
				});
			}
		});

	}
</script>
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0">
	<!-- Content Header (Page header) -->
	<section class="content-header">
		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#">파일쓰기</a></li>
		</ol>
	</section>
	<section class="content">
		<select id="connectionlist">
			<option value="">====선택하세요====</option>
		</select>
		<div class="box box-default" style="margin-top: 10px;">
			<!-- /.box-header -->
			<!-- form start -->
			<form role="form" onsubmit="return false;">
				<div class="box-body">
					<div class="form-group">
						<label for="Path">Path</label>
						<input type="text" class="form-control" id="FilePath" placeholder="Path">
					</div>
					<div class="form-group">
						<button type="button" class="btn btn-primary" onclick="uploadfile()">Submit</button>
					</div>
				</div>
			</form>
		</div>
		<div class="box box-default" id="resultbox">
			<div class="box-body">
				<div class="textcontainer">
					<div id="contentEditor" style="width: 100%; height: 300px; border: none;"></div>
				</div>
			</div>
		</div>
	</section>
</div>