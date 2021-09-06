<%@include file="common/common.jsp"%>
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
	});

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
				Content : $("#Content").val()
			},
			success : function(result) {
				if (result == 'success') {
					alert("업로드 완료")
					$("#Content").val("");

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
				<!-- <pre id="result" style="border: none; background: 0, 0, 0, 0; min-height: 450px; overflow: auto"></pre> -->
				<textarea id="Content" style="border: none; background: 0, 0, 0, 0; height: calc(100vh - 320px); width: 100%; overflow: auto"></textarea>
			</div>
		</div>
	</section>
</div>