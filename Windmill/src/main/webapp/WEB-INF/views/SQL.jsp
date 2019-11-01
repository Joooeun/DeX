<%@include file="common/common.jsp"%>
<style>
::-webkit-scrollbar {
	width: 5px;
	height: 5px;
	border: 1px solid #fff;
	opacity: 0.7;
}

::-webkit-scrollbar-button:start:decrement, ::-webkit-scrollbar-button:end:increment
	{
	display: block;
	height: 10px;
}

::-webkit-scrollbar-track {
	background: #efefef;
	-webkit-border-radius: 10px;
	border-radius: 10px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .2)
}

::-webkit-scrollbar-thumb {
	height: 50px;
	width: 50px;
	background: rgba(0, 0, 0, .2);
	-webkit-border-radius: 8px;
	border-radius: 8px;
	-webkit-box-shadow: inset 0 0 4px rgba(0, 0, 0, .1)
}
</style>
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

					if (result[i].split('.')[0] == $('#selectedConnection').val()) {
						$('#connectionlist').append("<option value=\"" + result[i].split('.')[0] + "\"  selected=\"selected\">" + result[i].split('.')[0] + "</option>");
					} else {
						$('#connectionlist').append("<option value='" + result[i].split('.')[0] + "'>" + result[i].split('.')[0] + "</option>");
					}
				}

				if ($(".paramvalue").eq(0).val() != '' && $(".paramvalue").length > 0) {
					excute();
				}

			},
			error : function() {
				alert("시스템 에러");
			}
		});

		$(document).keydown(function(event) {
			if (event.keyCode == '112') {
				sendSql($("#F1").val());
			} else if (event.keyCode == '113') {
				sendSql($("#F2").val());
			} else if (event.keyCode == '114') {
				sendSql($("#F3").val());
			} else if (event.keyCode == '115') {
				sendSql($("#F4").val());
			} else if (event.keyCode == '116') {
				sendSql($("#F5").val());
			} else if (event.keyCode == '117') {
				sendSql($("#F6").val());
			} else if (event.keyCode == '118') {
				sendSql($("#F7").val());
			} else if (event.keyCode == '119') {
				sendSql($("#F8").val());
			} else if (event.keyCode == '120') {
				sendSql($("#F9").val());
			} else if (event.keyCode == '121') {
				sendSql($("#F10").val());
			} else if (event.keyCode == '122') {
				sendSql($("#F11").val());
			} else if (event.keyCode == '가나다라마바사') {
				sendSql($("#F12").val());
			}
		});

		$(document).on("click", ".Resultrow", function() {
			$(".Resultrow").removeClass('success');
			$(this).addClass('success');
		});

	});

	function startexcute() {
		if ($("#connectionlist option:selected").val() == '') {
			alert("Connection을 선택하세요.");
			return;
		}

		excute();
		if ($("#refreshtimeout").val() > 0) {

			setInterval(excute, $("#refreshtimeout").val() * 1000);
		}
		setTimeout(function() {
			excute();
		}, 500);

	}

	function excute() {

		var sql = $("#sql_text").val();

		for (var i = 0; i < $(".param").length; i++) {
			if ($(".paramvalue").eq(i).val() != '') {
				if ($(".paramvalue").eq(i).attr('paramtype') == 'string') {
					sql = sql.split(':' + $(".param").eq(i).html()).join('\'' + $(".paramvalue").eq(i).val() + '\'');
				} else if ($(".paramvalue").eq(i).attr('paramtype') == 'number') {
					sql = sql.split(':' + $(".param").eq(i).html()).join($(".paramvalue").eq(i).val());
				}
			}
		}

		$.ajax({
			type : 'post',
			url : '/SQL/excute',
			data : {
				sql : sql,
				Connection : $("#connectionlist").val()
			},
			success : function(result) {

				$("#Resultbox").css("display", "block");

				var str = '';

				str += '<tr>';
				str += '<th>#</th>';
				for (var title = 0; title < result[0].length; title++) {
					str += '<th>' + result[0][title] + '</th>';
				}
				str += '</tr>';
				str += '<tr>' + '<td colspan="100" style="padding: 0; border: none;">' + '<div id="valuediv" style="height: calc(100vh - 3950px); overflow: auto;">' + '<table class="table table-condensed table-hover table-striped" id="result_body" style="margin: 0;">' + '</table>' + '</div>' + '</td>' + '</tr>';

				for (var outter = 1; outter < result.length; outter++) {
					str += '<tr style="visibility: hidden;">';
					str += '<td style="border:none;">' + outter + '</td>';

					for (var inner = 0; inner < result[outter].length; inner++) {
						str += '<td style="border:none;">' + result[outter][inner] + '</td>';
					}

					str += '</tr>';
				}

				$("#result_head").html(str);
				str = '';

				for (var outter = 1; outter < result.length; outter++) {
					str += '<tr class="Resultrow" param='+result[outter][0]+'>';
					str += '<td>' + outter + '</td>';

					for (var inner = 0; inner < result[outter].length; inner++) {
						str += '<td style="border-left:1px solid #cccccc">' + result[outter][inner] + '</td>';
					}

					str += '</tr>';
				}

				str += '<tr style="visibility: hidden;">';
				str += '<th>#</th>';
				for (var title = 0; title < result[0].length; title++) {
					str += '<th>' + result[0][title] + '</th>';
				}
				str += '</tr>';
				$("#result_body").html(str);

			},
			error : function() {
				alert("시스템 에러");
			}
		});

	}

	function sessionCon(value) {

		$.ajax({
			type : 'post',
			url : '/Connection/sessionCon',
			data : {
				Connection : value
			},
			success : function(result) {
				//alert("성공")
			},
			error : function() {
				//alert("시스템 에러");
			}
		});

	}

	function sendSql(value) {
		if (value == null) {
			return;
		}

		var colnum = value.split('&')[1].split(',');
		var str = '';
		for (var i = 0; i < colnum.length; i++) {
			if (i > 0) {
				str += '&';
			}
			str += $(".Resultrow.success").children('td').eq(colnum[i]).html();
		}

		$("#sendvalue").val(str);

		document.ParamForm.action = "/SQL?Path=" + encodeURI($("#Path").val() + "/" + value.split('&')[0] + ".sql");
		document.ParamForm.method = "POST";
		document.ParamForm.submit();

	}

	function readfile() {
		$.ajax({
			type : 'post',
			url : '/SQL/readfile',
			success : function(result) {
				alert(result)
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
			${title} <small>SQL</small>
		</h1>
		<ol class="breadcrumb">
			<li><a href="#"><i class="icon ion-ios-home"></i> Home</a></li>
			<li class="active"><a href="#" onclick="readfile()">SQL</a></li>
		</ol>
	</section>
	<section class="content">
		<div class="row">
			<div class="col-xs-12">
				<div class="box box-default ">
					<div class="box-header with-border">
						<h3 class="box-title">파라미터 입력</h3>
						&nbsp;&nbsp;&nbsp;<input id="selectedConnection" type="hidden" value="${Connection}">
						<select id="connectionlist" onchange="sessionCon(this.value)">
							<option value="">====Connection====</option>
						</select>
					</div>
					<form role="form-horizontal" name="ParamForm">
						<div class="box-body">
							<div class="form-group">
								<c:forEach var="item" items="${Param}" varStatus="status">
									<span class="col-sm-1 param" id="param${status.count}" style="padding-top: 7px; font-weight: bold; font-size: 15px">${item.name}</span>
									<div class="col-sm-2" style="margin: 2px 0; padding: 0 15px 0 0;">
										<input type="text" class="form-control paramvalue" paramtype="${item.type}" value="${item.value}" style="padding: 0 2px;">
									</div>
								</c:forEach>
								<div class="col-sm-1 pull-right">
									<input type="hidden" id="sendvalue" name="sendvalue"> <input type="button" class="form-control" value="실행" onclick="startexcute();">
								</div>
							</div>
						</div>
					</form>
				</div>
			</div>
		</div>
		<div class="row" id="Resultbox">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header with-border">
						<h3 class="box-title">Result</h3>
					</div>
					<div style="overflow-y: hidden; overflow-x: overlay;">
						<table class="table table-condensed table-striped" id="result_head" style="margin: 0; height: calc(100vh - 400px);">
						</table>
					</div>
					<div class="box-footer clearfix" hidden="hidden">
						<ul class="pagination pagination-sm no-margin pull-right">
							<li><a href="#"><<</a></li>
							<li><a href="#">1</a></li>
							<li><a href="#">2</a></li>
							<li><a href="#">3</a></li>
							<li><a href="#">>></a></li>
						</ul>
					</div>
				</div>
			</div>
		</div>
		<div class="row" id="Keybox" style="position: fixed; bottom: 0px; width: 100%">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header with-border">
						<h3 class="box-title">Short Key</h3>
					</div>
					<div class="box-body">
						<div class="form-group">
							<c:forEach var="item" items="${ShortKey}">
								<button type="button" class="btn btn-default" onclick="sendSql('${item.menu}&${item.column}')">${item.keytitle}</button>
								<input type="hidden" id="${item.key}" value="${item.menu}&${item.column}">
							</c:forEach>
						</div>
					</div>
				</div>
			</div>
		</div>
		<textarea rows="10" cols="200" id="sql_text" hidden="hidden">${sql}</textarea>
		<input id="Path" value="${Path}" type="hidden"> <input id="refreshtimeout" value="${refreshtimeout}" type="hidden">
	</section>
</div>