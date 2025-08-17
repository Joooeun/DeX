<%@include file="common/common.jsp"%>
<!-- Ace Editor CDN -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ace.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.0/ext-language_tools.js"></script>
<script>
	$(document).ready(function() {

		$.ajax({
			type : 'post',
			url : "/Connection/list",
			data : {
				TYPE : "HOST"
			},
			success : function(result) {
				for (var i = 0; i < result.length; i++) {
					$('#connectionlist').append("<option value='" + result[i].split('.')[0] + "'>" + result[i].split('.')[0] + "</option>");
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
				window.contentEditor = ace.edit("contentEditor");
				window.contentEditor.setTheme("ace/theme/chrome");
				window.contentEditor.session.setMode("ace/mode/text");
				window.contentEditor.setShowPrintMargin(false);
				window.contentEditor.setFontSize(14);
				window.contentEditor.setOption("enableBasicAutocompletion", true);
				window.contentEditor.setOption("enableLiveAutocompletion", true);
				window.contentEditor.setOption("enableSnippets", true);
				window.contentEditor.resize();
			} else {
				console.log("Ace Editor not available, using textarea");
				initTextareaEditor();
			}
		} catch (e) {
			console.log("Ace Editor initialization failed, using textarea");
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
				Connection : $("#connectionlist").val(),
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
		<h1>FileUpload</h1>
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
				<div id="contentEditor" style="height: calc(100vh - 320px); width: 100%; border: 1px solid #ddd; border-radius: 4px;"></div>
			</div>
		</div>
	</section>
</div>