<%@include file="common/common.jsp"%>
<!-- Ace Editor CDN -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ace.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ext-language_tools.js"></script>
<script>
	$(document).ready(function() {
		
		if($("#FilePath").val().length>0){
			//readfile()
		}

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
				showSystemError("연결 정보 조회 실패", {
					error: xhr.responseText,
					status: xhr.status,
					url: '/Connection/list'
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
				window.resultEditor = ace.edit("resultEditor");
				window.resultEditor.setTheme("ace/theme/chrome");
				window.resultEditor.session.setMode("ace/mode/text");
				window.resultEditor.setShowPrintMargin(false);
				window.resultEditor.setFontSize(14);
				window.resultEditor.setOptions({
					fontFamily: selectedFont,
					enableBasicAutocompletion: true,
					enableSnippets: true,
					enableLiveAutocompletion: true,
					showPrintMargin: false,
					showGutter: true,
					showInvisibles: false
				});
				window.resultEditor.setReadOnly(true); // 읽기 전용
				
				// 에디터 리사이즈
				setTimeout(function() {
					window.resultEditor.resize();
				}, 100);
				
				// 우하단 모서리 드래그로 높이 변경(저장 없음) - 공통 함수 사용
				if (typeof window.enableAceHeightResize === 'function') {
					var editorDiv = document.getElementById("resultEditor");
					window.enableAceHeightResize(editorDiv, window.resultEditor);
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
		var editorDiv = document.getElementById("resultEditor");
		editorDiv.innerHTML = '<textarea id="resultTextarea" style="width: 100%; height: 100%; font-family: monospace; font-size: 14px; border: none; resize: none; outline: none; background-color: #f5f5f5;" readonly></textarea>';
	}
	
	// 에디터에 값 설정하기
	function setEditorValue(value) {
		if (window.resultEditor) {
			window.resultEditor.setValue(value || '');
		} else {
			document.getElementById("resultTextarea").value = value || '';
		}
	}

	function readfile() {
		if ($("#connectionlist option:selected").val() == '') {
			alert("Connection을 선택하세요.");
			return;
		}

		$.ajax({
			type : 'post',
			url : '/FILE/readfile',
			data : {
				FilePath : $("#FilePath").val().split("\\").join("/"),
				connectionId : $("#connectionlist").val()
			},
			success : function(result) {

				$("#resultbox").css("display", "block");
				setEditorValue(result.result);

			},
			error : function(xhr, status, error) {
				showSystemError("파일 읽기 실패", {
					error: xhr.responseText,
					status: xhr.status,
					url: '/FileRead/read'
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
			<li class="active"><a href="#">파일읽기</a></li>
		</ol>
	</section>
	<section class="content">
		<select id="connectionlist" >
			<option value="">====선택하세요====</option>
		</select>
		<div class="box box-default" style="margin-top:10px;">
			<!-- /.box-header -->
			<!-- form start -->
			<form role="form" onsubmit="return false;">
				<div class="box-body">
					<div class="form-group">
						<label for="Path">Path</label> <input type="text" class="form-control" id="FilePath" placeholder="Path" value="${Path}">
					</div>
					<div class="form-group">
						<button type="button" class="btn btn-primary" onclick="readfile()">Submit</button>
					</div>
				</div>
			</form>
		</div>
		<div class="box box-default" id="resultbox" style="display: none;">
			<div class="box-body">
				<div class="textcontainer">
					<div id="resultEditor" style="width: 100%; height: 450px; border: none;"></div>
				</div>
			</div>
		</div>
	</section>
</div>