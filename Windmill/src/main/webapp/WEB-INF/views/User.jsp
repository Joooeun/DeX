<%@include file="common/common.jsp"%>
<script>
	$(document).ready(
			function() {
				$.ajax({
					type : 'post',
					url : "/User/list",
					data : {
						TYPE : ""
					},
					success : function(result) {
						for (var i = 0; i < result.length; i++) {
							$('#userlist').append(
									"<option value='" + result[i].split('.')[0]
											+ "'>" + result[i].split('.')[0]
											+ "</option>");
						}
					},
					error : function() {
						alert("시스템 에러");
					}
				});
			});
	function UserDetail(value) {
		if (value == 'create') {
			$('#id_input').css("display", "block");

			$('#IP').val('');
			$('#PW').val('');
			$('#MENU').val('');
			return;
		} else {
			$('#id_input').css("display", "none");
		}
		$.ajax({
			type : 'post',
			url : '/User/detail',
			data : {
				ID : value
			},
			success : function(result) {

				$('#IP').val(result.IP);
				$('#PW').val(result.PW);
				$('#MENU').val(result.MENU);
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}
	function save() {
		var filename = $("#userlist").val();
		if (filename == 'create') {
			filename = $('#ID').val();
		}
		$.ajax({
			type : 'post',
			url : '/User/save',
			data : {
				file : filename,
				ID : $('#ID').val(),
				IP : $('#IP').val(),
				PW : $('#PW').val(),
				MENU : $('#MENU').val(),
			},
			success : function(result) {
				alert("저장 되었습니다.");
			},
			error : function() {
				alert("저장되지 않았습니다.");
			}
		});
	}
</script>
<!-- Content Wrapper. Contains page content -->
<div class="content-wrapper" style="margin-left: 0">
	<!-- Content Header (Page header) -->
	<section class="content-header">
		<h1>User관리</h1>
		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#">User관리</a></li>
		</ol>
	</section>
	<section class="content">
		<select id="userlist" onchange="UserDetail(this.value)">
			<option value="" selected disabled hidden>==선택하세요==</option>
			<option id="create_option" value="create">새로 만들기</option>
		</select>
		<div class="box box-default" style="margin-top: 10px;">
			<div class="box-header with-border">
				<h3 class="box-title">User Detail</h3>
			</div>
			<!-- /.box-header -->
			<!-- form start -->
			<form role="form">
				<div class="box-body">
					<div class="form-group row">
						<div class="col-md-4" style="margin: 2px 0; display: none;"
							id="id_input">
							<label for="ID">ID</label> <input type="text"
								class="form-control" id="ID" placeholder="ID">
						</div>

					</div>
					<div class="form-group row">
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="IP">IP</label> <input type="text"
								class="form-control" id="IP" placeholder="IP">
						</div>
						<div class="col-md-4" style="margin: 2px 0;">
							<label for="PW">PW</label> <input type="text"
								class="form-control" id="PW" placeholder="PW">
						</div>



						<div class="col-md-4" style="margin: 2px 0;">
							<label for="MENU">MENU</label> <select class="form-control"
								id="MENU">
								<option value="" selected disabled hidden>==선택하세요==</option>
								<c:forEach var="item" items="${MENU}">
									<option id="create_option" value="${item.Name}">${item.Name}</option>
								</c:forEach>
							</select>
						</div>

					</div>

				</div>
				<!-- /.box-body -->
				<div class="box-footer">
					<button type="button" class="btn btn-primary" onclick="save()">Submit</button>
				</div>
			</form>
		</div>
	</section>
</div>