<%@include file="common/common.jsp"%>
<script>
	$(document).ready(function() {
		$.ajax({
			type : 'post',
			url : "/Connection/list",
			data : {
				TYPE : ""
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

	function ConnectionDetail(value) {

		if (value == 'create') {
			$('#DB_name_input').css("display", "block");
			$('#DB_IP').val('');
			$('#DB_PORT').val('');
			$('#DB').val('');
			$('#DB_USER').val('');
			$('#DB_PW').val('');
			$('#DBTYPE').val('');

			$('#HOST_name_input').css("display", "block");
			$('#HOST_IP').val('');
			$('#HOST_PORT').val('');
			$('#HOST_USER').val('');
			$('#HOST_PW').val('');
			return;
		} else {
			$('#DB_name_input').css("display", "none");
			$('#HOST_name_input').css("display", "none");
		}

		$.ajax({
			type : 'post',
			url : '/Connection/detail',
			data : {
				DB : value
			},
			success : function(result) {

				$('#HOST_IP').val(result.HOST_IP);
				$('#HOST_PORT').val(result.HOST_PORT);
				$('#HOST_DB').val(result.HOST_DB);
				$('#HOST_USER').val(result.HOST_USER);
				$('#HOST_PW').val(result.HOST_PW);
				
				$('#DB_IP').val(result.DB_IP);
				$('#DB_PORT').val(result.DB_PORT);
				$('#DB').val(result.DB);
				$('#DB_USER').val(result.DB_USER);
				$('#DB_PW').val(result.DB_PW);
				
				$('#DBTYPE').val(result.DBTYPE);
				$('#DBTYPE').val(result.DBTYPE).prop("selected", true);
			},
			error : function() {
				alert("시스템 에러");
			}
		});
	}

	function save() {

		var connection_filename = $("#connectionlist").val();

		if (connection_filename == 'create') {
			filename = $('#NAME').val();
		}

		$.ajax({
			type : 'post',
			url : '/Connection/save',
			data : {
				file : filename,
				HOST_IP : $('#IP').val(),
				HOST_PORT : $('#PORT').val(),
				HOST_USER : $('#USER').val(),
				HOST_PW : $('#PW').val(),
				IP : $('#IP').val(),
				PORT : $('#PORT').val(),
				DB : $('#DB').val(),
				USER : $('#USER').val(),
				PW : $('#PW').val(),
				DBTYPE : $('#DBTYPE').val()

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
		<h1>
			Connection관리
		</h1>
		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#">Connection관리</a></li>
		</ol>
	</section>
	<section class="content">
		<select id="connectionlist" onchange="ConnectionDetail(this.value)">
			<option value="" selected disabled hidden>==선택하세요==</option>
			<option id="create_option" value="create">새로 만들기</option>
		</select>

		<button type="button" class="btn btn-primary" onclick="save()">Submit</button>
		<div class="row" style="margin-top: 10px;">

			<div class="col-md-6">
				<div class="box box-default">
					<div class="box-header with-border">
						<h3 class="box-title">DB</h3>
						<select id="DBlist" onchange="ConnectionDetail(this.value)">
							<option value="" selected disabled hidden>==선택하세요==</option>
							<option id="create_option" value="create">새로 만들기</option>
						</select>
					</div>
					<!-- /.box-header -->
					<!-- form start -->
					<form role="form1">
						<div class="box-body">

							<div class="form-group row">
								<div class="col-md-6" style="margin: 2px 0; display: none;" id="DB_name_input">
									<label for="DB_NAME">DB_NAME</label> <input type="text" class="form-control" id="DB_NAME" placeholder="DB_NAME">
								</div>
								<input type="hidden" class="form-control" id="TYPE" value="DB">
								<!-- <div class="col-md-6" style="margin: 2px 0;">
									<label for="TYPE">TYPE</label>
									<select class="form-control" id="TYPE">
										<option value="" selected disabled hidden>TYPE</option>
										<option value="DB">DB</option>
										<option value="HOST">HOST</option>
									</select>
								</div> -->
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="DB_IP">IP</label> <input type="text" class="form-control" id="DB_IP" placeholder="IP">
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="DB_PORT">PORT</label> <input type="text" class="form-control" id="DB_PORT" placeholder="PORT">
								</div>
								<div class="col-md-6" style="margin: 2px 0;" id="form_DB">
									<label for="DB">DB</label> <input type="text" class="form-control" id="DB" placeholder="DB">
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="DBTYPE">DB TYPE</label>
									<select class="form-control" id="DBTYPE">
										<option value="" selected disabled hidden>DB TYPE</option>
										<option value="ORACLE">ORACLE</option>
										<option value="DB2">DB2</option>
									</select>
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="DB_USER">USER</label> <input type="text" class="form-control" id="DB_USER" placeholder="USER">
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="DB_PW">PW</label> <input type="text" class="form-control" id="DB_PW" placeholder="PW">
								</div>
							</div>
						</div>
						<!-- /.box-body -->
						<div class="box-footer">
							<button type="button" class="btn btn-primary" onclick="save()">Submit</button>
						</div>
					</form>
				</div>
			</div>
			<div class="col-md-6">
				<div class="box box-default">
					<div class="box-header with-border">
						<h3 class="box-title">HOST</h3>
						<select id="HOSTlist" onchange="ConnectionDetail(this.value)">
							<option value="" selected disabled hidden>==선택하세요==</option>
							<option id="create_option" value="create">새로 만들기</option>
						</select>
					</div>
					<!-- /.box-header -->
					<!-- form start -->
					<form role="form2">
						<div class="box-body">

							<div class="form-group row">
								<div class="col-md-6" style="margin: 2px 0; display: none;" id="HOST_name_input">
									<label for="HOST_NAME">HOST_NAME</label> <input type="text" class="form-control" id="HOST_NAME" placeholder="HOST_NAME">
								</div>
								<input type="hidden" class="form-control" id="TYPE" value="HOST">
								<!-- <div class="col-md-6" style="margin: 2px 0;">
									<label for="TYPE">TYPE</label>
									<select class="form-control" id="TYPE">
										<option value="" selected disabled hidden>TYPE</option>
										<option value="DB">DB</option>
										<option value="HOST">HOST</option>
									</select>
								</div> -->
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="HOST_IP">IP</label> <input type="text" class="form-control" id="HOST_IP" placeholder="IP">
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="HOST_PORT">PORT</label> <input type="text" class="form-control" id="HOST_PORT" placeholder="PORT">
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="HOST_USER">USER</label> <input type="text" class="form-control" id="HOST_USER" placeholder="USER">
								</div>
								<div class="col-md-6" style="margin: 2px 0;">
									<label for="HOST_PW">PW</label> <input type="text" class="form-control" id="HOST_PW" placeholder="PW">
								</div>
							</div>
						</div>
						<!-- /.box-body -->
						<div class="box-footer">
							<button type="button" class="btn btn-primary" onclick="save()">Submit</button>
						</div>
					</form>
				</div>
			</div>
		</div>
	</section>
</div>