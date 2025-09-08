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
			error : function() {
				alert("시스템 에러");
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
				ace.require("ace/ext/language_tools");
				window.resultEditor = ace.edit("resultEditor");
				window.resultEditor.setTheme("ace/theme/chrome");
				window.resultEditor.session.setMode("ace/mode/text");
				window.resultEditor.setShowPrintMargin(false);
				window.resultEditor.setFontSize(14);
				window.resultEditor.setOption("enableBasicAutocompletion", true);
				window.resultEditor.setOption("enableLiveAutocompletion", true);
				window.resultEditor.setOption("enableSnippets", true);
				window.resultEditor.setReadOnly(true); // 읽기 전용
				window.resultEditor.resize();
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
			error : function() {
				alert("시스템 에러");
			}
		});

	}
</script>
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0">
	<!-- Content Header (Page header) -->
	<section class="content-header">
		<h1>FileRead</h1>
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
				<div id="resultEditor" style="height: 450px; width: 100%; border: 1px solid #ddd; border-radius: 4px;"></div>
			</div>
		</div>
	</section>
</div>