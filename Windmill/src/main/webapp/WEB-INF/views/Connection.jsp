<%@include file="common/common.jsp"%>
<script>
	$(document).ready(function() {

		$.ajax({
			type : 'post',
			url : "/Connection/list",
			data : {
				TYPE : "DB"
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

		if (value == '')
			return;

		$.ajax({
			type : 'post',
			url : '/Connection/detail',
			data : {
				DB : value
			},
			success : function(result) {

				$('#IP').val(result.IP);
				$('#PORT').val(result.PORT);
				$('#DB').val(result.DB);
				$('#USER').val(result.USER);
				$('#PW').val(result.PW);
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
		<h1>
			Connection관리 <small>Sample</small>
		</h1>
		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#">Connection관리</a></li>
		</ol>
	</section>
	<section class="content">
		<select id="connectionlist" onchange="ConnectionDetail(this.value)">
			<option>====선택하세요====</option>
		</select>
		<div class="box box-primary">
			<div class="box-header with-border">
				<h3 class="box-title">Connection Detail</h3>
			</div>
			<!-- /.box-header -->
			<!-- form start -->
			<form role="form">
				<div class="box-body">
					<div class="form-group">
						<label for="IP">IP</label> <input type="text" class="form-control" id="IP" placeholder="IP">
					</div>
					<div class="form-group">
						<label for="PORT">PORT</label> <input type="text" class="form-control" id="PORT" placeholder="PORT">
					</div>
					<div class="form-group">
						<label for="DB">DB</label> <input type="text" class="form-control" id="DB" placeholder="DB">
					</div>
					<div class="form-group">
						<label for="USER">USER</label> <input type="text" class="form-control" id="USER" placeholder="USER">
					</div>
					<div class="form-group">
						<label for="PW">PW</label> <input type="text" class="form-control" id="PW" placeholder="PW">
					</div>
				</div>
				<!-- /.box-body -->
				<div class="box-footer">
					<button type="button" class="btn btn-primary">Submit</button>
				</div>
			</form>
		</div>
	</section>
</div>