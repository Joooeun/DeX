<%@include file="common/common.jsp"%>
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
				for (var i = 0; i < result.length; i++) {
					$('#connectionlist').append("<option value='" + result[i].split('.')[0] + "'>" + result[i].split('.')[0] + "</option>");
				}
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	});

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
				Connection : $("#connectionlist").val()
			},
			success : function(result) {

				$("#resultbox").css("display", "block");
				$("#result").text(result.result);

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
				<pre id="result" style="border: none; background: 0, 0, 0, 0; min-height: 450px; overflow: auto"></pre>
			</div>
		</div>
	</section>
</div>